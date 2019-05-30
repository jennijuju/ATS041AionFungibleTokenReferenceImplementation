package atsToken;

import avm.Address;
import avm.Blockchain;
import org.aion.avm.core.util.LogSizeUtils;
import org.aion.avm.tooling.AvmRule;

import org.aion.avm.userlib.AionBuffer;
import org.aion.avm.userlib.abi.ABIStreamingEncoder;
import org.aion.vm.api.interfaces.IExecutionLog;
import org.aion.vm.api.interfaces.ResultCode;
import org.junit.*;

import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ATSTokenContractTest {
    @Rule
    public AvmRule avmRule = new AvmRule(true);

    //default address with balance
    private Address deployer =  avmRule.getPreminedAccount();
    private Address contractAddress;

    private BigInteger nAmp = BigInteger.valueOf(1_000_000_000_000_000_000L);
    private String tokenName = "JENNIJUJU";
   // private String tokenNameNull = "";
    private String tokenSymbol = "J3N";
    //private int tokenGranularity = 1;
    private int tokenGranularity = 3;
    private byte[] tokenTotalSupply = BigInteger.valueOf(333_333_333_333_333_333L).multiply(nAmp).toByteArray();
    // byte[] tokenTotalSupply = BigInteger.ZERO.toByteArray();

    @Before
    public void deployDapp() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        byte[] data = encoder.encodeOneString(tokenName)
                                .encodeOneString(tokenSymbol)
                                .encodeOneInteger(tokenGranularity)
                                .encodeOneByteArray(tokenTotalSupply)
                                .toBytes();
        byte[] contractData = avmRule.getDappBytes(ATSTokenContract.class, data, ATSTokenContractEvents.class, TokenHolderInformation.class);
        contractAddress = avmRule.deploy(deployer, BigInteger.ZERO, contractData).getDappAddress();
    }

    @Test
    public void testInitialization() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,encoder.encodeOneString("name").toBytes());
        String resStr = (String) result.getDecodedReturnData();
        Assert.assertTrue(resStr.equals("JENNIJUJU"));

        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,encoder.encodeOneString("symbol").toBytes());
        resStr = (String) result.getDecodedReturnData();
        Assert.assertTrue(resStr.equals("J3N"));

        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,encoder.encodeOneString("granularity").toBytes());
        int resInt = (int) result.getDecodedReturnData();
        Assert.assertTrue(resInt == 3);

        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,encoder.encodeOneString("totalSupply").toBytes());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(new BigInteger(tokenTotalSupply)));

        result = avmRule.call(deployer,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(deployer).toBytes());
        resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(new BigInteger(tokenTotalSupply)));
    }

    @Test
    public void testGetBalanceOfNone() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(deployer,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(avmRule.getRandomAddress(BigInteger.ZERO)).toBytes());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(resBytes.length == 0);
    }


    //basics
    @Test
    public void testIsOperatorFor(){
        //operator == token holder
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(deployer)
                        .encodeOneAddress(deployer)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);


        //no token holder information
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(avmRule.getRandomAddress(BigInteger.ZERO))
                        .encodeOneAddress(avmRule.getRandomAddress(BigInteger.ZERO))
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(!res);

        //only has balance
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(avmRule.getRandomAddress(BigInteger.ZERO))
                        .encodeOneAddress(deployer)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(!res);

    }

    //operator == token holder
    @Test
    public void testAutorizeOperator1() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                                                    encoder.encodeOneString("authorizeOperator")
                                                            .encodeOneAddress(deployer)
                                                            .toBytes());
        ResultCode status = result.getReceiptStatus();
        Assert.assertTrue(status.isFailed());
    }


    //tokenHolderInformation only has balance
    @Test
    public void testAutorizeOperator2() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        AvmRule.ResultWrapper result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());

        //test isOperator
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(deployer)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);
    }

    //token holder has no info
    @Test
    public void testAutorizeOperator3() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());

        //test isOperator
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //balance should be added as 0
        result = avmRule.call(deployer,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(tokenHolder).toBytes());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.ZERO));
    }

    //add new operator that is not existed
    @Test
    public void testAutorizeOperator4() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        //add first operator
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());

        //test operator isOperator
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //add second operator
        Address operator2 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator2)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator2.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());

        //test operator isOperator
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator2)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //balance should be added as 0
        result = avmRule.call(deployer,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(tokenHolder).toBytes());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.ZERO));
    }

    //add new operator that already exists
    @Test
    public void testAutorizeOperator5() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        //add first operator
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());

        //test operator isOperator
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        System.out.println("Added!" + res);
        Assert.assertTrue(res);

        //add same operator
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(0, result.getLogs().size());


        //balance should be added as 0
        result = avmRule.call(deployer,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(tokenHolder).toBytes());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.ZERO));
    }



    // basics
    @Test
    public void testRevokeOperator1(){
        //token holder == operator
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(deployer)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //token holder has no info
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //token holder only has balance
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
    }


    //revoke the only operator
    @Test
    public void testRevokeOperator2() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        //add  operator
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());


        //revoke operator
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());


        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("RevokedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());

//
//        //test operator isOperator, should return false
//        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
//                encoder.encodeOneString("isOperatorFor")
//                        .encodeOneAddress(operator)
//                        .encodeOneAddress(tokenHolder)
//                        .toBytes());
//        Boolean res = (boolean) result.getDecodedReturnData();
//        Assert.assertTrue(!res);
//
//        //balance should be added as 0
//        result = avmRule.call(deployer,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(tokenHolder).toBytes());
//        String resStr = (String) result.getDecodedReturnData();
//        System.out.println("Balance " + resStr);
//        Assert.assertTrue(resStr.equals(BigInteger.ZERO.toString()));

//        //revoke again, should show numberOfOperator = 0
//        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
//                encoder.encodeOneString("revokeOperator")
//                        .encodeOneAddress(operator)
//                        .toBytes());
//        Assert.assertTrue(result.getReceiptStatus().isSuccess());
    }

    //revoke one of the operators
    @Test
    public void testRevokeOperator3() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        //add  operator
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator2
        Address operator2 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO, encoder.encodeOneString("authorizeOperator").encodeOneAddress(operator2).toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator3
        Address operator3 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator3)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator4
        Address operator4 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO, encoder.encodeOneString("authorizeOperator").encodeOneAddress(operator4).toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //revoke operator2
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(operator2)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());


        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("RevokedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator2.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());


        //test operator isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator3 isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator3)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator4 isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator4)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator2 isOperator, should return false
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator2)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(!res);

        //balance should be added as 0
        result = avmRule.call(deployer,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(tokenHolder).toBytes());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.ZERO));
    }

    //revoke the last operator
    @Test
    public void testRevokeOperator4() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        //add  operator
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator2
        Address operator2 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO, encoder.encodeOneString("authorizeOperator").encodeOneAddress(operator2).toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator3
        Address operator3 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator3)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator4
        Address operator4 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO, encoder.encodeOneString("authorizeOperator").encodeOneAddress(operator4).toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //revoke operator4
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(operator4)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());


        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("RevokedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator4.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());


        //test operator isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator3 isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator2)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator4 isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator3)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator2 isOperator, should return false
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator4)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(!res);

        //balance should be added as 0
        result = avmRule.call(deployer,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(tokenHolder).toBytes());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.ZERO));
    }

    //revoke the second last operator
    @Test
    public void testRevokeOperator5() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        //add  operator
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator2
        Address operator2 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO, encoder.encodeOneString("authorizeOperator").encodeOneAddress(operator2).toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator3
        Address operator3 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator3)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator4
        Address operator4 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO, encoder.encodeOneString("authorizeOperator").encodeOneAddress(operator4).toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //revoke operator3
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(operator3)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());


        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("RevokedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator3.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());


        //test operator isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator3 isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator2)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator4 isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator4)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator2 isOperator, should return false
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator3)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(!res);

        //balance should be added as 0
        result = avmRule.call(deployer,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(tokenHolder).toBytes());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.ZERO));
    }

    //revoke the first operator
    @Test
    public void testRevokeOperator6() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        //add  operator
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator2
        Address operator2 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO, encoder.encodeOneString("authorizeOperator").encodeOneAddress(operator2).toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator3
        Address operator3 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator3)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator4
        Address operator4 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO, encoder.encodeOneString("authorizeOperator").encodeOneAddress(operator4).toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //revoke operator
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());


        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("RevokedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());


        //test operator isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator2)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator3 isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator3)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator4 isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator4)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator2 isOperator, should return false
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(!res);

        //balance should be added as 0
        result = avmRule.call(deployer,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(tokenHolder).toBytes());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.ZERO));
    }


    //revoke an operator doesnt exist
    @Test
    public void testRevokeOperator7() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        //add  operator
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //add  operator2
        Address operator2 = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO, encoder.encodeOneString("authorizeOperator").encodeOneAddress(operator2).toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        //revoke random
        Address random = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(random).toBytes());

        Assert.assertTrue(result.getReceiptStatus().isSuccess());


        assertEquals(0, result.getLogs().size());

        //test operator isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        //test operator2 isOperator, should return true
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator2)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);
    }


    //sender has enough balance and receiver has no info yet
    @Test
    public void testSend1() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        Address to = avmRule.getRandomAddress(BigInteger.ZERO);
        AvmRule.ResultWrapper result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(to)
                        .encodeOneByteArray(BigInteger.valueOf(3).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(null)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("Sent".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(to.toByteArray()), log.getTopics().get(3));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(3).multiply(nAmp)).getArray(),
                log.getData());

        //check balance
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(deployer)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(new BigInteger(tokenTotalSupply).subtract(BigInteger.valueOf(3).multiply(nAmp))));

        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(to)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.valueOf(3).multiply(nAmp)));
    }

    //sender has enough balance and receiver has a balance but no operator
    @Test
    public void testSend2() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        Address to = avmRule.getRandomAddress(BigInteger.ZERO);
        AvmRule.ResultWrapper result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(to)
                        .encodeOneByteArray(BigInteger.valueOf(3).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("Sent".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(to.toByteArray()), log.getTopics().get(3));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(3).multiply(nAmp)).getArray(),
                log.getData());

        //check balance for first tx
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(deployer)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(new BigInteger(tokenTotalSupply).subtract(BigInteger.valueOf(3).multiply(nAmp))));

        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(to)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.valueOf(3).multiply(nAmp)));

        //second tx
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(to)
                        .encodeOneByteArray(BigInteger.valueOf(3).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("Sent".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(to.toByteArray()), log.getTopics().get(3));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(3).multiply(nAmp)).getArray(),
                log.getData());

        //check balance for second tx
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(deployer)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(new BigInteger(tokenTotalSupply).subtract(BigInteger.valueOf(3).multiply(nAmp).multiply(BigInteger.TWO))));

        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(to)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.valueOf(3).multiply(nAmp).multiply(BigInteger.TWO)));
    }

    //sender has enough balance and receiver has a balance and operators
    @Test
    public void testSend3() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        Address to = avmRule.getRandomAddress(BigInteger.valueOf(3).multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(to)
                        .encodeOneByteArray(BigInteger.valueOf(3).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("Sent".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(to.toByteArray()), log.getTopics().get(3));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(3).multiply(nAmp)).getArray(),
                log.getData());

        //check balance for first tx
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(deployer)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        byte[] resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(new BigInteger(tokenTotalSupply).subtract(BigInteger.valueOf(3).multiply(nAmp))));

        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(to)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.valueOf(3).multiply(nAmp)));

        //autorize operator
        Address operator = avmRule.getRandomAddress(BigInteger.ZERO);
        result = avmRule.call(to,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
         log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(to.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());

        //second tx
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(to)
                        .encodeOneByteArray(BigInteger.valueOf(3).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("Sent".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(to.toByteArray()), log.getTopics().get(3));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(3).multiply(nAmp)).getArray(),
                log.getData());

        //check balance for second tx
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(deployer)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(new BigInteger(tokenTotalSupply).subtract(BigInteger.valueOf(3).multiply(nAmp).multiply(BigInteger.TWO))));


        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(to)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.valueOf(3).multiply(nAmp).multiply(BigInteger.TWO)));


    }

    //from doesnt have enough balance
    @Test
    public void testSend4() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        Address to = avmRule.getRandomAddress(BigInteger.ZERO);
        AvmRule.ResultWrapper result = avmRule.call(avmRule.getRandomAddress(BigInteger.valueOf(3).multiply(nAmp)),
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(to)
                        .encodeOneByteArray(BigInteger.valueOf(3).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    //basics requirements
    @Test
    public void testSend5() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        //to=0x0000..
        AvmRule.ResultWrapper result = avmRule.call(avmRule.getRandomAddress(BigInteger.valueOf(3).multiply(nAmp)),
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(new Address(new byte[32]))
                        .encodeOneByteArray(BigInteger.valueOf(3).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());

        //to = contract itself
        encoder = new ABIStreamingEncoder();
        result = avmRule.call(avmRule.getRandomAddress(BigInteger.valueOf(3).multiply(nAmp)),
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(contractAddress)
                        .encodeOneByteArray(BigInteger.valueOf(3).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());

        //test granularity
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(avmRule.getRandomAddress(BigInteger.ZERO))
                        .encodeOneByteArray(BigInteger.valueOf(7).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    @Test
    public void testOperatorSend() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        Address to = avmRule.getRandomAddress(BigInteger.valueOf(3).multiply(nAmp));

        //authorize operator
        Address operator = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(new byte[0], log.getData());

         result = avmRule.call(operator,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("operatorSend")
                        .encodeOneAddress(deployer)
                        .encodeOneAddress(to)
                        .encodeOneByteArray(BigInteger.valueOf(3).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("Sent".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(to.toByteArray()), log.getTopics().get(3));
        //Todo: Why its 40 bytes instead of 32? - ask Jeff
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(3).multiply(nAmp)).getArray(),
                log.getData());

        //check balance for first tx
        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(deployer)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        byte[]  resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(new BigInteger(tokenTotalSupply).subtract(BigInteger.valueOf(3).multiply(nAmp))));

        result = avmRule.call(deployer,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(to)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        resBytes = (byte[]) result.getDecodedReturnData();
        Assert.assertTrue(new BigInteger(resBytes).equals(BigInteger.valueOf(3).multiply(nAmp)));

    }

    //is not operator
    @Test
    public void testOperatorSend2() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        AvmRule.ResultWrapper result = avmRule.call(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)),
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("operatorSend")
                        .encodeOneAddress(deployer)
                        .encodeOneAddress(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)))
                        .encodeOneByteArray(BigInteger.valueOf(3).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }


    //token holder doesnt have enough balance
    @Test
    public void testBurn1() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)),
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("burn")
                        .encodeOneByteArray(BigInteger.valueOf(3).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    @Test
    public void testBurn2() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(deployer, contractAddress, BigInteger.ZERO,
                 encoder.encodeOneString("burn")
                        .encodeOneByteArray(BigInteger.valueOf(333).multiply(nAmp).toByteArray())
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getLogs().size());
        IExecutionLog log = result.getLogs().get(0);

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("Burned".getBytes()), log.getTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(deployer.toByteArray()), log.getTopics().get(2));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(333).multiply(nAmp)).getArray(),
                log.getData());
    }



}
