package atsToken;

import avm.Address;
import org.aion.avm.userlib.AionBuffer;

import java.math.BigInteger;

public class TokenHolderInformation {

    private static int BIGINTEGER_LENGTH = 32;

    protected byte[] currentTokenHolderInformation;
    int currentTokenHolderInfoLength;

    protected TokenHolderInformation(byte[] currentTokenHolderInformation) {
        this.currentTokenHolderInformation = currentTokenHolderInformation;
        this.currentTokenHolderInfoLength = currentTokenHolderInformation.length;
    }

    //ToDo: create a function doesnt require tokenHolderInfo
    protected boolean isOperatorFor(Address operator, byte[] tokenHolderInfo) {
        AionBuffer tokenHolderInformation = AionBuffer.wrap(tokenHolderInfo);
        tokenHolderInformation.get32ByteInt();
        //boolean isOperatorFor = false;
        while (/*!isOperatorFor && */(tokenHolderInformation.getPosition() < tokenHolderInformation.getLimit())) {
            Address operatorWalker = tokenHolderInformation.getAddress();
            if (operator.equals(operatorWalker)) {
                return true;
            }
        }
        return false;
    }

    protected boolean tryAddOperator(Address newOperator) {

        // currenTokenHolderInformation will not be null.
        if (currentTokenHolderInformation.length <= BIGINTEGER_LENGTH) { /*has balance but no operator yet*/
            currentTokenHolderInformation = AionBuffer.allocate(BIGINTEGER_LENGTH + Address.LENGTH)
                    .put32ByteInt(AionBuffer.wrap(this.currentTokenHolderInformation).get32ByteInt())  //balance
                    .putAddress(newOperator)
                    .getArray();
            return true;
        } else{
            //ToDo: use the new func.
            boolean isOperatorFor = isOperatorFor(newOperator, currentTokenHolderInformation);
            if (!isOperatorFor) {
                AionBuffer tokenHolderInfoBuffer = AionBuffer.wrap(currentTokenHolderInformation);
                tokenHolderInfoBuffer.get32ByteInt();
                byte[] newTokenHolderInformation = new byte[this.currentTokenHolderInfoLength + Address.LENGTH];
                System.arraycopy(this.currentTokenHolderInformation, 0,
                        newTokenHolderInformation, 0,
                        currentTokenHolderInfoLength);
                System.arraycopy(newOperator.toByteArray(), 0,
                        newTokenHolderInformation, currentTokenHolderInfoLength,
                        Address.LENGTH);
                currentTokenHolderInformation = newTokenHolderInformation;
                return true;
            }
            return false;
        }
    }

    protected boolean tryReveokeOperator(Address revokeOperator) {
        AionBuffer tokenHolderInfoBuffer =  AionBuffer.wrap(currentTokenHolderInformation);
        tokenHolderInfoBuffer.get32ByteInt();
        //boolean isOperatorFor = false;
        int walker = 0;
        while(/*!isOperatorFor && */(tokenHolderInfoBuffer.getPosition() < tokenHolderInfoBuffer.getLimit())) {
            Address operatorWalker = tokenHolderInfoBuffer.getAddress();
            if(revokeOperator.equals(operatorWalker)) {
                //isOperatorFor = true;
                byte[] newTokenHolderInformation = new byte[this.currentTokenHolderInfoLength - Address.LENGTH];

                int arraySizeBeforeRevokeOperator =  BIGINTEGER_LENGTH + walker * Address.LENGTH;
                System.arraycopy(this.currentTokenHolderInformation,0,
                        newTokenHolderInformation,0,
                        arraySizeBeforeRevokeOperator);
                System.arraycopy(this.currentTokenHolderInformation, arraySizeBeforeRevokeOperator + Address.LENGTH,
                        newTokenHolderInformation,arraySizeBeforeRevokeOperator, (currentTokenHolderInfoLength - arraySizeBeforeRevokeOperator - Address.LENGTH));
                currentTokenHolderInformation = newTokenHolderInformation;
                return true;
            }
            walker++;
        }
        return false;
    }

    protected void updateBalance(BigInteger newBalance) {
        byte[] newBalanceArray = AionBuffer.allocate(BIGINTEGER_LENGTH).put32ByteInt(newBalance).getArray();
        if (this.currentTokenHolderInformation == null) {
            currentTokenHolderInformation = newBalanceArray;
        } else {
            byte[] newTokenHolderInformation = new byte[this.currentTokenHolderInfoLength];
            System.arraycopy(newBalanceArray, 0,
                    newTokenHolderInformation, 0,
                    BIGINTEGER_LENGTH);
            System.arraycopy(this.currentTokenHolderInformation, BIGINTEGER_LENGTH,
                    newTokenHolderInformation, BIGINTEGER_LENGTH,
                    (currentTokenHolderInfoLength - BIGINTEGER_LENGTH));
            currentTokenHolderInformation = newBalanceArray;
        }
    }

    protected BigInteger getBalanceOf() {
        return (this.currentTokenHolderInformation != null)
                ? AionBuffer.wrap(this.currentTokenHolderInformation).get32ByteInt()
                : BigInteger.ZERO;
    }

//
//        private byte[] getData() {
//            return this.currentTokenHolderInformation;
//        }
}
