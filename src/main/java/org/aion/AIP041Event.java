package org.aion;

import avm.Address;
import avm.Blockchain;
import org.aion.avm.userlib.AionBuffer;

import java.math.BigInteger;

public class AIP041Event {

    private static int BIGINTEGER_LENGTH = 32;

    protected static void AIP041AIPTokenCreated(String tokenName, String tokenSymbol, int tokenGranularity, Address creator) {
        Blockchain.log("AIP041TokenCreated".getBytes(),
                tokenName.getBytes(),
                tokenSymbol.getBytes(),
                padding(BigInteger.valueOf(tokenGranularity)),
                creator.toByteArray());
    }

    protected static void AIP041AIPTokenMinted(Address issuer, Address to, BigInteger amount, byte[] data, byte[] issuerData) {

        if (data == null){
            data = new byte[0];
        }

        if (issuerData == null){
            issuerData = new byte[0];
        }

        byte[] eventData = AionBuffer.allocate(Integer.BYTES + data.length + Integer.BYTES + issuerData.length)
                .putInt(data.length)
                .put(data)
                .putInt(issuerData.length)
                .put(issuerData)
                .getArray();

        Blockchain.log("AIP041Minted".getBytes(),
                        issuer.toByteArray(),
                        to.toByteArray(),
                        padding(amount),
                        eventData);
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
    protected static void AIP041Sent(Address operator, Address from, Address to, BigInteger amount, byte[] holderData, byte[] operatorData) {

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

        Blockchain.log("AIP041Sent".getBytes(),
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
    protected static void AIP041Burned(Address operator, Address from, BigInteger amount, byte[] holderData, byte[] operatorData) {

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

        Blockchain.log("AIP041Burned".getBytes(),
                operator.toByteArray(),
                from.toByteArray(),
                data);
    }

    protected static void AIP041AuthorizedOperator(Address operator, Address tokenHolder) {
        Blockchain.log("AIP041AuthorizedOperator".getBytes(),
                operator.toByteArray(),
                tokenHolder.toByteArray(),
                new byte[0]);
    }

    protected static void AIP041RevokedOperator(Address operator, Address tokenHolder) {
        Blockchain.log("AIP041RevokedOperator".getBytes(),
                operator.toByteArray(),
                tokenHolder.toByteArray(),
                new byte[0]);
    }

    protected static void AIP041AddedTokenIssuer(Address newTokenIssuer) {
        Blockchain.log("AIP041AddedTokenIssuer".getBytes(), newTokenIssuer.toByteArray(), new byte[0]);
    }

    protected static void AIP041RemovedTokenIssuer(Address oldTokenIssuer) {
        Blockchain.log("AIP041RemovedTokenIssuer".getBytes(), oldTokenIssuer.toByteArray(), new byte[0]);
    }

    private static byte[] padding(BigInteger value) {
        byte[] valueArray = value.toByteArray();
        byte[] paddedArray = new byte[32];
        System.arraycopy(valueArray, 0, paddedArray,32-valueArray.length, valueArray.length);
        return paddedArray;
    }

}
