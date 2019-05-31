const BN = require('bn.js');
const Web3 = require("aion-web3");
const web3 = new Web3(new Web3.providers.HttpProvider(""));
const privateKey = "";
const account = web3.eth.accounts.privateKeyToAccount(privateKey);
console.log(account.address);
async function operatorSend() {

    //contract
    var namp = new BN("10000000000000000");
    var amount = new BN("333");
    let amountInNamp = namp.mul(amount);
    
    let from = "";
    let to = "";
    var userdata = new Array(32).fill(0);

    let data = web3.avm.contract.method('operatorSend').inputs(['address','address','byte[]','byte[]','byte[]'],[from,to,amountInNamp.toArray('be',32),userdata,userdata]).encode();
    const Tx = {
        from: account.address,
        to: "",
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

}

operatorSend();
