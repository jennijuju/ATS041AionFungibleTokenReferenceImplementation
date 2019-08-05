package org.aion;

import avm.Address;
import org.aion.avm.core.util.LogSizeUtils;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.userlib.AionBuffer;
import org.aion.avm.userlib.abi.ABIStreamingEncoder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class ATSTokenContractTest {
    @Rule
    public AvmRule avmRule = new AvmRule(true);
    
    private Address tokenOwner =  avmRule.getPreminedAccount();//Account deploys the token contract.
    private Address contractAddress;

    private BigInteger nAmp = BigInteger.valueOf(1_000_000_000_000_000_000L);
    private String tokenName = "JENNIJUJU";
    private String tokenSymbol = "J3N";
    private int tokenGranularity = 3;
    private BigInteger tokenTotalSupply = BigInteger.valueOf(333_333_333_333_333_333L).multiply(nAmp);

    @Before
    public void deployDapp() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        byte[] data = encoder.encodeOneString(tokenName)
                                .encodeOneString(tokenSymbol)
                                .encodeOneInteger(tokenGranularity)
                                .encodeOneBigInteger(tokenTotalSupply)
                                .toBytes();
        byte[] contractData = avmRule.getDappBytes(AIP041Standard.class, data, 1, AIP041Event.class, AIP041KeyValueStorage.class);
        contractAddress = avmRule.deploy(tokenOwner, BigInteger.ZERO, contractData).getDappAddress();
    }


    @Test
    public void testInitialization() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,encoder.encodeOneString("name").toBytes());

        String resStr = (String) result.getDecodedReturnData();
        Assert.assertTrue(resStr.equals("JENNIJUJU"));

        result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,encoder.encodeOneString("symbol").toBytes());
        resStr = (String) result.getDecodedReturnData();
        Assert.assertTrue(resStr.equals("J3N"));

        result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,encoder.encodeOneString("granularity").toBytes());
        int resInt = (int) result.getDecodedReturnData();
        Assert.assertTrue(resInt == 3);

        result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,encoder.encodeOneString("totalSupply").toBytes());
        BigInteger resBytes = (BigInteger) result.getDecodedReturnData();
        Assert.assertTrue(resBytes.compareTo(tokenTotalSupply) == 0);
    }

    @Test
    public void testInitializeBalanceToDeployer(){
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress, BigInteger.ZERO, new ABIStreamingEncoder().encodeOneString("balanceOf").encodeOneAddress(tokenOwner).toBytes());
        BigInteger res = (BigInteger) result.getDecodedReturnData();
        Assert.assertTrue(res.compareTo(tokenTotalSupply) == 0);
    }


    @Test
    public void testGetBalanceOfaZeroBalanceAccount() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(avmRule.getRandomAddress(BigInteger.ZERO)).toBytes());
        BigInteger resBytes = (BigInteger) result.getDecodedReturnData();
        System.out.println(resBytes);
        Assert.assertTrue(resBytes.compareTo(BigInteger.ZERO) == 0 );
    }


    @Test
    public void testIsOperatorForTokenHolderItself(){
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                                                    encoder.encodeOneString("isOperatorFor")
                                                            .encodeOneAddress(tokenOwner)
                                                            .encodeOneAddress(tokenOwner)
                                                            .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

    }

    @Test
    public void testIsOperatorForWhenIsNotOperator(){
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(tokenOwner)
                        .encodeOneAddress(avmRule.getRandomAddress(BigInteger.ZERO))
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(!res);
    }

    @Test
    public void testAuthorizeOperatorWithaNonExistingOperator() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        Address operator = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                                                    encoder.encodeOneString("authorizeOperator")
                                                            .encodeOneAddress(operator)
                                                            .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        assertEquals(1, result.getTransactionResult().logs.size());
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(new byte[0],result.getTransactionResult().logs.get(0).copyOfData());

        result = avmRule.call(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)),contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

    }

    @Test
    public void testAuthorizeOperatorAnZeroAddress() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(new Address(new byte[32]))
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
    }

    @Test
    public void testAuthorizeOperatorWithAnExistingOperator() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        Address operator = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        assertEquals(1, result.getTransactionResult().logs.size());
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(new byte[0],result.getTransactionResult().logs.get(0).copyOfData());

        result = avmRule.call(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)),contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        assertEquals(1, result.getTransactionResult().logs.size());
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(new byte[0],result.getTransactionResult().logs.get(0).copyOfData());
    }

    @Test
    public void testAuthorizeOperatorTokenHolderItselfAfterAuthorizing() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        assertEquals(1, result.getTransactionResult().logs.size());
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(new byte[0],result.getTransactionResult().logs.get(0).copyOfData());

        result = avmRule.call(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)),contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(tokenHolder)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);
    }

    @Test
    public void testAuthorizeOperatorTokenHolderItselfWithoutAuthorizing() { //By default, a token holder is its own operator.
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));

        AvmRule.ResultWrapper result = avmRule.call(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)),contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(tokenHolder)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);
    }

    @Test
    public void testRevokeOperatorTokenHolderItself(){
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }


    @Test
    public void testRevokeOperatorThatWasAnOperator() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        Address operator = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        result = avmRule.call(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)),contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(res);

        result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        assertEquals(1, result.getTransactionResult().logs.size());
        assertArrayEquals(LogSizeUtils.truncatePadTopic("RevokedOperator".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenHolder.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(new byte[0],result.getTransactionResult().logs.get(0).copyOfData());


        result = avmRule.call(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)),contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(!res);
    }

    @Test
    public void testRevokeOperatorThatWasNotAnOperator() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        Address tokenHolder = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        Address operator = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenHolder,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("revokeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(0, result.getTransactionResult().logs.size());

        result = avmRule.call(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)),contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("isOperatorFor")
                        .encodeOneAddress(operator)
                        .encodeOneAddress(tokenHolder)
                        .toBytes());
        Boolean res = (boolean) result.getDecodedReturnData();
        Assert.assertTrue(!res);
    }

    @Test
    public void testSendaValidTransaction() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        Address to = avmRule.getRandomAddress(BigInteger.ZERO);
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(to)
                        .encodeOneBigInteger(BigInteger.valueOf(3).multiply(nAmp))
                        .encodeOneByteArray(null)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getTransactionResult().logs.size());

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("Sent".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(to.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(3));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(3).multiply(nAmp)).getArray(),
               result.getTransactionResult().logs.get(0).copyOfData());

        result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(tokenOwner)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        BigInteger res = (BigInteger) result.getDecodedReturnData();
        Assert.assertTrue(res.compareTo(tokenTotalSupply.subtract(BigInteger.valueOf(3).multiply(nAmp))) == 0);

        result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(to)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        res = (BigInteger) result.getDecodedReturnData();
        Assert.assertTrue(res.compareTo(BigInteger.valueOf(3).multiply(nAmp)) == 0);
    }

    @Test
    public void testSendanInvalidTransaction() { //Amount is greater than the balance.
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        Address to = avmRule.getRandomAddress(BigInteger.ZERO);
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(to)
                        .encodeOneBigInteger(BigInteger.valueOf(333_333_333_333_333_334L).multiply(nAmp))
                        .encodeOneByteArray(null)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    @Test
    public void testSendToNullAddress() { //Amount is greater than the balance.
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(null)
                        .encodeOneBigInteger(BigInteger.valueOf(333_333_333_333_333_334L).multiply(nAmp))
                        .encodeOneByteArray(null)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    @Test
    public void testSendToZeroAddress() { //Amount is greater than the balance.
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(new Address(new byte[32]))
                        .encodeOneBigInteger(BigInteger.valueOf(333_333_333_333_333_334L).multiply(nAmp))
                        .encodeOneByteArray(null)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    @Test
    public void testSendDisobeyGranularity() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        Address to = avmRule.getRandomAddress(BigInteger.ZERO);
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(to)
                        .encodeOneBigInteger(BigInteger.valueOf(333_333_333_333_333_332L).multiply(nAmp))
                        .encodeOneByteArray(null)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    @Test
    public void testSendaNegativeTransaction() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        Address to = avmRule.getRandomAddress(BigInteger.ZERO);
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(to)
                        .encodeOneBigInteger(BigInteger.valueOf(-3).multiply(nAmp))
                        .encodeOneByteArray(null)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }


    @Test
    public void testOperatorSend() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        Address to = avmRule.getRandomAddress(BigInteger.valueOf(3).multiply(nAmp));

        //authorize operator
        Address operator = avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp));
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("authorizeOperator")
                        .encodeOneAddress(operator)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getTransactionResult().logs.size());

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("AuthorizedOperator".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(new byte[0],result.getTransactionResult().logs.get(0).copyOfData());

        result = avmRule.call(operator,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("operatorSend")
                        .encodeOneAddress(tokenOwner)
                        .encodeOneAddress(to)
                        .encodeOneBigInteger(BigInteger.valueOf(3).multiply(nAmp))
                        .encodeOneByteArray(new byte[0])
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getTransactionResult().logs.size());

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("Sent".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(operator.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(to.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(3));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(3).multiply(nAmp)).getArray(),
               result.getTransactionResult().logs.get(0).copyOfData());

        result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(tokenOwner)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        BigInteger resBI = (BigInteger) result.getDecodedReturnData();
        Assert.assertTrue(resBI.compareTo(tokenTotalSupply.subtract(BigInteger.valueOf(3).multiply(nAmp))) == 0);

        result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(to)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        resBI = (BigInteger) result.getDecodedReturnData();
        Assert.assertTrue(resBI.compareTo(BigInteger.valueOf(3).multiply(nAmp)) == 0);
    }


    @Test
    public void testOperatorSendWithZeroTokenHolderAddress() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        Address to = avmRule.getRandomAddress(BigInteger.valueOf(3).multiply(nAmp));


        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("operatorSend")
                        .encodeOneAddress(new Address(new byte[32]))
                        .encodeOneAddress(to)
                        .encodeOneBigInteger(BigInteger.valueOf(3).multiply(nAmp))
                        .encodeOneByteArray(new byte[0])
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getTransactionResult().logs.size());

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("Sent".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(to.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(3));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(3).multiply(nAmp)).getArray(),
                result.getTransactionResult().logs.get(0).copyOfData());

        result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(tokenOwner)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        BigInteger resBI = (BigInteger) result.getDecodedReturnData();
        Assert.assertTrue(resBI.compareTo(tokenTotalSupply.subtract(BigInteger.valueOf(3).multiply(nAmp))) == 0);

        result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("balanceOf")
                        .encodeOneAddress(to)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        resBI = (BigInteger) result.getDecodedReturnData();
        Assert.assertTrue(resBI.compareTo(BigInteger.valueOf(3).multiply(nAmp)) == 0);
    }

    @Test
    public void testOperatorSendWhenIsNotOperator() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();

        Address to = avmRule.getRandomAddress(BigInteger.ZERO);
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,contractAddress,BigInteger.ZERO,
                encoder.encodeOneString("send")
                        .encodeOneAddress(to)
                        .encodeOneBigInteger(BigInteger.valueOf(3).multiply(nAmp))
                        .encodeOneByteArray(null)
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        assertEquals(1, result.getTransactionResult().logs.size());

        // validate the topics and data
        assertArrayEquals(LogSizeUtils.truncatePadTopic("Sent".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(to.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(3));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(3).multiply(nAmp)).getArray(),
                result.getTransactionResult().logs.get(0).copyOfData());


        result = avmRule.call(tokenOwner,
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("operatorSend")
                        .encodeOneAddress(to)
                        .encodeOneAddress(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)))
                        .encodeOneBigInteger(BigInteger.valueOf(3).multiply(nAmp))
                        .encodeOneByteArray(new byte[0])
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    @Test
    public void testBurnWithoutEnoughBalance() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)),
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("burn")
                        .encodeOneBigInteger(BigInteger.valueOf(3).multiply(nAmp))
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    @Test
    public void testBurnEnoughBalance() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("burn")
                        .encodeOneBigInteger(BigInteger.valueOf(3).multiply(nAmp))
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        assertEquals(1, result.getTransactionResult().logs.size());

        assertArrayEquals(LogSizeUtils.truncatePadTopic("Burned".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(3).multiply(nAmp)).getArray(),
                result.getTransactionResult().logs.get(0).copyOfData());

        result = avmRule.call(tokenOwner,contractAddress, BigInteger.ZERO, encoder.encodeOneString("balanceOf").encodeOneAddress(tokenOwner).toBytes());
        BigInteger res = (BigInteger) result.getDecodedReturnData();
        System.out.println(res);
        Assert.assertTrue(res.compareTo(tokenTotalSupply.subtract(BigInteger.valueOf(3).multiply(nAmp))) == 0 );

        result = avmRule.call(tokenOwner,contractAddress, BigInteger.ZERO, encoder.encodeOneString("totalSupply").toBytes());
        res = (BigInteger) result.getDecodedReturnData();
        System.out.println(res);
        Assert.assertTrue(res.compareTo(tokenTotalSupply.subtract(BigInteger.valueOf(3).multiply(nAmp))) == 0 );
    }


    @Test
    public void testBurnExceedEnoughBalance() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("burn")
                        .encodeOneBigInteger(BigInteger.valueOf(333_333_333_333_333_334L).multiply(nAmp))
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    @Test
    public void testBurnWrongGranularity() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("burn")
                        .encodeOneBigInteger(BigInteger.valueOf(333_333_333_333_333_332L).multiply(nAmp))
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    @Test
    public void testOperatorBurnIsNotOperator() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(avmRule.getRandomAddress(BigInteger.TEN.multiply(nAmp)),
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("operatorBurn")
                        .encodeOneAddress(tokenOwner)
                        .encodeOneBigInteger(BigInteger.valueOf(333_333_333_333L).multiply(nAmp))
                        .encodeOneByteArray(new byte[0])
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isFailed());
    }

    @Test
    public void testOperatorBurnWithZeroTokenHolderAddress() {
        ABIStreamingEncoder encoder = new ABIStreamingEncoder();
        AvmRule.ResultWrapper result = avmRule.call(tokenOwner,
                contractAddress,
                BigInteger.ZERO,
                encoder.encodeOneString("operatorBurn")
                        .encodeOneAddress(new Address(new byte[32]))
                        .encodeOneBigInteger(BigInteger.valueOf(333_333_333_333L).multiply(nAmp))
                        .encodeOneByteArray(new byte[0])
                        .encodeOneByteArray(new byte[0])
                        .toBytes());
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        assertEquals(1, result.getTransactionResult().logs.size());

        assertArrayEquals(LogSizeUtils.truncatePadTopic("Burned".getBytes()), result.getTransactionResult().logs.get(0).copyOfTopics().get(0));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(1));
        assertArrayEquals(LogSizeUtils.truncatePadTopic(tokenOwner.toByteArray()), result.getTransactionResult().logs.get(0).copyOfTopics().get(2));
        assertArrayEquals(AionBuffer.allocate(40).put32ByteInt(BigInteger.valueOf(333_333_333_333L).multiply(nAmp)).getArray(),
                result.getTransactionResult().logs.get(0).copyOfData());
    }

}