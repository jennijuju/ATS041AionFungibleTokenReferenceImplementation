package org.aion;

import avm.Address;
import avm.Blockchain;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.userlib.abi.ABIDecoder;

import java.math.BigInteger;
import java.util.Arrays;

public class ATSTokenContract {


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
    }


    /********************************************Initialization********************************************/

    private static void initialize() {
        ATSStorage.putBalance(Blockchain.getCaller(), tokenTotalSupply);
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

    @Callable
    public static byte[] balanceOf(Address tokenHolder) {
        byte[] balance = ATSStorage.getBalance(tokenHolder);
        return (balance != null)
                ? balance
                : BigInteger.ZERO.toByteArray();
    }

    @Callable
    public static void authorizeOperator(Address operator) {

        //Should not assign token holder itself to be the operator. Quickly revert the tx to save energy.
        Blockchain.require(!Blockchain.getCaller().equals(operator));

        if (ATSStorage.getIsOperator(operator, Blockchain.getCaller()) == null) { // is not operator
            ATSStorage.updateOperator(operator, Blockchain.getCaller(), true);
            ATSTokenContractEvents.AuthorizedOperator(operator, Blockchain.getCaller());

        }
    }

    @Callable
    public static void revokeOperator(Address operator) {

        if (!Blockchain.getCaller().equals(operator)) {

            if (ATSStorage.getIsOperator(operator, Blockchain.getCaller()) != null) { // is operator
                ATSStorage.updateOperator(operator, Blockchain.getCaller(), false);
                ATSTokenContractEvents.RevokedOperator(operator, Blockchain.getCaller());
            }
        }
    }

    @Callable
    public static boolean isOperatorFor(Address operator, Address tokenHolder) {
        if (operator.equals(tokenHolder)) {
            return true;
        }

        return (Arrays.equals(ATSStorage.getIsOperator(operator, tokenHolder), new byte[] {0x01}))
                ? true
                : false;

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
        Blockchain.require(amount.compareTo(BigInteger.ZERO) > -1); //Amount is not negative value
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO)); //Check granularity
        Blockchain.require(!to.equals(new Address(new byte[32]))); //Forbid sending to 0x0 (=burning)
        Blockchain.require(!to.equals(Blockchain.getAddress())); //Forbid sending to this contract

        callSender(operator, from, to, amount, userData, operatorData);

        byte[] fromBalance = ATSStorage.getBalance(from);
        Blockchain.require(fromBalance != null); //Revert transaction if sender does not have a balance at all quickly to save energy
        Blockchain.require(new BigInteger(fromBalance).compareTo(amount) > -1); // Sender has sufficient balance
        ATSStorage.putBalance(from, new BigInteger(fromBalance).subtract(amount));// Update the sender balance


        byte[] toBalance =  ATSStorage.getBalance(to);
        if(toBalance != null) { //Receiver has a balanace
            ATSStorage.putBalance(to, new BigInteger(toBalance).add(amount));
            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            ATSTokenContractEvents.Sent(operator, from, to, amount, userData, operatorData);

        } else { //Receiver is a new token holder
            ATSStorage.putBalance(to, amount);
            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            ATSTokenContractEvents.Sent(operator, from, to, amount, userData, operatorData);
        }
    }

    private static void doBurn(Address operator, Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
        Blockchain.require(amount.compareTo(BigInteger.ZERO) > -1); //Amount is not a negative number
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO));

        byte[] balance = ATSStorage.getBalance(tokenHolder);
        Blockchain.require(balance != null); //Token holder has sufficient balance to burn
        Blockchain.require(new BigInteger(balance).compareTo(BigInteger.ZERO) > -1); //Token Holder has sufficient balance to burn
        ATSStorage.putBalance(tokenHolder, new BigInteger(balance).subtract(amount));

        tokenTotalSupply = tokenTotalSupply.subtract(amount);

        callSender(operator, tokenHolder, new Address(new byte[32]), amount, holderData, operatorData);ATSTokenContractEvents.Burned(operator, tokenHolder, amount, holderData, operatorData);
    }

    private static void callSender(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {

    }

    private static void callRecipient(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {

    }

    private static boolean isRegularAccount(Address address) {
        return (Blockchain.getCodeSize(address) > 0) ? true : false;
    }
}




