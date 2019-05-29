//package atsToken;
//
//import avm.Address;
//import avm.Blockchain;
//import org.aion.avm.tooling.abi.Callable;
//import org.aion.avm.userlib.AionBuffer;
//import org.aion.avm.userlib.AionSet;
//import org.aion.avm.userlib.abi.ABIDecoder;
//
//import java.math.BigInteger;
//import java.util.Arrays;
//
//public class ATSTokenContractCopy {
//
//    /***********************************************Constants***********************************************/
//    private static final int BIGINTEGER_LENGTH = 32;
//    private static final int ADDRESS_LENGTH = 32;
//
//    /******************************************ATS Contract State******************************************/
//    private static String tokenName;
//    private static String tokenSymbol;
//    private static int tokenGranularity;
//    private static BigInteger tokenTotalSupply;
//    //private static Address AIRContract = new Address("0xa062407049f4fa5fb15f088f115fe87f6d6231e45c2e8f8448a44c282a9d7bf3".getBytes());
//
//
//
//
//
//    /**************************************Deployment Initialization***************************************/
//    static {
//        ABIDecoder decoder = new ABIDecoder(Blockchain.getData());
//        tokenName =  decoder.decodeOneString();
//        Blockchain.require(tokenName.length()>0);
//        tokenSymbol = decoder.decodeOneString();
//        Blockchain.require(tokenSymbol.length()>0);
//        tokenGranularity = decoder.decodeOneInteger();
//        Blockchain.require(tokenGranularity >= 1);
//        tokenTotalSupply = new BigInteger(decoder.decodeOneByteArray());
//        Blockchain.require(tokenTotalSupply.compareTo(BigInteger.ZERO) == 1);
//
//        initialize();
//
//        //ToDo: Register in the AIR
//    }
//
//
//    /********************************************Initialization********************************************/
//    /**
//     * The creator of the token contract holds the total supply.
//     * Log the token creation.
//     */
//    private static void initialize() {
//        Blockchain.putStorage(Blockchain.getCaller().toByteArray(),tokenTotalSupply.toByteArray());
//        ATSTokenContractEvents.ATSTokenCreated(tokenTotalSupply,Blockchain.getCaller());
//    }
//
//
//    /**********************************************Token Info**********************************************/
//
//    @Callable
//    public static String getTokenName() {
//        return tokenName;
//    }
//
//    @Callable
//    public static String getTokenSymbol() {
//        return tokenSymbol;
//    }
//
//    @Callable
//    public static int getTokenGranularity() {
//        return tokenGranularity;
//    }
//
//    @Callable
//    public static String getTokenTotalSupply() {
//        return tokenTotalSupply.toByteArray().toString();
//    }
//
//    /*********************************************Token Holder*********************************************/
//    /**
//     * Return balance in String to make it human readable.
//     * @param tokenHolder
//     * @return
//     */
//    @Callable
//    public static String getBalanceOf(Address tokenHolder) {
//        AionBuffer tokenHolderInformation = AionBuffer.wrap(Blockchain.getStorage(tokenHolder.toByteArray()));
//        return !tokenHolderInformation.equals(null) ? tokenHolderInformation.get32ByteInt().toString() : "0";
//    }
//
//    @Callable
//    public static void authorizeOperator(Address operator) {
//        Blockchain.require(!Blockchain.getCaller().equals(operator));
//        Address tokenHolder = Blockchain.getCaller();
//        AionBuffer tokenHolderInformation = AionBuffer.wrap(Blockchain.getStorage(tokenHolder.toByteArray()));
//
//        if(tokenHolderInformation.equals(null)) { /*No related information yet.
//                                                    Add balance as 0 first to make sure first 32 bytes of token holder information is balance.
//                                                    Following by operators.*/
//            byte[] newInformation =  AionBuffer.allocate(BIGINTEGER_LENGTH + Address.LENGTH)
//                                                .put32ByteInt(BigInteger.ZERO)
//                                                .putAddress(operator)
//                                                .getArray();
//            Blockchain.putStorage(tokenHolder.toByteArray(), newInformation);
//            ATSTokenContractEvents.AuthorizedOperator(operator,tokenHolder);
//        }
//        else {
//            AionSet<Address> operators = getOperatorsFor(tokenHolderInformation);
//            operators.add(operator);
//            byte[] newInformation = updateOperator(tokenHolderInformation.get32ByteInt(),operators).getArray();
//            Blockchain.putStorage(tokenHolder.toByteArray(), newInformation);
//            ATSTokenContractEvents.AuthorizedOperator(operator,tokenHolder);
//        }
//    }
//
//    @Callable
//    public static void revokeOperator(Address operator) {
//        Blockchain.require(!Blockchain.getCaller().equals(operator));
//        Address tokenHolder = Blockchain.getCaller();
//        AionBuffer tokenHolderInformation = AionBuffer.wrap(Blockchain.getStorage(tokenHolder.toByteArray()));
//        if(tokenHolderInformation.equals(null)) return;
//        else {
//            AionSet<Address> operators = getOperatorsFor(tokenHolderInformation);
//            operators.remove(operator);
//            byte[] newInformation = updateOperator(tokenHolderInformation.get32ByteInt(),operators).getArray();
//            Blockchain.putStorage(tokenHolder.toByteArray(), newInformation);
//            ATSTokenContractEvents.RevokedOperator(operator,tokenHolder);
//        }
//    }
//
//    @Callable
//    public static boolean isOperatorFor(Address operator, Address tokenHolder) {
//        if (operator.equals(tokenHolder)) return true;
//
//        AionBuffer tokenHolderInformation = AionBuffer.wrap(Blockchain.getStorage(tokenHolder.toByteArray()));
//        if (tokenHolderInformation.equals(null)) return false;
//        else {
//            AionSet<Address> operators = getOperatorsFor(tokenHolderInformation);
//            return operators.contains(operator);
//        }
//    }
//
//    private static AionSet<Address> getOperatorsFor(AionBuffer tokenHolderInformation) {
//        byte[] tokenHolderInformationArray = tokenHolderInformation.getArray();
//        AionSet<Address> operatorsSet =  new AionSet<>();
//        for (int i = BIGINTEGER_LENGTH; i < tokenHolderInformationArray.length; i = i + Address.LENGTH)
//            operatorsSet.add(new Address(Arrays.copyOfRange(tokenHolderInformationArray,i,i+ADDRESS_LENGTH)));
//        return operatorsSet;
//    }
//
//    private static AionBuffer updateOperator(BigInteger balance,AionSet<Address> operators) {
//        AionBuffer newInformation =  AionBuffer.allocate(BIGINTEGER_LENGTH + operators.size() * ADDRESS_LENGTH);
//        newInformation.put32ByteInt(balance);
//        for (Address o : operators)
//            newInformation.putAddress(o);
//       return newInformation;
//    }
//
//    private static void updateBalance(BigInteger balance, Address tokenHolder) {
//        byte[] tokenHolderInformation = Blockchain.getStorage(tokenHolder.toByteArray());
//        if(tokenHolderInformation.equals(null) || tokenHolderInformation.length == BIGINTEGER_LENGTH ) {
//            Blockchain.putStorage(tokenHolder.toByteArray(),balance.toByteArray());
//        }
//        else {
//            byte[] operators = Arrays.copyOfRange(tokenHolderInformation, BIGINTEGER_LENGTH, tokenHolderInformation.length);
//            byte[] newInformation = AionBuffer.allocate(BIGINTEGER_LENGTH + operators.length)
//                    .put32ByteInt(balance)
//                    .put(operators)
//                    .getArray();
//            Blockchain.putStorage(tokenHolder.toByteArray(), newInformation);
//        }
//    }
//    /********************************************Transactions*********************************************/
//    @Callable
//    public static void send(Address to, byte[] amount, byte[] userData) {
//        doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, new BigInteger(amount), userData, new byte[0], true);
//    }
//
//    @Callable
//    public static void operatorSend(Address from, Address to, byte[] amount, byte[] userData, byte[] operatorData) {
//        Blockchain.require(isOperatorFor(Blockchain.getCaller(),from));
//        doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, new BigInteger(amount), userData, operatorData, true);
//    }
//
//    private static void doSend(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {
//        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO));
//        callSender(operator, from, to, amount, userData, operatorData);
//        Address zeroAddress = new Address(new byte[0]);
//        Blockchain.require(!to.equals(zeroAddress)); //forbid sending to 0x0 (=burning)
//        Blockchain.require(!to.equals(Blockchain.getAddress())); //forbid sending to this contract
//
//        AionBuffer fromTokenHolderInformation = AionBuffer.wrap(Blockchain.getStorage(from.toByteArray()));
//        BigInteger fromBalance = fromTokenHolderInformation.get32ByteInt();
//        Blockchain.require(fromBalance.compareTo(amount) > -1); //ensure enough funds
//        updateBalance(fromBalance.subtract(amount),from);
//
//        AionBuffer toTokenHolderInformation = AionBuffer.wrap(Blockchain.getStorage(to.toByteArray()));
//        BigInteger toBalance = toTokenHolderInformation.get32ByteInt();
//        updateBalance(toBalance.add(amount),to);
//
//        callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
//
//        ATSTokenContractEvents.Sent(operator, from, to, amount, userData, operatorData);
//
//    }
//
//    //ToDO: register to AIR
//    private static void callSender(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {
//
//    }
//
//    private static void callRecipient(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {
//
//    }
//
//    /************************************************Events***********************************************/
//    private static class ATSTokenContractEvents {
//
//        private static void ATSTokenCreated(BigInteger totalSupply, Address creator){
//            Blockchain.log("ATSTokenCreated".getBytes(),
//                    totalSupply.toByteArray(),
//                    creator.toByteArray(),
//                    new byte[0]);
//        }
//
//        /**
//         * Store byte[] sizes for collecting data
//         * @param operator
//         * @param from
//         * @param to
//         * @param amount
//         * @param holderData
//         * @param operatorData
//         */
//        private static void Sent(Address operator, Address from, Address to, BigInteger amount, byte[] holderData, byte[] operatorData) {
//
//            byte[] data = AionBuffer.allocate(BIGINTEGER_LENGTH + Integer.BYTES + holderData.length + Integer.BYTES + operatorData.length)
//                    .put32ByteInt(amount)
//                    .putInt(holderData.length)
//                    .put(holderData)
//                    .putInt(operatorData.length)
//                    .put(operatorData)
//                    .getArray();
//
//            Blockchain.log("Sent".getBytes(),
//                    operator.toByteArray(),
//                    from.toByteArray(),
//                    to.toByteArray(),
//                    data);
//        }
//
//        /**
//         * Store byte[] sizes for collecting data
//         * @param operator
//         * @param from
//         * @param amount
//         * @param holderData
//         * @param operatorData
//         */
//        private static void Burned(Address operator, Address from, BigInteger amount, byte[] holderData, byte[] operatorData) {
//
//            byte[] data = AionBuffer.allocate(BIGINTEGER_LENGTH + Integer.BYTES + holderData.length + Integer.BYTES + operatorData.length)
//                    .put32ByteInt(amount)
//                    .putInt(holderData.length)
//                    .put(holderData)
//                    .putInt(operatorData.length)
//                    .put(operatorData)
//                    .getArray();
//
//            Blockchain.log("Burned".getBytes(),
//                    operator.toByteArray(),
//                    from.toByteArray(),
//                    data);
//        }
//
//        private static void AuthorizedOperator(Address operator, Address tokenHolder) {
//            Blockchain.log("AuthorizedOperator".getBytes(),
//                    operator.toByteArray(),
//                    tokenHolder.toByteArray(),
//                    new byte[0]);
//        }
//
//        private static void RevokedOperator(Address operator, Address tokenHolder) {
//            Blockchain.log("RevokedOperator".getBytes(),
//                    operator.toByteArray(),
//                    tokenHolder.toByteArray(),
//                    new byte[0]);
//        }
//
//    }
//}
//
//
