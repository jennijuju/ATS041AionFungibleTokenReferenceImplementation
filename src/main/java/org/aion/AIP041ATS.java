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


    /**************************************Deployment Initialization***************************************/

    /**
     * tokenName: Name of the token.
     */
    @Initializable
    private static String tokenName ;

    /**
     * tokenSymbol: Symbol of the token.
     */
    @Initializable
    private static String tokenSymbol;

    /**
     * tokenGranularity: Granularity of the token.
     * The granularity is the smallest number of tokens (in the basic unit, which is 10^-18) which MAY be minted, sent and burned in any transaction.
     */
    @Initializable
    private static int tokenGranularity;

    /**
     * totalSupply: Total number of the minted token upon token creation.
     */
    @Initializable
    private static BigInteger tokenTotalSupply;

    /**
     * Initialization upon deployment.
     */
    static {
        Blockchain.require(tokenName.length() > 0);
        Blockchain.require(tokenSymbol.length() > 0);
        Blockchain.require(tokenGranularity >= 1);
        Blockchain.require(tokenTotalSupply.compareTo(BigInteger.ZERO) == 1);

        initialize();
    }


    /********************************************Initialization********************************************/
    /**
     * Total number of the minted token upon token creation is initialized under the token creator's account.
     * "ATSTokenCreated" event is emitted upon deployment.
     */
    private static void initialize() {
        Blockchain.putStorage(AIP041KeyValueStorage.getBalanceKey(Blockchain.getCaller()), tokenTotalSupply.toByteArray());
        AIP041Event.ATSTokenCreated(tokenTotalSupply, Blockchain.getCaller());
    }


    /**********************************************Token Info**********************************************/

    /**
     * Get the name of the token.
     * @return The name of the token.
     */
    @Callable
    public static String name() {
        return tokenName;
    }

    /**
     * Get the symbol of the token.
     * @return The symbol of the token.
     */
    @Callable
    public static String symbol() {
        return tokenSymbol;
    }

    /**
     * Get the granularity of the token.
     * @return The granularity of the token.
     */
    @Callable
    public static int granularity() {
        return tokenGranularity;
    }

    /**
     * Get the total supply of the token.
     * @return The total supply of the token.
     */
    @Callable
    public static BigInteger totalSupply() {
        return tokenTotalSupply;
    }

    /*********************************************Token Holder*********************************************/

    /**
     * Get the balance of an account.
     * @param tokenHolder An account that has ownership over a token balance.
     * @return The token balance of the account.
     */
    @Callable
    public static BigInteger balanceOf(Address tokenHolder) {
        byte[] balance = Blockchain.getStorage(AIP041KeyValueStorage.getBalanceKey(tokenHolder));
        return (balance != null)
                ? new BigInteger(balance)
                : BigInteger.ZERO;
    }

    /**
     * Set a third party operator address as an operator of Blockchain.getCaller() to send and burn tokens on its behalf.
     * @param operator The address that Blockchain.getCaller() wants to authorize to send and burn tokens on its behalf in the future.
     */
    @Callable
    public static void authorizeOperator(Address operator) {
        AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller());
        Blockchain.getStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller()));
        if ((Blockchain.getStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller())) == null) ||
             Blockchain.getCaller().equals(operator)) {
            Blockchain.putStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller()), new byte[] {0x01});
            AIP041Event.AuthorizedOperator(operator, Blockchain.getCaller());
        } else {//A token holder MAY authorize an already authorized operator. An AuthorizedOperator MUST be emitted each time.
            AIP041Event.AuthorizedOperator(operator, Blockchain.getCaller());
        }
    }

    @Callable
    public static void revokeOperator(Address operator) {
        Blockchain.require(!operator.equals(Blockchain.getCaller()));
        if (Blockchain.getStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller())) != null) {
            Blockchain.putStorage(AIP041KeyValueStorage.getIsOperatorKey(operator,Blockchain.getCaller()), null);
            AIP041Event.RevokedOperator(operator, Blockchain.getCaller());
        }

    }

    @Callable
    public static boolean isOperatorFor(Address operator, Address tokenHolder) {
        if (operator.equals(tokenHolder)) {
            return true;
        }

        return (Arrays.equals(Blockchain.getStorage(AIP041KeyValueStorage.getIsOperatorKey(operator, tokenHolder)), new byte[] {0x01}))
                ? true
                : false;

    }

    /******************************************Token Movement*******************************************/
    @Callable
    public static void send(Address to, BigInteger amount, byte[] userData) {
        doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, amount, userData, new byte[0], true);
    }

    @Callable
    public static void operatorSend(Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {
        if (from.equals(new Address(new byte[32]))) {
            doSend(Blockchain.getCaller(), Blockchain.getCaller(), to, amount, userData, operatorData, true);
        } else {
            Blockchain.require(isOperatorFor(Blockchain.getCaller(),from));
            doSend(Blockchain.getCaller(), from, to, amount, userData, operatorData, true);
        }
    }

    @Callable
    public static void burn(BigInteger amount, byte[] holderData) {
        doBurn(Blockchain.getCaller(),Blockchain.getCaller(), amount ,holderData, new byte[0]);
    }


    @Callable
    public static void operatorBurn(Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
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




