package org.aion;

import avm.Blockchain;
import avm.Address;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.tooling.abi.Initializable;

import java.math.BigInteger;
import java.util.Arrays;

public class AIP041Standard {


    /**************************************Deployment Initialization***************************************/
    @Initializable
    private static String tokenName ;

    @Initializable
    private static String tokenSymbol;

    @Initializable
    private static int tokenGranularity;

    @Initializable
    private static BigInteger tokenTotalSupply;

    static {

        Blockchain.require(tokenName.length() > 0);
        Blockchain.require(tokenSymbol.length() > 0);
        Blockchain.require(tokenGranularity >= 1);
        Blockchain.require(tokenTotalSupply.compareTo(BigInteger.ZERO) == 1);

        initialize();
    }


    /********************************************Initialization********************************************/

    private static void initialize() {
        Blockchain.putStorage(AIP041KeyValueStorage.getBalanceKey(Blockchain.getCaller()), tokenTotalSupply.toByteArray());
        AIP041Event.ATSTokenCreated(tokenTotalSupply, Blockchain.getCaller());
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
    public static BigInteger totalSupply() {
        return tokenTotalSupply;
    }

    /*********************************************Token Holder*********************************************/

    @Callable
    public static BigInteger balanceOf(Address tokenHolder) {
        byte[] balance = Blockchain.getStorage(AIP041KeyValueStorage.getBalanceKey(tokenHolder));
        return (balance != null)
                ? new BigInteger(balance)
                : BigInteger.ZERO;
    }

    @Callable
    public static void authorizeOperator(Address operator) {

        //Should not assign token holder itself to be the operator. Quickly revert the tx to save energy.
        Blockchain.require(!Blockchain.getCaller().equals(operator));
        if (Blockchain.getStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller())) == null) { // is not operator
            Blockchain.putStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller()), new byte[] {0x01});
            AIP041Event.AuthorizedOperator(operator, Blockchain.getCaller());

        }
    }

    @Callable
    public static void revokeOperator(Address operator) {

        if (!Blockchain.getCaller().equals(operator)) {

            if (Blockchain.getStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller())) != null) { // is operator
                Blockchain.putStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller()), null);
                AIP041Event.RevokedOperator(operator, Blockchain.getCaller());
            }
        }
    }

    @Callable
    public static boolean isOperatorFor(Address operator, Address tokenHolder) {
        if (operator.equals(tokenHolder)) {
            return true;
        }

        return (Arrays.equals(Blockchain.getStorage(AIP041KeyValueStorage.getIsOperatorKey(operator, tokenHolder)), new byte[] {0x01}))
                ? true
                : false;

    }

    /******************************************Token Movement*******************************************/
    @Callable
    public static void send(Address to, BigInteger amount, byte[] userData) {
        doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, amount, userData, new byte[0], true);
    }

    @Callable
    public static void operatorSend(Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {
        Blockchain.require(isOperatorFor(Blockchain.getCaller(),from));
        doSend(Blockchain.getCaller(), from, to, amount, userData, operatorData, true);
    }

    @Callable
    public static void burn(BigInteger amount, byte[] holderData) {
        doBurn(Blockchain.getCaller(),Blockchain.getCaller(), amount ,holderData, new byte[0]);
    }


    @Callable
    public static void operatorBurn(Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
        Blockchain.require(isOperatorFor(Blockchain.getCaller(), tokenHolder));
        doBurn(Blockchain.getCaller(), tokenHolder, amount, holderData, new byte[0]);
    }

    private static void doSend(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {
        Blockchain.require(amount.compareTo(BigInteger.ZERO) > -1); //Amount is not negative value
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO)); //Check granularity
        Blockchain.require(!to.equals(new Address(new byte[32]))); //Forbid sending to 0x0 (=burning)
        Blockchain.require(!to.equals(Blockchain.getAddress())); //Forbid sending to this contract

        callSender(operator, from, to, amount, userData, operatorData);

        byte[] fromBalance = Blockchain.getStorage(AIP041KeyValueStorage.getBalanceKey(from));
        Blockchain.require(fromBalance != null); //Revert transaction if sender does not have a balance at all quickly to save energy
        Blockchain.require(new BigInteger(fromBalance).compareTo(amount) > -1); // Sender has sufficient balance
        Blockchain.putStorage(AIP041KeyValueStorage.getBalanceKey(from), new BigInteger(fromBalance).subtract(amount).toByteArray());


        byte[] toBalance = Blockchain.getStorage(AIP041KeyValueStorage.getBalanceKey(to));
        if(toBalance != null) { //Receiver has a balanace
            Blockchain.putStorage(AIP041KeyValueStorage.getBalanceKey(to), new BigInteger(toBalance).add(amount).toByteArray());
            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            AIP041Event.Sent(operator, from, to, amount, userData, operatorData);

        } else { //Receiver is a new token holder
            Blockchain.putStorage(AIP041KeyValueStorage.getBalanceKey(to), amount.toByteArray());

            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            AIP041Event.Sent(operator, from, to, amount, userData, operatorData);
        }
    }

    private static void doBurn(Address operator, Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
        Blockchain.require(amount.compareTo(BigInteger.ZERO) > -1); //Amount is not a negative number
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO));

        byte[] balance =Blockchain.getStorage(AIP041KeyValueStorage.getBalanceKey(tokenHolder));
        Blockchain.require(balance != null); //Token holder has sufficient balance to burn
        Blockchain.require(new BigInteger(balance).compareTo(BigInteger.ZERO) > -1); //Token Holder has sufficient balance to burn
        Blockchain.putStorage(AIP041KeyValueStorage.getBalanceKey(tokenHolder), new BigInteger(balance).subtract(amount).toByteArray());

        tokenTotalSupply = tokenTotalSupply.subtract(amount);

        callSender(operator, tokenHolder, new Address(new byte[32]), amount, holderData, operatorData);
        AIP041Event.Burned(operator, tokenHolder, amount, holderData, operatorData);
    }

    private static void callSender(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {

    }

    private static void callRecipient(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {

    }

    private static boolean isRegularAccount(Address address) {
        return (Blockchain.getCodeSize(address) > 0) ? true : false;
    }

}




