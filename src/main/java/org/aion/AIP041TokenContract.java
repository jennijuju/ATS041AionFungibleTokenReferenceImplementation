package org.aion;


import avm.Address;
import avm.Blockchain;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.tooling.abi.Initializable;

import java.math.BigInteger;

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
public class AIP041TokenContract {

    /**************************************Deployment Initialization***************************************/

    /**
     * tokenName: Name of the token.
     */
    @Initializable
    private static String AIP041TokenName ;

    /**
     * tokenSymbol: Symbol of the token.
     */
    @Initializable
    private static String AIP041TokenSymbol;

    /**
     * tokenGranularity: Granularity of the token.
     * The granularity is the smallest number of tokens (in the basic unit, which is 10^-18) which MAY be minted, sent and burned in any transaction.
     */
    @Initializable
    private static int AIP041TokenGranularity;

    /**
     * totalSupply: Total number of the minted token upon token creation.
     * Since the decimals of the token is always 18. The initial total supply should be set as in (the desired supply * 10^18) to assure the precision.
     * For example, if your desired initial total supply is 1,234 tokens, then it should be set to 1, 234 * 10^18 upon creation.
     */
    @Initializable
    private static BigInteger AIP041TokenTotalSupply;

    /**
     * Initialization upon deployment.
     */
    static {
        Blockchain.require(AIP041TokenName.length() > 0);
        Blockchain.require(AIP041TokenSymbol.length() > 0);
        Blockchain.require(AIP041TokenGranularity >= 1);
        Blockchain.require(AIP041TokenTotalSupply.compareTo(BigInteger.ZERO) > 0);
        AIP041Implementation.tokenName = AIP041TokenName;
        AIP041Implementation.tokenSymbol = AIP041TokenSymbol;
        AIP041Implementation.tokenGranularity = AIP041TokenGranularity;
        AIP041Implementation.tokenTotalSupply = AIP041TokenTotalSupply;
        AIP041Implementation.create();
    }

    /**********************************************Token Info**********************************************/
    @Callable
    public static String AIP041Name() {
        return AIP041Implementation.AIP041Name();
    }

    @Callable
    public static String AIP041Symbol() {
        return AIP041Implementation.AIP041Symbol();
    }

    @Callable
    public static int AIP041Granularity() {
        return AIP041Implementation.AIP041Granularity();
    }

    @Callable
    public static BigInteger AIP041TotalSupply() {
        return AIP041Implementation.AIP041TotalSupply();
    }

    @Callable
    public static BigInteger AIP041BalanceOf(Address tokenHolder) {
        return  AIP041Implementation.AIP041BalanceOf(tokenHolder);
    }

    @Callable
    public static void AIP041AuthorizeOperator(Address operator) {
        AIP041Implementation.AIP041AuthorizeOperator(operator);
    }

    @Callable
    public static void AIP041RevokeOperator(Address operator) {
        AIP041Implementation.AIP041RevokeOperator(operator);
    }

    @Callable
    public static boolean AIP041IsOperatorFor(Address operator, Address tokenHolder) {
        return AIP041Implementation.AIP041IsOperatorFor(operator, tokenHolder);
    }

    @Callable
    public static void AIP041Send(Address to, BigInteger amount, byte[] userData) {
        AIP041Implementation.AIP041Send(to, amount, userData);
    }

    @Callable
    public static void AIP041OperatorSend(Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData) {
        AIP041Implementation.AIP041OperatorSend(from, to, amount, userData, operatorData);
    }

    @Callable
    public static void AIP041Burn(BigInteger amount, byte[] holderData) {
        AIP041Implementation.AIP041Burn(amount, holderData);
    }

    @Callable
    public static void AIP041OperatorBurn(Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData) {
       AIP041Implementation.AIP041OperatorBurn(tokenHolder, amount, holderData, operatorData);
    }
}
