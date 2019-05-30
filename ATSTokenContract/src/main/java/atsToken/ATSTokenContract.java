package atsToken;

import avm.Address;
import avm.Blockchain;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.tooling.abi.Initializable;
import org.aion.avm.userlib.AionBuffer;
import java.math.BigInteger;
import java.util.Arrays;

public class ATSTokenContract {

    /***********************************************Constants***********************************************/
    private static final int BIGINTEGER_LENGTH = 32;


    /**************************************Deployment Initialization***************************************/
    @Initializable
    private static String tokenName;

    @Initializable
    private static String tokenSymbol;

    @Initializable
    private static int tokenGranularity;

    @Initializable
    private static byte[] tokenTotalSupplyByteArray;

    private static BigInteger tokenTotalSupply;

    static {
        Blockchain.require(tokenName.length() > 0);
        Blockchain.require(tokenSymbol.length() > 0);
        Blockchain.require(tokenGranularity >= 1);
        tokenTotalSupply = new BigInteger(tokenTotalSupplyByteArray);
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

    //Todo: make sure returning string is fine
    @Callable
    public static String getTokenTotalSupply() {
        return tokenTotalSupply.toString();
    }

    //Todo: test on network
    @Callable
    public static String getLiquidSupply() {
        return tokenTotalSupply.subtract(new BigInteger(getBalanceOf(Blockchain.getAddress()).getBytes())).toString();
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

    //Todo: token contract as operator?
    @Callable
    public static boolean isOperatorFor(Address operator, Address tokenHolder) {
        if (operator.equals(tokenHolder)) return true;
        byte[] tokenHolderInformation = Blockchain.getStorage(tokenHolder.toByteArray());
        if(tokenHolderInformation != null && tokenHolderInformation.length != BIGINTEGER_LENGTH) {
            TokenHolderInformation tokenHolderInfo = new TokenHolderInformation(tokenHolderInformation);
            return tokenHolderInfo.isOperatorFor(operator,tokenHolderInformation);
        } else return false;

    }


    /******************************************Token Movement*******************************************/
    @Callable
    public static void send(Address to, byte[] amount, byte[] userData) {
        //Blockchain.println("send user data length: " + userData.length);
        doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, new BigInteger(amount), userData, new byte[0], true);
    }

    @Callable
    public static void operatorSend(Address from, Address to, byte[] amount, byte[] userData, byte[] operatorData) {
        Blockchain.require(isOperatorFor(Blockchain.getCaller(),from));
        doSend(Blockchain.getCaller(), from, to, new BigInteger(amount), userData, operatorData, true);
    }

    //Todo: Can data be null? -ask Yao
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
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO));
        callSender(operator, from, to, amount, userData, operatorData);
        Blockchain.require(!to.equals(new Address(new byte[32]))); //forbid sending to 0x0 (=burning)
        Blockchain.require(!to.equals(Blockchain.getAddress())); //forbid sending to this contract
        //Blockchain.println("Check point 1.");


        byte[] fromTokenHolderInformation = Blockchain.getStorage(from.toByteArray());
        Blockchain.require(fromTokenHolderInformation != null); //No information at all means no balance, revert tx
        TokenHolderInformation fromInfo = new TokenHolderInformation(fromTokenHolderInformation);
        Blockchain.require(fromInfo.getBalanceOf().compareTo(amount) >= -1); //`from` doesnt have enough balance,
        // revert tx
        fromInfo.updateBalance(fromInfo.getBalanceOf().subtract(amount));
        Blockchain.putStorage(from.toByteArray(),fromInfo.currentTokenHolderInformation);

        //Blockchain.println("Check point 2.");


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
            //Blockchain.println("data length for log: " + (BIGINTEGER_LENGTH + Integer.BYTES + holderData.length + Integer.BYTES + operatorData.length));
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

    /********************************************Data Wrapper********************************************/
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

        private void updateBalance(BigInteger newBalance) {
            byte[] newBalanceArray = AionBuffer.allocate(BIGINTEGER_LENGTH).put32ByteInt(newBalance).getArray();
            if (this.currentTokenHolderInformation == null) {
                currentTokenHolderInformation = newBalanceArray;
            } else {
                byte[] newTokenHolderInformation = new byte[this.currentTokenHolderInfoLength];
                System.arraycopy(newBalanceArray, 0,
                        newTokenHolderInformation, 0,
                        BIGINTEGER_LENGTH);
                System.arraycopy(this.currentTokenHolderInformation, BIGINTEGER_LENGTH,
                        newTokenHolderInformation, BIGINTEGER_LENGTH,
                        (currentTokenHolderInfoLength - BIGINTEGER_LENGTH));
                currentTokenHolderInformation = newBalanceArray;
            }
        }

        private BigInteger getBalanceOf() {
            return (this.currentTokenHolderInformation != null)
                    ? AionBuffer.wrap(this.currentTokenHolderInformation).get32ByteInt()
                    : BigInteger.ZERO;
        }

//
//        private byte[] getData() {
//            return this.currentTokenHolderInformation;
//        }
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
}




