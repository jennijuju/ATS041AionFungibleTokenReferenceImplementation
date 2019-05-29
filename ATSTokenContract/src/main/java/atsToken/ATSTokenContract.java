package atsToken;

import avm.Address;
import avm.Blockchain;
import avm.Result;
import org.aion.avm.tooling.AddressUtil;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.userlib.AionBuffer;
import org.aion.avm.userlib.AionList;
import org.aion.avm.userlib.AionSet;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.avm.userlib.abi.ABIEncoder;

import javax.xml.crypto.Data;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.BiFunction;

public class ATSTokenContract {

    /***********************************************Constants***********************************************/
    private static final int BIGINTEGER_LENGTH = 32;

    /******************************************ATS Contract State******************************************/
    private static String tokenName;
    private static String tokenSymbol;
    private static int tokenGranularity;
    private static BigInteger tokenTotalSupply;
    //private static Address AIRContract = new Address("0xa062407049f4fa5fb15f088f115fe87f6d6231e45c2e8f8448a44c282a9d7bf3".getBytes());


    /**************************************Deployment Initialization***************************************/
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
    public static String getTokenName() {
        return tokenName;
    }

    @Callable
    public static String getTokenSymbol() {
        return tokenSymbol;
    }

    @Callable
    public static int getTokenGranularity() {
        return tokenGranularity;
    }

    @Callable
    public static String getTokenTotalSupply() {
        return tokenTotalSupply.toString();
    }

    /*********************************************Token Holder*********************************************/
    /**
     * Return balance in String to make it human readable.
     *
     * @param tokenHolder
     * @return
     */
    @Callable
    public static String getBalanceOf(Address tokenHolder) {
        byte[] tokenHolderInformation = Blockchain.getStorage(tokenHolder.toByteArray());
        return !Arrays.equals(tokenHolderInformation, null)
                ? AionBuffer.wrap(tokenHolderInformation).get32ByteInt().toString()
                : "0";
    }

    @Callable
    public static void authorizeOperator(Address operator) {
        Blockchain.require(!Blockchain.getCaller().equals(operator));
        Address tokenHolderAddress = Blockchain.getCaller();
        byte[] tokenHolderInformation = Blockchain.getStorage(tokenHolderAddress.toByteArray());
        if (tokenHolderInformation == null ) { /*No related information yet.
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
            TokenHolderInformation tokenHolder = new TokenHolderInformation(tokenHolderInformation);
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
            if(tokenHolderInformation != null && tokenHolderInformation.length != BIGINTEGER_LENGTH) {
                TokenHolderInformation tokenHolder = new TokenHolderInformation(tokenHolderInformation);
                boolean tryRevokeOperator = tokenHolder.tryReveokeOperator(operator);
                if(tryRevokeOperator) {
                    Blockchain.putStorage(tokenHolderAddress.toByteArray(), tokenHolder.currentTokenHolderInformation);
                    ATSTokenContractEvents.RevokedOperator(operator, tokenHolderAddress);
                }
            }
        }
    }


    @Callable
    public static boolean isOperatorFor(Address operator, Address tokenHolder) {
        if (operator.equals(tokenHolder)) return true;
        byte[] tokenHolderInformation = Blockchain.getStorage(tokenHolder.toByteArray());
        if(tokenHolderInformation != null && tokenHolderInformation.length != BIGINTEGER_LENGTH) {
            TokenHolderInformation tokenHolderInfo = new TokenHolderInformation(tokenHolderInformation);
            return tokenHolderInfo.isOperatorFor(operator,tokenHolderInformation);
        } else return false;

    }

    private static class TokenHolderInformation {
        private byte[] currentTokenHolderInformation;
        int currentTokenHolderInfoLength;

        private TokenHolderInformation(byte[] currentTokenHolderInformation) {
            this.currentTokenHolderInformation = currentTokenHolderInformation;
            this.currentTokenHolderInfoLength = currentTokenHolderInformation.length;
        }

        private boolean isOperatorFor(Address operator, byte[] tokenHolderInfo) {
            AionBuffer tokenHolderInformation = AionBuffer.wrap(tokenHolderInfo);
            tokenHolderInformation.get32ByteInt();
            //boolean isOperatorFor = false;
            while (/*!isOperatorFor && */(tokenHolderInformation.getPosition() < tokenHolderInformation.getCapacity())) {
                Address operatorWalker = tokenHolderInformation.getAddress();
                if (operator.equals(operatorWalker)) {
                    return true;
                }
            }
            return false;
        }

