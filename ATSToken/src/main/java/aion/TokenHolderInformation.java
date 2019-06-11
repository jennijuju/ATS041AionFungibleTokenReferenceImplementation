package aion;

import avm.Address;
import org.aion.avm.userlib.AionBuffer;

import java.math.BigInteger;

public class TokenHolderInformation {

    private static int BIGINTEGER_LENGTH = 32;

    protected byte[] currentTokenHolderInformation;

    protected TokenHolderInformation(byte[] currentTokenHolderInformation) {
        this.currentTokenHolderInformation = currentTokenHolderInformation;
    }

    protected boolean isOperatorFor(Address operator, byte[] tokenHolderInfo) {
        AionBuffer tokenHolderInformationBuffer = AionBuffer.wrap(tokenHolderInfo);
        tokenHolderInformationBuffer.get32ByteInt();
        while ((tokenHolderInformationBuffer.getPosition() < tokenHolderInformationBuffer.getLimit())) {
            Address operatorWalker = tokenHolderInformationBuffer.getAddress();
            if (operator.equals(operatorWalker)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isOperatorFor(Address operator) {
        AionBuffer tokenHolderInformationBuffer = AionBuffer.wrap(this.currentTokenHolderInformation);
        tokenHolderInformationBuffer.get32ByteInt();
        while ((tokenHolderInformationBuffer.getPosition() < tokenHolderInformationBuffer.getLimit())) {
            Address operatorWalker = tokenHolderInformationBuffer.getAddress();
            if (operator.equals(operatorWalker)) {
                return true;
            }
        }
        return false;
    }

    protected boolean tryAddOperator(Address newOperator) {

        // currenTokenHolderInformation will not be null.
        if (this.currentTokenHolderInformation.length <= BIGINTEGER_LENGTH) { /*has balance but no operator yet*/
            this.currentTokenHolderInformation = AionBuffer.allocate(BIGINTEGER_LENGTH + Address.LENGTH)
                    .put32ByteInt(AionBuffer.wrap(this.currentTokenHolderInformation).get32ByteInt())  //balance
                    .putAddress(newOperator)
                    .getArray();
            return true;
        } else{
            boolean isOperatorFor = isOperatorFor(newOperator);
            if (!isOperatorFor) {
                AionBuffer tokenHolderInformationBuffer = AionBuffer.wrap(this.currentTokenHolderInformation);
                tokenHolderInformationBuffer.get32ByteInt();
                byte[] newTokenHolderInformation = new byte[this.currentTokenHolderInformation.length + Address.LENGTH];
                System.arraycopy(this.currentTokenHolderInformation, 0,
                        newTokenHolderInformation, 0,
                        this.currentTokenHolderInformation.length);
                System.arraycopy(newOperator.toByteArray(), 0,
                        newTokenHolderInformation, this.currentTokenHolderInformation.length,
                        Address.LENGTH);
                this.currentTokenHolderInformation = newTokenHolderInformation;
                return true;
            }
            return false;
        }
    }

    protected boolean tryReveokeOperator(Address revokeOperator) {
        AionBuffer tokenHolderInformationBuffer =  AionBuffer.wrap(this.currentTokenHolderInformation);
        tokenHolderInformationBuffer.get32ByteInt();
        int walker = 0;
        while(tokenHolderInformationBuffer.getPosition() < tokenHolderInformationBuffer.getLimit()) {
            Address operatorWalker = tokenHolderInformationBuffer.getAddress();
            if(revokeOperator.equals(operatorWalker)) {

                byte[] newTokenHolderInformation = new byte[this.currentTokenHolderInformation.length - Address.LENGTH];

                int arraySizeBeforeRevokeOperator =  BIGINTEGER_LENGTH + walker * Address.LENGTH;
                System.arraycopy(this.currentTokenHolderInformation,0,
                        newTokenHolderInformation,0,
                        arraySizeBeforeRevokeOperator);
                System.arraycopy(this.currentTokenHolderInformation, arraySizeBeforeRevokeOperator + Address.LENGTH,
                        newTokenHolderInformation,arraySizeBeforeRevokeOperator, (this.currentTokenHolderInformation.length - arraySizeBeforeRevokeOperator - Address.LENGTH));
                this.currentTokenHolderInformation = newTokenHolderInformation;
                return true;
            }
            walker++;
        }
        return false;
    }

    protected void updateBalance(BigInteger newBalance) {
        byte[] newBalanceArray = AionBuffer.allocate(BIGINTEGER_LENGTH).put32ByteInt(newBalance).getArray();
        if (this.currentTokenHolderInformation == null) {
            this.currentTokenHolderInformation = newBalanceArray;
        } else {
            byte[] newTokenHolderInformation = new byte[this.currentTokenHolderInformation.length];
            System.arraycopy(newBalanceArray, 0,
                    newTokenHolderInformation, 0,
                    BIGINTEGER_LENGTH);
            System.arraycopy(this.currentTokenHolderInformation, BIGINTEGER_LENGTH,
                    newTokenHolderInformation, BIGINTEGER_LENGTH,
                    (this.currentTokenHolderInformation.length - BIGINTEGER_LENGTH));
            this.currentTokenHolderInformation = newBalanceArray;
        }
    }

    protected BigInteger getBalanceOf() {
        return (this.currentTokenHolderInformation != null)
                ? AionBuffer.wrap(this.currentTokenHolderInformation).get32ByteInt()
                : BigInteger.ZERO;
    }

}
