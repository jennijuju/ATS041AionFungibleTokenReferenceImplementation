const BN = require('bn.js');
const Web3 = require("aion-web3");
const web3 = new Web3(new Web3.providers.HttpProvider(""));


async function getTokenInfo() {

    let data = web3.avm.contract.method("getTokenName").encode();
    let transactionObject = {
        to: "0xA0dF59C5C94f481AFb08073ee70d0139cf2d8Fc3C72cdAbAaD2B6323f55F1AeC",
        data: data,
    };
    let initialResponse = await web3.eth.call(transactionObject);
    let tokenName = await web3.avm.contract.decode("string", initialResponse);
    console.log(tokenName);


    data = web3.avm.contract.method("getTokenSymbol").encode();
    transactionObject = {
        to: "0xA0dF59C5C94f481AFb08073ee70d0139cf2d8Fc3C72cdAbAaD2B6323f55F1AeC",
        data: data,
    };
    initialResponse = await web3.eth.call(transactionObject);
    let tokenSymbol = await web3.avm.contract.decode("string", initialResponse);
    console.log(tokenSymbol);



    data = web3.avm.contract.method("getTokenGranularity").encode();
    transactionObject = {
        to: "0xA0dF59C5C94f481AFb08073ee70d0139cf2d8Fc3C72cdAbAaD2B6323f55F1AeC",
        data: data,
    };
    initialResponse = await web3.eth.call(transactionObject);
    let tokenGranularity = await web3.avm.contract.decode("int", initialResponse);
    console.log(tokenGranularity);


    data = web3.avm.contract.method("getTokenTotalSupply").encode();
    transactionObject = {
        to: "0xA0dF59C5C94f481AFb08073ee70d0139cf2d8Fc3C72cdAbAaD2B6323f55F1AeC",
        data: data,
    };
    initialResponse = await web3.eth.call(transactionObject);
    let tokenTotalSupply = await web3.avm.contract.decode("string", initialResponse);
    console.log(tokenTotalSupply);
}

getTokenInfo();

/* Output
JENNIJUJU
J3N
1
333333333000000000000000000
*/