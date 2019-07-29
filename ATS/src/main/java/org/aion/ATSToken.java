package org.aion;

import avm.Address;
import avm.Blockchain;
import org.aion.avm.userlib.AionBuffer;

import java.math.BigInteger;
import java.util.Arrays;



public class ATSToken


        implements ATSInterface {


    /**************************************Deployment Initialization***************************************/

    private String tokenName;

    private String tokenSymbol;

    private int tokenGranularity;

    private BigInteger tokenTotalSupply;

    ATSToken(String tokenName, String tokenSymbol, int tokenGranularity, BigInteger tokenTotalSupply) {

       this.tokenName = tokenName;
       this.tokenSymbol = tokenSymbol;
       this.tokenGranularity = tokenGranularity;
       this.tokenTotalSupply = tokenTotalSupply;

       initialize();
    }


    /********************************************Initialization********************************************/

    private void initialize() {
        Blockchain.putStorage(Blockchain.getOrigin().toByteArray(), tokenTotalSupply.toByteArray());
        ATSTokenContractEvents.ATSTokenCreated(tokenTotalSupply, Blockchain.getOrigin());
    }


    /**********************************************Token Info**********************************************/

    public String name() {
        return tokenName;
    }

    public String symbol() {
        return tokenSymbol;
    }


    public int granularity() {
        return tokenGranularity;
    }

    public byte[] totalSupply() {
        return tokenTotalSupply.toByteArray();
    }

    /*********************************************Token Holder*********************************************/

    public byte[] balanceOf(Address tokenHolder) {
        byte[] balance = Blockchain.getStorage(tokenHolder.toByteArray());
        return (balance != null)
                ? balance
                : BigInteger.ZERO.toByteArray();
    }

    public void authorizeOperator(Address operator) {

        //Should not assign token holder itself to be the operator. Quickly revert the tx to save energy.
        Blockchain.require(!Blockchain.getOrigin().equals(operator));


        byte[] operatorAndTokenHolder = AionBuffer.allocate(Address.LENGTH * 2)
                                                  .putAddress(operator).putAddress(Blockchain.getOrigin())
                                                  .getArray();

        if (Blockchain.getStorage(Blockchain.blake2b(operatorAndTokenHolder)) == null) { // is not operator
            Blockchain.putStorage(Blockchain.blake2b(operatorAndTokenHolder), new byte[] {0x01});
            ATSTokenContractEvents.AuthorizedOperator(operator, Blockchain.getOrigin());

        }
    }

    public  void revokeOperator(Address operator) {

        if (!Blockchain.getOrigin().equals(operator)) {

            byte[] operatorAndTokenHolder = AionBuffer.allocate(Address.LENGTH * 2)
                    .putAddress(operator).putAddress(Blockchain.getOrigin())
                    .getArray();

            if (Blockchain.getStorage(Blockchain.blake2b(operatorAndTokenHolder)) != null) { // is operator
                Blockchain.putStorage(Blockchain.blake2b(operatorAndTokenHolder), null);
                ATSTokenContractEvents.RevokedOperator(operator, Blockchain.getOrigin());
            }
        }
    }


    public boolean isOperatorFor(Address operator, Address tokenHolder) {
        if (operator.equals(tokenHolder)) {
            return true;
        }

        byte[] operatorAndTokenHolder = AionBuffer.allocate(Address.LENGTH * 2)
                .putAddress(operator).putAddress(tokenHolder)
                .getArray();

        return (Arrays.equals(Blockchain.getStorage(Blockchain.blake2b(operatorAndTokenHolder)), new byte[] {0x01}))
                ? true
                : false;

    }

    /******************************************Token Movement*******************************************/
    public void send(Address to, byte[] amount, byte[] userData) {
        doSend(Blockchain.getOrigin(), Blockchain.getOrigin(), to, new BigInteger(amount), userData, new byte[0], true);
    }

    public void operatorSend(Address from, Address to, byte[] amount, byte[] userData, byte[] operatorData) {
        Blockchain.require(isOperatorFor(Blockchain.getOrigin(),from));
        doSend(Blockchain.getOrigin(), from, to, new BigInteger(amount), userData, operatorData, true);
    }

    public void burn(byte[] amount, byte[] holderData) {
        doBurn(Blockchain.getOrigin(),Blockchain.getOrigin(), new BigInteger(amount) ,holderData, new byte[0]);
    }

    public void operatorBurn(Address tokenHolder, byte[] amount, byte[] holderData, byte[] operatorData) {
        Blockchain.require(isOperatorFor(Blockchain.getOrigin(), tokenHolder));
        doBurn(Blockchain.getOrigin(), tokenHolder, new BigInteger(amount), holderData, new byte[0]);
    }

    private void doSend(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {
        Blockchain.require(amount.compareTo(BigInteger.ZERO) > -1); //Amount is not negative value
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO)); //Check granularity
        Blockchain.require(!to.equals(new Address(new byte[32]))); //Forbid sending to 0x0 (=burning)
        Blockchain.require(!to.equals(Blockchain.getAddress())); //Forbid sending to this contract

        callSender(operator, from, to, amount, userData, operatorData);

        byte[] fromBalance = Blockchain.getStorage(from.toByteArray());
        Blockchain.require(fromBalance != null); //Revert transaction if sender does not have a balance at all quickly to save energy
        Blockchain.require(new BigInteger(fromBalance).compareTo(amount) > -1); // Sender has sufficient balance
        Blockchain.putStorage(from.toByteArray(), new BigInteger(fromBalance).subtract(amount).toByteArray()); // Update the sender balance


        byte[] toBalance = Blockchain.getStorage(to.toByteArray());
        if(toBalance != null) { //Receiver has a balanace
            Blockchain.putStorage(to.toByteArray(), new BigInteger(toBalance).add(amount).toByteArray());
            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            ATSTokenContractEvents.Sent(operator, from, to, amount, userData, operatorData);

        } else { //Receiver is a new token holder
            Blockchain.putStorage(to.toByteArray(), amount.toByteArray());
            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            ATSTokenContractEvents.Sent(operator, from, to, amount, userData, operatorData);
        }
    }

    private void doBurn(Address operator, Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
        Blockchain.require(amount.compareTo(BigInteger.ZERO) > -1); //Amount is not a negative number
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO));

        byte[] balance = Blockchain.getStorage(tokenHolder.toByteArray());
        Blockchain.require(balance != null); //Token holder has sufficient balance to burn
        Blockchain.require(new BigInteger(balance).compareTo(BigInteger.ZERO) > -1); //Token Holder has sufficient balance to burn
        Blockchain.putStorage(tokenHolder.toByteArray(), new BigInteger(balance).subtract(amount).toByteArray()); //Update balance

        tokenTotalSupply = tokenTotalSupply.subtract(amount);

        callSender(operator, tokenHolder, new Address(new byte[32]), amount, holderData, operatorData);ATSTokenContractEvents.Burned(operator, tokenHolder, amount, holderData, operatorData);
    }

    //TODO: check if its an account or contract
    private void callSender(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {

    }

    private void callRecipient(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {

    }

    private static boolean isRegularAccount(Address address) {
        return (Blockchain.getCodeSize(address) > 0) ? true : false;
    }




}




