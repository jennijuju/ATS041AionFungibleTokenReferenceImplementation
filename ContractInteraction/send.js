const BN = require('bn.js');
const Web3 = require("aion-web3");
const web3 = new Web3(new Web3.providers.HttpProvider(""));
const privateKey = "";
const account = web3.eth.accounts.privateKeyToAccount(privateKey);
console.log(account.address);
async function send() {

    //contract
    var namp = new BN("1000000000000000000");
    var amount = new BN("333");
    let amountInNamp = namp.mul(amount);
    let to = "0xa01a69a6b76369dcb7509e8ba575d05005c3ebba5d664122764648e2193b6464";
    var userdata = new Array(32).fill(0);

    let data = web3.avm.contract.method('send').inputs(['address','byte[]','byte[]'],[to,amountInNamp.toArray('be',32),userdata]).encode();
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

}
send();
