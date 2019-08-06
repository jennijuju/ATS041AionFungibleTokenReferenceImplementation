package org.aion;


import avm.Address;
import avm.Blockchain;
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
public class TokenContract {

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
        AIP041ATS.tokenName = tokenName;
        AIP041ATS.tokenSymbol = tokenSymbol;
        AIP041ATS.tokenGranularity = tokenGranularity;
        AIP041ATS.tokenTotalSupply = tokenTotalSupply;
        AIP041ATS.initialize();
    }

    /**********************************************Token Info**********************************************/
    @Callable
    public static String name() {
        return AIP041ATS.AIP041Name();
    }

    @Callable
    public static String symbol() {
        return AIP041ATS.AIP041Symbol();
    }

    @Callable
    public static int granularity() {
        return AIP041ATS.AIP041Granularity();
    }

    @Callable
    public static BigInteger totalSupply() {
        return AIP041ATS.AIP041TotalSupply();
    }

    @Callable
    public static BigInteger balanceOf(Address tokenHolder) {
        return  AIP041ATS.AIP041BalanceOf(tokenHolder);
    }

    @Callable
    public static void authorizeOperator(Address operator) {
        AIP041ATS.AIP041AuthorizeOperator(operator);
    }

    @Callable
    public static void revokeOperator(Address operator) {
        AIP041ATS.AIP041RevokeOperator(operator);
    }

    @Callable
    public static boolean isOperatorFor(Address operator, Address tokenHolder) {
        return AIP041ATS.AIP041IsOperatorFor(operator, tokenHolder);
    }

    @Callable
    public static void send(Address to, BigInteger amount, byte[] userData) {
        AIP041ATS.AIP041Send(to, amount, userData);
    }

    @Callable
    public static void operatorSend(Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {
        AIP041ATS.AIP041OperatorSend(from, to, amount, userData, operatorData);
    }

    @Callable
    public static void burn(BigInteger amount, byte[] holderData) {
        AIP041ATS.AIP041Burn(amount, holderData);
    }

    @Callable
    public static void operatorBurn(Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
       AIP041ATS.AIP041OperatorBurn(tokenHolder, amount, holderData, operatorData);
    }
}
