package org.aion;


import avm.Address;

interface ATSTokenInterface {
    
    /**********************************************Token Info**********************************************/


    public String name();

         
    public String symbol();

    public int granularity();

    public byte[] totalSupply();

    /*********************************************Token Holder*********************************************/

    public byte[] balanceOf(Address tokenHolder);

    public void authorizeOperator(Address operator);

    public void revokeOperator(Address operator);

    public boolean isOperatorFor(Address operator, Address tokenHolder);

    /******************************************Token Movement*******************************************/
    public void send(Address to, byte[] amount, byte[] userData);

    public void operatorSend(Address from, Address to, byte[] amount, byte[] userData, byte[] operatorData);

    public void burn(byte[] amount, byte[] holderData);

    public void operatorBurn(Address tokenHolder, byte[] amount, byte[] holderData, byte[] operatorData);


    /*********************************************Cross Chain *******************************************/
    public void thaw (Address localRecipient, byte[] amount, byte[] bridgeId, byte[] bridgeData,
                             byte[] remoteSender, byte[] remoteBridgeId, byte[] remoteData);

    public void freeze(byte[] remoteRecipient, byte[] amount, byte[] bridgeId, byte[] localData);

    public void operatorFreeze(Address localSender, byte[] remoteRecipient, byte[] amount, byte[] bridgeId,
                                      byte[] localData);
    public byte[] getLiquidSupply();
}
