package org.aion;

import avm.Blockchain;
import avm.Address;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.tooling.abi.Initializable;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Reference contract implementation for AIP-041 Aion fungible Token(https://github.com/aionnetwork/AIP/issues/41).
 *
 * This code is pending audit.
 * Do not use or deploy this code before reviewing it personally first.
 *
 * @author:
 * Jiaying Wang(https://github.com/jennijuju)
 *
 * @contributors:
 * Yao Sun(https://github.com/qoire);
 * Jeff Disher(https://github.com/jeff-aion);
 * Aayush Rajasekaran(https://github.com/arajasek)
 *
 *            _____ ____  _   _   ______                 _ _     _        _______    _
 *     /\   |_   _/ __ \| \ | |  |  ____|               (_) |   | |    |__   __|  | |
 *    /  \    | || |  | |  \| |  | |__ _   _ _ __   __ _ _| |__ | | ___   | | ___ | | _____ _ __
 *   / /\ \   | || |  | | . ` |  |  __| | | | '_ \ / _` | | '_ \| |/ _ \  | |/ _ \| |/ / _ \ '_ \
 *  / ____ \ _| || |__| | |\  |  | |  | |_| | | | | (_| | | |_) | |  __/  | | (_) |   <  __/ | | |
 * /_/    \_\_____\____/|_| \_|  |_|   \__,_|_| |_|\__, |_|_.__/|_|\___|  |_|\___/|_|\_\___|_| |_|
 *                                                  __/ |
 *                                                 |___/
 *
 */
public class AIP041ATS {



    protected static String tokenName ;
    protected static String tokenSymbol;
    protected static int tokenGranularity;
    protected static BigInteger tokenTotalSupply;

    /********************************************Initialization********************************************/
    /**
     * Total number of the minted token upon token creation is initialized under the token creator's account.
     * "ATSTokenCreated" event is emitted upon deployment.
     */
    protected static void initialize() {
        Blockchain.putStorage(AIP041KeyValueStorage.getBalanceKey(Blockchain.getCaller()), tokenTotalSupply.toByteArray());
        AIP041Event.ATSTokenCreated(tokenTotalSupply, Blockchain.getCaller());
    }


    /**********************************************Token Info**********************************************/

    /**
     * Get the name of the token.
     * @return The name of the token.
     */
    protected static String name() {
        return tokenName;
    }

    /**
     * Get the symbol of the token.
     * @return The symbol of the token.
     */
    protected static String symbol() {
        return tokenSymbol;
    }

    /**
     * Get the granularity of the token.
     * @return The granularity of the token.
     */
    protected static int granularity() {
        return tokenGranularity;
    }

    /**
     * Get the total supply of the token.
     * @return The total supply of the token.
     */
    protected static BigInteger totalSupply() {
        return tokenTotalSupply;
    }

    /*********************************************Token Holder*********************************************/

    /**
     * Get the balance of an account.
     * @param tokenHolder An account that has ownership over a token balance.
     * @return The token balance of the account.
     */
    protected static BigInteger balanceOf(Address tokenHolder) {
        byte[] balance = Blockchain.getStorage(AIP041KeyValueStorage.getBalanceKey(tokenHolder));
        return (balance != null)
                ? new BigInteger(balance)
                : BigInteger.ZERO;
    }

    /**
     * Set a third party operator address as an operator of Blockchain.getCaller() to send and burn tokens on its behalf.
     * A token holder MAY authorize an already authorized operator.
     * An AuthorizedOperator MUST be emitted each time.
     * @param operator  Address to set as an operator for Blockchain.getCaller()
     *
     */

    protected static void authorizeOperator(Address operator) {
        Blockchain.putStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller()), new byte[] {0x01});
        AIP041Event.AuthorizedOperator(operator, Blockchain.getCaller());
    }

    /**
     * Remove the right of the operator address to be an operator for Blockchain.getCaller() to send and burn tokens on its behalf.
     * The token holder (Blockchain.getCaller()) is always an operator for itself. This right MUST NOT be revoked. This function MUST revert if it is called to revoke the token holder.
     * A RevokeOperator MUST be emitted each time.
     * @param operator  Address to rescind as an operator for Blockchain.getCaller().
     */
    protected static void revokeOperator(Address operator) {
        Blockchain.require(!operator.equals(Blockchain.getCaller()));
        if (Blockchain.getStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller())) != null) {
            Blockchain.putStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller()), null);
            AIP041Event.RevokedOperator(operator, Blockchain.getCaller());
        }

    }

    /**
     * Indicate whether the operator address is an operator of the tokenHolder address.
     * A token holder is always an operator of itself.
     * @param operator Address which may be an operator of tokenHolder.
     * @param tokenHolder Address of a token holder which may have the operator address as an operator.
     * @return true if operator is an operator of tokenHolder and false otherwise.
     */
    protected static boolean isOperatorFor(Address operator, Address tokenHolder) {
        if (operator.equals(tokenHolder)) {
            return true;
        }

        return (Arrays.equals(Blockchain.getStorage(AIP041KeyValueStorage.getIsOperatorKey(operator, tokenHolder)), new byte[] {0x01}))
                ? true
                : false;

    }

    /******************************************Token Movement*******************************************/

    /**
     * Send the amount of tokens from the address Blockchain.getCaller() to the address to.
     * The operator and the token holder MUST both be the Blockchain.getCaller().
     * @param to Token recipient.
     * @param amount Number of tokens to send.
     * @param userData Information attached to the send, and intended for the recipient (to).
     */
    protected static void send(Address to, BigInteger amount, byte[] userData) {
        doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, amount, userData, new byte[0], true);
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
    protected static void operatorSend(Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {
        if (from.equals(new Address(new byte[32]))) {
            doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, amount, userData, operatorData, true);
        } else {
            Blockchain.require(isOperatorFor(Blockchain.getCaller(),from));
            doSend(Blockchain.getCaller(), from, to, amount, userData, operatorData, true);
        }
    }

    /**
     * Burn the amount of tokens from the address Blockchain.getCaller().
     * The operator and the token holder MUST both be the Blockchain.getCaller().
     * @param amount Number of tokens to burn.
     * @param holderData Information attached to the burn by the token holder.
     */
    protected static void burn(BigInteger amount, byte[] holderData) {
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
    protected static void operatorBurn(Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
        if (tokenHolder.equals(new Address(new byte[32]))) {
            doBurn(Blockchain.getCaller(), Blockchain.getCaller(), amount, holderData,operatorData);
        } else {
            Blockchain.require(isOperatorFor(Blockchain.getCaller(), tokenHolder));
            doBurn(Blockchain.getCaller(), tokenHolder, amount, holderData, operatorData);
        }
    }


    private static void doSend(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {
        Blockchain.require(amount.compareTo(BigInteger.ZERO) > -1); //Amount is not negative value
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO)); //Check granularity
        Blockchain.require(!to.equals(new Address(new byte[32]))); //Forbid sending to 0x0 (=burning)
        Blockchain.require(!to.equals(Blockchain.getAddress())); //Forbid sending to this contract

        callSender(operator, from, to, amount, userData, operatorData);

        byte[] fromBalance = Blockchain.getStorage(AIP041KeyValueStorage.getBalanceKey(from));
        Blockchain.require(fromBalance != null); //Revert transaction if sender does not have a balance at all quickly to save energy
        Blockchain.require(new BigInteger(fromBalance).compareTo(amount) > -1); // Sender has sufficient balance
        Blockchain.putStorage(AIP041KeyValueStorage.getBalanceKey(from), new BigInteger(fromBalance).subtract(amount).toByteArray());


        byte[] toBalance = Blockchain.getStorage(AIP041KeyValueStorage.getBalanceKey(to));
        if(toBalance != null) { //Receiver has a balanace
            Blockchain.putStorage(AIP041KeyValueStorage.getBalanceKey(to), new BigInteger(toBalance).add(amount).toByteArray());
            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            AIP041Event.Sent(operator, from, to, amount, userData, operatorData);

        } else { //Receiver is a new token holder
            Blockchain.putStorage(AIP041KeyValueStorage.getBalanceKey(to), amount.toByteArray());

            callRecipient(operator, from, to, amount, userData, operatorData, preventLocking);
            AIP041Event.Sent(operator, from, to, amount, userData, operatorData);
        }
    }

    private static void doBurn(Address operator, Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
        Blockchain.require(amount.compareTo(BigInteger.ZERO) > -1); //Amount is not a negative number
        Blockchain.require(amount.mod(BigInteger.valueOf(tokenGranularity)).equals(BigInteger.ZERO));

        byte[] balance =Blockchain.getStorage(AIP041KeyValueStorage.getBalanceKey(tokenHolder));
        Blockchain.require(balance != null); //Token holder has sufficient balance to burn
        Blockchain.require(new BigInteger(balance).compareTo(BigInteger.ZERO) > -1); //Token Holder has sufficient balance to burn
        Blockchain.putStorage(AIP041KeyValueStorage.getBalanceKey(tokenHolder), new BigInteger(balance).subtract(amount).toByteArray());

        tokenTotalSupply = tokenTotalSupply.subtract(amount);

        callSender(operator, tokenHolder, new Address(new byte[32]), amount, holderData, operatorData);
        AIP041Event.Burned(operator, tokenHolder, amount, holderData, operatorData);
    }

    private static void callSender(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {

    }

    private static void callRecipient(Address operator, Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData, boolean preventLocking) {

    }

    private static boolean isRegularAccount(Address address) {
        return (Blockchain.getCodeSize(address) > 0) ? true : false;
    }

}




