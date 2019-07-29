package org.aion;

import avm.Address;
import avm.Blockchain;
import org.aion.avm.tooling.AddressUtil;
import org.aion.avm.userlib.AionBuffer;

import java.math.BigInteger;

public class ATSStorage {

    protected enum Keys {
        BALANCE,
        IS_OPERATOR
    }

    public static void putBalance(Address tokenHolder, BigInteger amount) {
        Blockchain.putStorage(getBalanceKey(tokenHolder), amount.toByteArray());
    }

    public static void updateOperator(Address operator, Address tokenHolder, boolean isOperator) {
        byte[] isOperatorByte;
        if (isOperator) {
            isOperatorByte = new byte[] {0x01};
        } else {
            isOperatorByte = null;
        }
        Blockchain.putStorage(getIsOperatorKey(operator, tokenHolder), isOperatorByte);
    }

    public static byte[] getBalance(Address tokenHolder) {
        return Blockchain.getStorage(getBalanceKey(tokenHolder));
    }

    public static byte[] getIsOperator(Address operator, Address tokenHolder) {
        return Blockchain.getStorage(getIsOperatorKey(operator,tokenHolder));
    }


    private static byte[] getBalanceKey(Address tokenHolder) {
        return Blockchain.blake2b(AionBuffer.allocate(Integer.BYTES + Address.LENGTH)
                                            .putInt(Keys.BALANCE.ordinal())
                                            .putAddress(tokenHolder)
                                            .getArray());
    }

    private static byte[] getIsOperatorKey(Address tokenHolder, Address operator) {
        return Blockchain.blake2b(AionBuffer.allocate(Integer.BYTES + Address.LENGTH * 2)
                                            .putInt(Keys.IS_OPERATOR.ordinal())
                                            .putAddress(operator)
                                            .putAddress(tokenHolder)
                                            .getArray());
    }



}
