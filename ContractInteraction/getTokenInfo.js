const BN = require('bn.js');
const Web3 = require("aion-web3");
const web3 = new Web3(new Web3.providers.HttpProvider("https://aion.api.nodesmith.io/v1/mastery/jsonrpc?apiKey=ec13c1ff5f65488fa6432f5f79e595f6"));

function toHexString(byteArray) {
  return Array.from(byteArray, function(byte) {
    return ('0' + (byte & 0xFF).toString(16)).slice(-2);
  }).join('')
}
async function getTokenInfo() {
    let ctAddress = "0xA01d351d3D0971EA31C19301A835866998B8CE1f9E40153bd0BA5421519B8b02";
    let data = web3.avm.contract.method("name").encode();
    let transactionObject = {
        to: ctAddress,
        data: data,
    };
    let initialResponse = await web3.eth.call(transactionObject);
    let tokenName = await web3.avm.contract.decode("string", initialResponse);
    console.log(tokenName);


    data = web3.avm.contract.method("symbol").encode();
    transactionObject = {
        to: ctAddress,
        data: data,
    };
    initialResponse = await web3.eth.call(transactionObject);
    let tokenSymbol = await web3.avm.contract.decode("string", initialResponse);
    console.log(tokenSymbol);



    data = web3.avm.contract.method("granularity").encode();
    transactionObject = {
        to: ctAddress,
        data: data,
    };
    initialResponse = await web3.eth.call(transactionObject);
    let tokenGranularity = await web3.avm.contract.decode("int", initialResponse);
    console.log(tokenGranularity);


    data = web3.avm.contract.method("totalSupply").encode();
    transactionObject = {
        to: ctAddress,
        data: data,
    };
    initialResponse = await web3.eth.call(transactionObject);
    let tokenTotalSupply = await web3.avm.contract.decode("byte[]", initialResponse);
    console.log(parseInt(toHexString(tokenTotalSupply),16));
}

getTokenInfo();

/* Output
JENNIJUJU
J3N
1
333333333000000000000000000
*/