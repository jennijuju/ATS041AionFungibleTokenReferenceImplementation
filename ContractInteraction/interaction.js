const BN = require('bn.js');
const Web3 = require("aion-web3");
const web3 = new Web3(new Web3.providers.HttpProvider(""));
const privateKey = "";
const account = web3.eth.accounts.privateKeyToAccount(privateKey);
console.log(account.address);
async function send() {

    //contract
    var namp = new BN("10000000000000000");
    var amount = new BN("333");
    let amountInNamp = namp.mul(amount);
    let to = "0xa01a69a6b76369dcb7509e8ba575d05005c3ebba5d664122764648e2193b6464";
    var userdata = new Array(32).fill(0);

    let data = web3.avm.contract.method('send').inputs(['address','byte[]','byte[]'],[to,amountInNamp.toArray('be',32), userdata]).encode();
    //console.log(data);
    //construct a transaction
    const Tx = {
        from: account.address,
        to: "0xa09e62545e25f5263f21856010c4792be97dcfd17f1f3500f7cbe3382017bf1c",
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