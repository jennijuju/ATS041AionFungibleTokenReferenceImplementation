const BN = require('bn.js');
const Web3 = require("aion-web3");
const web3 = new Web3(new Web3.providers.HttpProvider("https://aion.api.nodesmith.io/v1/mastery/jsonrpc?apiKey=ec13c1ff5f65488fa6432f5f79e595f6"));
const privateKey = "0xA77abf828e0af71551a439174c31e96f19552247cd1e19b1d6322dd61b3df832e17865bbd3b0490940eea10e34a529881932422cf8a19fe8f85aa8ddfff020e7";
const account = web3.eth.accounts.privateKeyToAccount(privateKey);
console.log(account.address);
async function send() {
 //contract
   
    let operator = "0xa0ff9f75a32bdb897d68ca429da91f97c96fa4d84844bdb761e1f04ffdbfc0b5";
    var userdata = new Array(32).fill(0);

    let data = web3.avm.contract.method('revokeOperator').inputs(['address'],[operator]).encode();
    //console.log(data);
    //construct a transaction
    const Tx = {
        from: account.address,
        to: "0xA0dF59C5C94f481AFb08073ee70d0139cf2d8Fc3C72cdAbAaD2B6323f55F1AeC",
        data: data,
        gasPrice: 10000000000,
        gas: 2000000
    };
    
    const signedTx = await web3.eth.accounts.signTransaction(
        Tx, account.privateKey
    ).then((res) => signedCall = res);

    const receipt = await web3.eth.sendSignedTransaction(
        signedTx.rawTransaction
    ).on('receipt', receipt => {
        console.log("Receipt received!\ntxHash =", receipt.transactionHash)
    });



    data = web3.avm.contract.method("isOperatorFor").inputs(['address','address'],[operator,account.address]).encode();
    transactionObject = {
        to: "0xA0dF59C5C94f481AFb08073ee70d0139cf2d8Fc3C72cdAbAaD2B6323f55F1AeC",
        data: data,
    };
    initialResponse = await web3.eth.call(transactionObject);
    let res = await web3.avm.contract.decode("boolean", initialResponse);
    console.log("Is operator: " + res); 
}
send();