        private boolean tryAddOperator(Address newOperator) {

           if (currentTokenHolderInformation.length == BIGINTEGER_LENGTH) { /*has balance but no operator yet*/
                byte[] newTokenHolderInformation = AionBuffer.allocate(BIGINTEGER_LENGTH + Address.LENGTH)
                        .put32ByteInt(AionBuffer.wrap(this.currentTokenHolderInformation).get32ByteInt())  //balance
                        .putAddress(newOperator)
                        .getArray();
                currentTokenHolderInformation = newTokenHolderInformation;
                return true;
            } else{
                boolean isOperatorFor = isOperatorFor(newOperator,currentTokenHolderInformation);
                if (!isOperatorFor) {
                    AionBuffer tokenHolderInfoBuffer = AionBuffer.wrap(currentTokenHolderInformation);
                    tokenHolderInfoBuffer.get32ByteInt();
                    byte[] newTokenHolderInformation = new byte[this.currentTokenHolderInfoLength + Address.LENGTH];
                    System.arraycopy(this.currentTokenHolderInformation, 0,
                            newTokenHolderInformation, 0,
                            currentTokenHolderInfoLength);
                    System.arraycopy(newOperator.toByteArray(), 0,
                            newTokenHolderInformation, currentTokenHolderInfoLength,
                            Address.LENGTH);
                    currentTokenHolderInformation = newTokenHolderInformation;
                    return true;
                }
                return false;
            }
        }

