package org.aion;

import avm.Address;
import avm.Blockchain;
import org.aion.avm.userlib.AionBuffer;

import java.math.BigInteger;

public class AIP041KeyValueStorage {

    protected enum Keys {
        BALANCE,
        IS_OPERATOR
    }

    public static byte[] getBalanceKey(Address tokenHolder) {
        return Blockchain.blake2b(AionBuffer.allocate(Integer.BYTES + Address.LENGTH)
                                            .putInt(Keys.BALANCE.hashCode())
                                            .putAddress(tokenHolder)
                                            .getArray());
    }

    public static byte[] getIsOperatorKey(Address tokenHolder, Address operator) {
        return Blockchain.blake2b(AionBuffer.allocate(Integer.BYTES + Address.LENGTH * 2)
                                            .putInt(Keys.IS_OPERATOR.hashCode())
                                            .putAddress(operator)
                                            .putAddress(tokenHolder)
                                            .getArray());
    }
}
