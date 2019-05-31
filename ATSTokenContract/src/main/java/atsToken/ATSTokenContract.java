package atsToken;

import avm.Address;
import avm.Blockchain;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.userlib.AionBuffer;
import org.aion.avm.userlib.abi.ABIDecoder;

import java.math.BigInteger;

public class ATSTokenContract {

    /***********************************************Constants***********************************************/
    private static final int BIGINTEGER_LENGTH = 32;


    /**************************************Deployment Initialization***************************************/

    private static String tokenName;

    private static String tokenSymbol;

    private static int tokenGranularity;

    private static BigInteger tokenTotalSupply;

    static {

        ABIDecoder decoder = new ABIDecoder(Blockchain.getData());

        tokenName = decoder.decodeOneString();
        Blockchain.require(tokenName.length() > 0);
        tokenSymbol = decoder.decodeOneString();
        Blockchain.require(tokenSymbol.length() > 0);
        tokenGranularity = decoder.decodeOneInteger();
        Blockchain.require(tokenGranularity >= 1);
        tokenTotalSupply = new BigInteger(decoder.decodeOneByteArray());
        Blockchain.require(tokenTotalSupply.compareTo(BigInteger.ZERO) == 1);

        initialize();
        //ToDo: Register in the AIR
    }


    /********************************************Initialization********************************************/
    /**
     * The creator of the token contract holds the total supply.
     * Log the token creation.
     */
    private static void initialize() {
        Blockchain.putStorage(Blockchain.getCaller().toByteArray(), AionBuffer.allocate(BIGINTEGER_LENGTH).put32ByteInt(tokenTotalSupply).getArray());
        ATSTokenContractEvents.ATSTokenCreated(tokenTotalSupply, Blockchain.getCaller());
    }


    /**********************************************Token Info**********************************************/

    @Callable
    public static String name() {
        return tokenName;
    }

    @Callable
    public static String symbol() {
        return tokenSymbol;
    }

    @Callable
    public static int granularity() {
        return tokenGranularity;
    }

    @Callable
    public static byte[] totalSupply() {
        return tokenTotalSupply.toByteArray();
    }

    /*********************************************Token Holder*********************************************/
    /**
     * Return balance in String to make it human readable.
     *
     * @param tokenHolder
     * @return
     */
    @Callable
    public static byte[] balanceOf(Address tokenHolder) {
        byte[] tokenHolderInformation = Blockchain.getStorage(tokenHolder.toByteArray());
        return (tokenHolderInformation != null)
                ? AionBuffer.wrap(tokenHolderInformation).get32ByteInt().toByteArray()
                : new byte[0];
    }

    @Callable
    public static void authorizeOperator(Address operator) {

        //Should not assign token holder itself to be the operator. Quickly revert the tx to save energy.
        Blockchain.require(!Blockchain.getCaller().equals(operator));

        Address tokenHolderAddress = Blockchain.getCaller();
        byte[] tokenHolderInformationBytes = Blockchain.getStorage(tokenHolderAddress.toByteArray());
        if (tokenHolderInformationBytes == null ) { /*No related information yet.
                                                    Add balance as 0 first to make sure first 32 bytes of token holder information is balance.
                                                    Set number of operators to 1.
                                                    Following by the operator.*/
            byte[] newInformation = AionBuffer.allocate(BIGINTEGER_LENGTH + Address.LENGTH)
                    .put32ByteInt(BigInteger.ZERO)
                    .putAddress(operator)
                    .getArray();
            Blockchain.putStorage(tokenHolderAddress.toByteArray(), newInformation);
            ATSTokenContractEvents.AuthorizedOperator(operator, tokenHolderAddress);
        } else {
            TokenHolderInformation tokenHolder = new TokenHolderInformation(tokenHolderInformationBytes);
            boolean addOperatorSuccess = tokenHolder.tryAddOperator(operator);
            if(addOperatorSuccess) {
                Blockchain.putStorage(tokenHolderAddress.toByteArray(), tokenHolder.currentTokenHolderInformation);
                ATSTokenContractEvents.AuthorizedOperator(operator, tokenHolderAddress);
            }
        }
    }

    @Callable
    public static void revokeOperator(Address operator) {
        if (!Blockchain.getCaller().equals(operator)) {
            Address tokenHolderAddress = Blockchain.getCaller();
            byte[] tokenHolderInformation = Blockchain.getStorage(tokenHolderAddress.toByteArray());
            if(tokenHolderInformation != null && tokenHolderInformation.length > BIGINTEGER_LENGTH) {
                TokenHolderInformation tokenHolder = new TokenHolderInformation(tokenHolderInformation);
                boolean tryRevokeOperator = tokenHolder.tryReveokeOperator(operator);
                if(tryRevokeOperator) {
                    Blockchain.putStorage(tokenHolderAddress.toByteArray(), tokenHolder.currentTokenHolderInformation);
                    ATSTokenContractEvents.RevokedOperator(operator, tokenHolderAddress);
                }
            }
        }
    }

    //Todo: token contract as operator?
    @Callable
    public static boolean isOperatorFor(Address operator, Address tokenHolder) {
        if (operator.equals(tokenHolder)) {
            return true;
        }
        byte[] tokenHolderInformation = Blockchain.getStorage(tokenHolder.toByteArray());
        if(tokenHolderInformation != null && tokenHolderInformation.length > BIGINTEGER_LENGTH) {
            TokenHolderInformation tokenHolderInfo = new TokenHolderInformation(tokenHolderInformation);
            return tokenHolderInfo.isOperatorFor(operator,tokenHolderInformation);
        } else {
            return false;
        }

    }

