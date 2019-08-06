package org.aion;


import avm.Address;
import avm.Blockchain;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.tooling.abi.Initializable;

import java.math.BigInteger;

/**
 * Reference contract implementation for ATS-041 Aion fungible Token(https://github.com/aionnetwork/ATS/issues/41).
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
public class ATS041TokenContract {

    /**************************************Deployment Initialization***************************************/

    /**
     * tokenName: Name of the token.
     */
    @Initializable
    private static String ATS041TokenName ;

    /**
     * tokenSymbol: Symbol of the token.
     */
    @Initializable
    private static String ATS041TokenSymbol;

    /**
     * tokenGranularity: Granularity of the token.
     * The granularity is the smallest number of tokens (in the basic unit, which is 10^-18) which MAY be minted, sent and burned in any transaction.
     */
    @Initializable
    private static int ATS041TokenGranularity;

    /**
     * totalSupply: Total number of the minted token upon token creation.
     */
    @Initializable
    private static BigInteger ATS041TokenTotalSupply;

    /**
     * Initialization upon deployment.
     */
    static {
        Blockchain.require(ATS041TokenName.length() > 0);
        Blockchain.require(ATS041TokenSymbol.length() > 0);
        Blockchain.require(ATS041TokenGranularity >= 1);
        Blockchain.require(ATS041TokenTotalSupply.compareTo(BigInteger.ZERO) == 1);
        ATS041Implementation.tokenName = ATS041TokenName;
        ATS041Implementation.tokenSymbol = ATS041TokenSymbol;
        ATS041Implementation.tokenGranularity = ATS041TokenGranularity;
        ATS041Implementation.tokenTotalSupply = ATS041TokenTotalSupply;
        ATS041Implementation.initialize();
    }

    /**********************************************Token Info**********************************************/
    @Callable
    public static String ATS041Name() {
        return ATS041Implementation.ATS041Name();
    }

    @Callable
    public static String ATS041Symbol() {
        return ATS041Implementation.ATS041Symbol();
    }

    @Callable
    public static int ATS041Granularity() {
        return ATS041Implementation.ATS041Granularity();
    }

    @Callable
    public static BigInteger ATS041TotalSupply() {
        return ATS041Implementation.ATS041TotalSupply();
    }

    @Callable
    public static BigInteger ATS041BalanceOf(Address tokenHolder) {
        return  ATS041Implementation.ATS041BalanceOf(tokenHolder);
    }

    @Callable
    public static void ATS041AuthorizeOperator(Address operator) {
        ATS041Implementation.ATS041AuthorizeOperator(operator);
    }

    @Callable
    public static void ATS041RevokeOperator(Address operator) {
        ATS041Implementation.ATS041RevokeOperator(operator);
    }

    @Callable
    public static boolean ATS041IsOperatorFor(Address operator, Address tokenHolder) {
        return ATS041Implementation.ATS041IsOperatorFor(operator, tokenHolder);
    }

    @Callable
    public static void ATS041Send(Address to, BigInteger amount, byte[] userData) {
        ATS041Implementation.ATS041Send(to, amount, userData);
    }

    @Callable
    public static void ATS041OperatorSend(Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {
        ATS041Implementation.ATS041OperatorSend(from, to, amount, userData, operatorData);
    }

    @Callable
    public static void ATS041Burn(BigInteger amount, byte[] holderData) {
        ATS041Implementation.ATS041Burn(amount, holderData);
    }

    @Callable
    public static void ATS041OperatorBurn(Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
       ATS041Implementation.ATS041OperatorBurn(tokenHolder, amount, holderData, operatorData);
    }
}
