package org.aion;

import avm.Address;
import avm.Blockchain;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.userlib.abi.ABIDecoder;

import java.math.BigInteger;

public  class MyATSContract {

    private static ATSToken ats;

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

        ats = new ATSToken(tokenName, tokenSymbol, tokenGranularity, tokenTotalSupply);
    }
    

    /**********************************************Token Info**********************************************/
    @Callable
    public static String name() {
        return ats.name();
    }

    @Callable
    public static String symbol() {
        return ats.symbol();
    }

    @Callable
    public static int granularity() {
        return ats.granularity();
    }

    @Callable
    public static byte[] totalSupply() {
        return ats.totalSupply();
    }

    /*********************************************Token Holder*********************************************/
    @Callable
    public static byte[] balanceOf(Address tokenHolder) {
        return ats.balanceOf(tokenHolder);
    }

    @Callable
    public static void authorizeOperator(Address operator) {
       ats.authorizeOperator(operator);
    }

    @Callable
    public static  void revokeOperator(Address operator) {
        ats.revokeOperator(operator);
    }

    @Callable
    public static boolean isOperatorFor(Address operator, Address tokenHolder) {
        return ats.isOperatorFor(operator,tokenHolder);

    }

    /******************************************Token Movement*******************************************/
    @Callable
    public static void send(Address to, byte[] amount, byte[] userData) {
        ats.send(to, amount, userData);
    }

    @Callable
    public static void operatorSend(Address from, Address to, byte[] amount, byte[] userData, byte[] operatorData) {
       ats.operatorSend(from, to, amount, userData, operatorData);
    }

    @Callable
    public static void burn(byte[] amount, byte[] holderData) {
        ats.burn(amount, holderData);
    }

    @Callable
    public static void operatorBurn(Address tokenHolder, byte[] amount, byte[] holderData, byte[] operatorData) {
       ats.operatorBurn(tokenHolder, amount, holderData, operatorData);
    }

    /*********************************************Cross Chain (Additional features) *******************************************/
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

    @Callable
    public static byte[] liquidSupply() {
        return new byte[0];
    }


}