        private boolean tryReveokeOperator(Address revokeOperator) {
            AionBuffer tokenHolderInfoBuffer =  AionBuffer.wrap(currentTokenHolderInformation);
            tokenHolderInfoBuffer.get32ByteInt();
            //boolean isOperatorFor = false;
            int walker = 0;
            while(/*!isOperatorFor && */(tokenHolderInfoBuffer.getPosition() < tokenHolderInfoBuffer.getLimit())) {
                Address operatorWalker = tokenHolderInfoBuffer.getAddress();
                if(revokeOperator.equals(operatorWalker)) {
                    //isOperatorFor = true;
                    byte[] newTokenHolderInformation = new byte[this.currentTokenHolderInfoLength - Address.LENGTH];
                    System.arraycopy(this.currentTokenHolderInformation,0,
                                    newTokenHolderInformation,0,
                            (BIGINTEGER_LENGTH + walker * Address.LENGTH));
                    System.arraycopy(this.currentTokenHolderInformation, (BIGINTEGER_LENGTH + (walker + 1)* Address.LENGTH),
                                      newTokenHolderInformation,(BIGINTEGER_LENGTH + walker * Address.LENGTH), (currentTokenHolderInfoLength- (BIGINTEGER_LENGTH + (walker + 1)* Address.LENGTH)));
                    currentTokenHolderInformation = newTokenHolderInformation;
                    return true;
                }
                walker++;
            }
            return false;
        }
//
//        private byte[] getData() {
//            return this.currentTokenHolderInformation;
//        }
    }

    private static void updateBalance(BigInteger newAmount, byte[] tokenHolderInfo, Address tokenHolder) {
        byte[] operatorInfo = Arrays.copyOfRange(tokenHolderInfo, BIGINTEGER_LENGTH, tokenHolderInfo.length);
        Blockchain.putStorage(tokenHolder.toByteArray(), AionBuffer.allocate(BIGINTEGER_LENGTH + operatorInfo.length)
                                                            .put32ByteInt(newAmount)
                                                            .put(operatorInfo)
                                                            .getArray());
    }


    /********************************************Transactions*********************************************/
    @Callable
    public static void send(Address to, byte[] amount, byte[] userData) {
        doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, new BigInteger(amount), userData, new byte[0], true);
    }

    @Callable
    public static void operatorSend(Address from, Address to, byte[] amount, byte[] userData, byte[] operatorData) {
        Blockchain.require(isOperatorFor(Blockchain.getCaller(),from));
        doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, new BigInteger(amount), userData, operatorData, true);
    }

    private static void doSend(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO));
        callSender(operator, from, to, amount, userData, operatorData);
        Address zeroAddress = new Address(new byte[0]);
        Blockchain.require(!to.equals(zeroAddress)); //forbid sending to 0x0 (=burning)
        Blockchain.require(!to.equals(Blockchain.getAddress())); //forbid sending to this contract

        byte[] fromInfo = Blockchain.getStorage(from.toByteArray());
        Blockchain.require(!Arrays.equals(fromInfo, null)); //revert tx if no info at all
        AionBuffer fromInfoBuffer = AionBuffer.wrap(fromInfo);
        BigInteger fromBalance = fromInfoBuffer.get32ByteInt();
        Blockchain.require(fromBalance.compareTo(amount) > -1);
        updateBalance(fromBalance.subtract(amount), fromInfo, from);

        byte[] toInfo = Blockchain.getStorage(to.toByteArray());
        if (Arrays.equals(toInfo, null)) { /*no info existed*/
            Blockchain.putStorage(to.toByteArray(), AionBuffer.allocate(BIGINTEGER_LENGTH).put32ByteInt(amount).getArray());
            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            ATSTokenContractEvents.Sent(operator, from, to, amount, userData, operatorData);
        } else {
            AionBuffer toInfoBuffer = AionBuffer.wrap(toInfo);
            BigInteger toBalance = toInfoBuffer.get32ByteInt();
            updateBalance(toBalance.add(amount), toInfo, to);
            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            ATSTokenContractEvents.Sent(operator, from, to, amount, userData, operatorData);
        }
    }

    //ToDO: register to AIR
    private static void callSender(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {

    }

    private static void callRecipient(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {

    }

    private static void isRegularAccount(Address to) {

    }

    /************************************************Events***********************************************/
    private static class ATSTokenContractEvents {

        private static void ATSTokenCreated(BigInteger totalSupply, Address creator) {
            Blockchain.log("ATSTokenCreated".getBytes(),
                    totalSupply.toByteArray(),
                    creator.toByteArray(),
                    new byte[0]);
        }

        /**
         * Store byte[] sizes for collecting data
         *
         * @param operator
         * @param from
         * @param to
         * @param amount
         * @param holderData
         * @param operatorData
         */
        private static void Sent(Address operator, Address from, Address to, BigInteger amount, byte[] holderData, byte[] operatorData) {

            byte[] data = AionBuffer.allocate(BIGINTEGER_LENGTH + Integer.BYTES + holderData.length + Integer.BYTES + operatorData.length)
                    .put32ByteInt(amount)
                    .putInt(holderData.length)
                    .put(holderData)
                    .putInt(operatorData.length)
                    .put(operatorData)
                    .getArray();

            Blockchain.log("Sent".getBytes(),
                    operator.toByteArray(),
                    from.toByteArray(),
                    to.toByteArray(),
                    data);

        }

        /**
         * Store byte[] sizes for collecting data
         *
         * @param operator
         * @param from
         * @param amount
         * @param holderData
         * @param operatorData
         */
        private static void Burned(Address operator, Address from, BigInteger amount, byte[] holderData, byte[] operatorData) {

            byte[] data = AionBuffer.allocate(BIGINTEGER_LENGTH + Integer.BYTES + holderData.length + Integer.BYTES + operatorData.length)
                    .put32ByteInt(amount)
                    .putInt(holderData.length)
                    .put(holderData)
                    .putInt(operatorData.length)
                    .put(operatorData)
                    .getArray();

            Blockchain.log("Burned".getBytes(),
                    operator.toByteArray(),
                    from.toByteArray(),
                    data);
        }

        private static void AuthorizedOperator(Address operator, Address tokenHolder) {
            Blockchain.log("AuthorizedOperator".getBytes(),
                    operator.toByteArray(),
                    tokenHolder.toByteArray(),
                    new byte[0]);
        }

        private static void RevokedOperator(Address operator, Address tokenHolder) {
            Blockchain.log("RevokedOperator".getBytes(),
                    operator.toByteArray(),
                    tokenHolder.toByteArray(),
                    new byte[0]);
        }

    }
}