    /******************************************Token Movement*******************************************/
    @Callable
    public static void send(Address to, byte[] amount, byte[] userData) {
        doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, new BigInteger(amount), userData, new byte[0], true);
    }

    @Callable
    public static void operatorSend(Address from, Address to, byte[] amount, byte[] userData, byte[] operatorData) {
        Blockchain.require(isOperatorFor(Blockchain.getCaller(),from));
        doSend(Blockchain.getCaller(), from, to, new BigInteger(amount), userData, operatorData, true);
    }

    @Callable
    public static void burn(byte[] amount, byte[] holderData) {
        doBurn(Blockchain.getCaller(),Blockchain.getCaller(), new BigInteger(amount) ,holderData, new byte[0]);
    }

    @Callable
    public static void operatorBurn(Address tokenHolder, byte[] amount, byte[] holderData, byte[] operatorData) {
        Blockchain.require(isOperatorFor(Blockchain.getCaller(), tokenHolder));
        doBurn(Blockchain.getCaller(), tokenHolder, new BigInteger(amount), holderData, new byte[0]);
    }
    private static void doSend(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {
        Blockchain.require(amount.compareTo(BigInteger.ZERO) >= -1);
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO));
        callSender(operator, from, to, amount, userData, operatorData);
        Blockchain.require(!to.equals(new Address(new byte[32]))); //forbid sending to 0x0 (=burning)
        Blockchain.require(!to.equals(Blockchain.getAddress())); //forbid sending to this contract


        byte[] fromTokenHolderInformation = Blockchain.getStorage(from.toByteArray());
        Blockchain.require(fromTokenHolderInformation != null); //No information at all means no balance, revert tx
        TokenHolderInformation fromInfo = new TokenHolderInformation(fromTokenHolderInformation);
        Blockchain.require(fromInfo.getBalanceOf().compareTo(amount) >= -1); //`from` doesnt have enough balance,
        // revert tx
        fromInfo.updateBalance(fromInfo.getBalanceOf().subtract(amount));
        Blockchain.putStorage(from.toByteArray(),fromInfo.currentTokenHolderInformation);


        byte[] toTokenHolderInformation = Blockchain.getStorage(to.toByteArray());
        if(toTokenHolderInformation == null) {
            //Blockchain.println("Check point 3.");

            Blockchain.putStorage(to.toByteArray(),
                                    AionBuffer.allocate(BIGINTEGER_LENGTH).put32ByteInt(amount).getArray());
            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            ATSTokenContractEvents.Sent(operator, from, to, amount, userData, operatorData);
        } else {
            //Blockchain.println("Check point 4.");

            TokenHolderInformation toInfo = new TokenHolderInformation(Blockchain.getStorage(to.toByteArray()));
            toInfo.updateBalance(toInfo.getBalanceOf().add(amount));
            Blockchain.putStorage(to.toByteArray(), toInfo.currentTokenHolderInformation);
            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            ATSTokenContractEvents.Sent(operator, from, to, amount, userData, operatorData);
        }
    }

    private static void doBurn(Address operator, Address tokenHolder, BigInteger amount, byte[] holderData,
                               byte[] operatorData) {
        Blockchain.require(amount.compareTo(BigInteger.ZERO) >= -1);
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO));
        byte[] tokenHolderInformation = Blockchain.getStorage(tokenHolder.toByteArray());
        Blockchain.require(tokenHolderInformation != null);
        TokenHolderInformation tokenhHolderInfo = new TokenHolderInformation(tokenHolderInformation);
        Blockchain.require(tokenhHolderInfo.getBalanceOf().compareTo(amount) >= -1);
        tokenhHolderInfo.updateBalance(tokenhHolderInfo.getBalanceOf().subtract(amount));
        Blockchain.putStorage(tokenHolder.toByteArray(),tokenhHolderInfo.currentTokenHolderInformation);
        //Todo: test on real network
        tokenTotalSupply = tokenTotalSupply.subtract(amount);

        //Todo: Why callSender for burning - ask Yao
        callSender(operator, tokenHolder, new Address(new byte[32]), amount, holderData, operatorData);//?
        ATSTokenContractEvents.Burned(operator, tokenHolder, amount, holderData, operatorData);
    }

    //ToDO: register to AIR
    private static void callSender(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {

    }

    //ToDO: register to AIR
    private static void callRecipient(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {

    }

    private static boolean isRegularAccount(Address address) {
        return (Blockchain.getCodeSize(address) > 0) ? true : false;
    }


    /*********************************************Cross Chain *******************************************/
    @Callable
    public static void thaw (Address localRecipient, byte[] amount, byte[] bridgeId, byte[] bridgeData,
                             byte[] remoteSender, byte[] remoteBridgeId, byte[] remoteData) {
    }

    @Callable
    public static void freeze(byte[] remoteRecipient, byte[] amount, byte[] bridgeId, byte[] localData) {
    }

    @Callable
    public static void operatorFreeze(Address localSender, byte[] remoteRecipient, byte[] amount, byte[] bridgeId,
                                      byte[] localData) {
    }


    //Todo: test on network
    @Callable
    public static byte[] getLiquidSupply() {
        return tokenTotalSupply.subtract(new BigInteger(balanceOf(Blockchain.getAddress()))).toByteArray();
    }

}




