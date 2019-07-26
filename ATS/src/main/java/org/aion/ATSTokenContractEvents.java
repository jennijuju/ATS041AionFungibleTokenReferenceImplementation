package org.aion;

import avm.Address;
import avm.Blockchain;
import org.aion.avm.userlib.AionBuffer;

import java.math.BigInteger;

public class ATSTokenContractEvents {

    private static int BIGINTEGER_LENGTH = 32;

    protected static void ATSTokenCreated(BigInteger totalSupply, Address creator) {
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
    protected static void Sent(Address operator, Address from, Address to, BigInteger amount, byte[] holderData, byte[] operatorData) {

        if (holderData == null){
            holderData = new byte[0];
        }

        if (operatorData == null){
            operatorData = new byte[0];
        }

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
    protected static void Burned(Address operator, Address from, BigInteger amount, byte[] holderData, byte[] operatorData) {

        if (holderData == null){
            holderData = new byte[0];
        }

        if (operatorData == null){
            operatorData = new byte[0];
        }

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

    protected static void AuthorizedOperator(Address operator, Address tokenHolder) {
        Blockchain.log("AuthorizedOperator".getBytes(),
                operator.toByteArray(),
                tokenHolder.toByteArray(),
                new byte[0]);
    }

    protected static void RevokedOperator(Address operator, Address tokenHolder) {
        Blockchain.log("RevokedOperator".getBytes(),
                operator.toByteArray(),
                tokenHolder.toByteArray(),
                new byte[0]);
    }

    protected static void Thawed(Address localRecipient, byte[] bridgeID, byte[] remoteSender, BigInteger amount, byte[] bridgeData, byte[] remoteBridgeID, byte[] remoteData) {
        if (bridgeID == null){
            bridgeID = new byte[0];
        }

        if (remoteSender == null){
            remoteSender = new byte[0];
        }

        if (bridgeData == null){
            bridgeID = new byte[0];
        }

        if (remoteBridgeID == null){
            remoteBridgeID = new byte[0];
        }

        if (remoteData == null){
            remoteData = new byte[0];
        }
        byte[] data = AionBuffer.allocate(BIGINTEGER_LENGTH + Integer.BYTES + bridgeData.length + Integer.BYTES + remoteBridgeID.length + Integer.BYTES + remoteData.length)
                .put32ByteInt(amount)
                .putInt(bridgeData.length)
                .put(bridgeData)
                .putInt(remoteBridgeID.length)
                .put(remoteBridgeID)
                .putInt(remoteData.length)
                .put(remoteData)
                .getArray();

        Blockchain.log("Thawed".getBytes(),
                localRecipient.toByteArray(),
                bridgeID,
                remoteSender,
                data);

    }

    protected static void Froze(Address localSender, byte[] remoteRecipient, byte[] bridgeID, BigInteger amount, byte[] localData){

        byte[] data = AionBuffer.allocate(BIGINTEGER_LENGTH +  Integer.BYTES + localData.length)
                .put32ByteInt(amount)
                .putInt(localData.length)
                .put(localData)
                .getArray();

        Blockchain.log("Froze".getBytes(),
                localSender.toByteArray(),
                remoteRecipient,
                bridgeID,
                data);
    }



}
