package org.aion;

import avm.Blockchain;
import avm.Address;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Reference contract implementation for AIP-041 Aion fungible Token(https://github.com/aionnetwork/AIP/issues/41).
 * This token is a non fixed supply token.
 * All tokens are minted to token deployer upon token creation.
 *
 * This code is pending audit.
 * Do not use or deploy this code before reviewing it personally first.
 *
 * @author:
 * Jiaying Wang(https://github.com/jennijuju)
 *
 *
 */
public class AIP041Implementation {

    protected static String tokenName ;
    protected static String tokenSymbol;
    protected static int tokenGranularity;
    protected static BigInteger tokenTotalSupply;

    /********************************************Initialization********************************************/
    /**
     * Total number of the minted token upon token creation is initialized under the token creator's account.
     * "AIPTokenCreated" event is emitted upon deployment.
     */
    protected static void create() {
        Blockchain.putStorage(AIP041KeyValueStorage.AIP041GetBalanceKey(Blockchain.getCaller()), tokenTotalSupply.toByteArray());
        AIP041Event.AIP041AIPTokenCreated(tokenTotalSupply, Blockchain.getCaller(), tokenName, tokenGranularity, tokenSymbol);
        AIP041Event.AIP041Minted(Blockchain.getCaller(), Blockchain.getCaller(), tokenTotalSupply, null, null);
    }

    /**********************************************Token Info**********************************************/

    /**
     * Get the name of the token.
     * @return The name of the token.
     */
    protected static String AIP041Name() {
        return tokenName;
    }

    /**
     * Get the symbol of the token.
     * @return The symbol of the token.
     */
    protected static String AIP041Symbol() {
        return tokenSymbol;
    }

    /**
     * Get the granularity of the token.
     * @return The granularity of the token.
     */
    protected static int AIP041Granularity() {
        return tokenGranularity;
    }

    /**
     * Get the total supply of the token.
     * @return The total supply of the token.
     */
    protected static BigInteger AIP041TotalSupply() {
        return tokenTotalSupply;
    }

    /*********************************************Token Holder*********************************************/

    /**
     * Get the balance of an account.
     * @param tokenHolder An account that has ownership over a token balance.
     * @return The token balance of the account.
     */
    protected static BigInteger AIP041BalanceOf(Address tokenHolder) {
        return getBalance(AIP041KeyValueStorage.AIP041GetBalanceKey(tokenHolder));
    }

    /**
     * Set a third party operator address as an operator of Blockchain.getCaller() to send and burn tokens on its behalf.
     * A token holder MAY authorize an already authorized operator.
     * An AuthorizedOperator MUST be emitted each time.
     * @param operator  Address to set as an operator for Blockchain.getCaller()
     *
     */
    protected static void AIP041AuthorizeOperator(Address operator) {
        Blockchain.putStorage(AIP041KeyValueStorage.AIP041GetIsOperatorKey(operator,Blockchain.getCaller()), new byte[] {0x01});
        AIP041Event.AIP041AuthorizedOperator(operator, Blockchain.getCaller());
    }

    /**
     * Remove the right of the operator address to be an operator for Blockchain.getCaller() to send and burn tokens on its behalf.
     * The token holder (Blockchain.getCaller()) is always an operator for itself. This right MUST NOT be revoked. This function MUST revert if it is called to revoke the token holder.
     * A RevokeOperator MUST be emitted each time.
     * @param operator  Address to rescind as an operator for Blockchain.getCaller().
     */
    protected static void AIP041RevokeOperator(Address operator) {
        Blockchain.require(!operator.equals(Blockchain.getCaller()));  //An address MUST always be an operator for itself.
        Blockchain.putStorage(AIP041KeyValueStorage.AIP041GetIsOperatorKey(operator,Blockchain.getCaller()), null);
        AIP041Event.AIP041RevokedOperator(operator, Blockchain.getCaller());
    }

    /**
     * Indicate whether the operator address is an operator of the tokenHolder address.
     * A token holder is always an operator of itself.
     * @param operator Address which may be an operator of tokenHolder.
     * @param tokenHolder Address of a token holder which may have the operator address as an operator.
     * @return true if operator is an operator of tokenHolder and false otherwise.
     */
    protected static boolean AIP041IsOperatorFor(Address operator, Address tokenHolder) {
        return operator.equals(tokenHolder) ||
                (Blockchain.getStorage(AIP041KeyValueStorage.AIP041GetIsOperatorKey(operator, tokenHolder)) == null ? false : true);
    }

    /******************************************Token Movement*******************************************/

    /**
     * Send the amount of tokens from the address Blockchain.getCaller() to the address to.
     * The operator and the token holder MUST both be the Blockchain.getCaller().
     * @param to Token recipient.
     * @param amount Number of tokens to send.
     * @param userData Information attached to the send, and intended for the recipient (to).
     */
    protected static void AIP041Send(Address to, BigInteger amount, byte[] userData) {
        doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, amount, userData, new byte[0]);
    }

    /**
     * Send the amount of tokens on behalf of the address from to the address to.
     * The operator MUST be Blockchain.getCaller().
     * The value of from MAY be 0x0, then the from (token holder) used for the send MUST be Blockchain.getCaller() (the operator).
     * @param from Token holder.
     * @param to Token recipient.
     * @param amount  Number of tokens to send.
     * @param userData Information attached to the send, and intended for the recipient (to).
     * @param operatorData Information attached to the send by the operator.
     */
    protected static void AIP041OperatorSend(Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {
        if (from.equals(new Address(new byte[32]))) {
            doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, amount, userData, operatorData);
        } else {
            Blockchain.require(AIP041IsOperatorFor(Blockchain.getCaller(),from));
            doSend(Blockchain.getCaller(), from, to, amount, userData, operatorData);
        }
    }

    /**
     * Burn the amount of tokens from the address Blockchain.getCaller().
     * The operator and the token holder MUST both be the Blockchain.getCaller().
     * @param amount Number of tokens to burn.
     * @param holderData Information attached to the burn by the token holder.
     */
    protected static void AIP041Burn(BigInteger amount, byte[] holderData) {
        doBurn(Blockchain.getCaller(),Blockchain.getCaller(), amount ,holderData, new byte[0]);
    }

    /**
     * Burn the amount of tokens on behalf of the address from.
     * The operator MUST be Blockchain.getCaller().
     * The value of from MAY be 0x0, then the from (token holder) used for the burn MUST be Blockchain.getCaller() (the operator).
     * If the operator address is not an authorized operator of the from address, then the burn process MUST revert.
     * @param tokenHolder Token holder whose tokens will be burned (or 0x0 to set from to Blockchain.getCaller()).
     * @param amount Number of tokens to burn.
     * @param holderData Information attached to the burn by the token holder.
     * @param operatorData Information attached to the burn by the operator.
     */
    protected static void AIP041OperatorBurn(Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
        if (tokenHolder.equals(new Address(new byte[32]))) {
            doBurn(Blockchain.getCaller(), Blockchain.getCaller(), amount, holderData,operatorData);
        } else {
            Blockchain.require(AIP041IsOperatorFor(Blockchain.getCaller(), tokenHolder));
            doBurn(Blockchain.getCaller(), tokenHolder, amount, holderData, operatorData);
        }
    }

    private static void doSend(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {
        Blockchain.require(amount.signum() >= 0); //Amount is not negative value
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO));
        Blockchain.require(!to.equals(Blockchain.getAddress()));

        byte[] fromBalanceKey = AIP041KeyValueStorage.AIP041GetBalanceKey(from);
        BigInteger fromBalance = getBalance(fromBalanceKey);
        Blockchain.require(fromBalance.compareTo(amount) >= 0); //Revert transaction if sender does not have a balance at all quickly to save energy

        AIP041Event.AIP041Sent(operator, from, to, amount, userData, operatorData);

        byte[] toBalanceKey = AIP041KeyValueStorage.AIP041GetBalanceKey(to);
        BigInteger toBalance = getBalance(toBalanceKey);
        Blockchain.putStorage(toBalanceKey, toBalance.add(amount).toByteArray());
        Blockchain.putStorage(fromBalanceKey, fromBalance.subtract(amount).toByteArray());
    }

    private static void doBurn(Address operator, Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
        Blockchain.require(!tokenHolder.equals(new Address(new byte[32])));
        Blockchain.require(amount.signum() >= 0); //Amount is not a negative number
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO));

        byte[] balanceKey = AIP041KeyValueStorage.AIP041GetBalanceKey(tokenHolder);
        BigInteger balance = getBalance(balanceKey);
        Blockchain.require(balance.compareTo(amount) >= 0); //Token holder
        // has sufficient balance to burn

        tokenTotalSupply = tokenTotalSupply.subtract(amount);
        AIP041Event.AIP041Burned(operator, tokenHolder, amount, holderData, operatorData);
        Blockchain.putStorage(balanceKey, balance.subtract(amount).toByteArray());
    }

    private static BigInteger getBalance(byte[] accountKey) {
        byte[] balance = Blockchain.getStorage(accountKey);
        return balance == null ? BigInteger.ZERO : new BigInteger(balance);
    }
}




