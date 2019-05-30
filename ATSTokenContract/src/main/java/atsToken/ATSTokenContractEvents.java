package atsToken;

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

}
