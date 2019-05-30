const BN = require('bn.js');
const Web3 = require("aion-web3");
let path = require('path');
const web3 = new Web3(new Web3.providers.HttpProvider(""));
const privateKey = "";
const account = web3.eth.accounts.privateKeyToAccount(privateKey);
console.log(account.address);
async function deploy() {

    //contract
    let jarPath = path.join(__dirname,'jar','ATSToken-1.0-SNAPSHOT.jar');
    var namp = new BN("1000000000000000000");
    var supply = new BN("333333333");
    let totalSupply = namp.mul(supply);
    let data = web3.avm.contract.deploy(jarPath).args(['string','string','int', 'byte[]'],['Jennijuju', 'J3N', 1, totalSupply.toArray("be",32)]).init();
    console.log(data);
    //construct a transaction
    const Tx = {
        from: account.address,
        data: data,
        gasPrice: 10000000000,
        gas: 5000000,
        type: '0x2' //AVM java contract deployment
    };
    
    const signedTx = await web3.eth.accounts.signTransaction(
        Tx, account.privateKey
    ).then((res) => signedCall = res);


    //console.log(signedTx);
    //console.log(nonce);
    const receipt = await web3.eth.sendSignedTransaction(
        signedTx.rawTransaction
    ).on('receipt', receipt => {
        console.log("Receipt received!\ntxHash =", receipt.transactionHash)
    });

    console.log(receipt.logs[0]);
    console.log("Contract Address: " + receipt.contractAddress);

}

deploy();