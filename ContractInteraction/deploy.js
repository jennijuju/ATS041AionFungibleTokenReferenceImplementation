const BN = require('bn.js');
const Web3 = require("aion-web3");
let path = require('path');
const web3 = new Web3(new Web3.providers.HttpProvider("https://aion.api.nodesmith.io/v1/mastery/jsonrpc?apiKey=ec13c1ff5f65488fa6432f5f79e595f6"));
const privateKey = "0xA77abf828e0af71551a439174c31e96f19552247cd1e19b1d6322dd61b3df832e17865bbd3b0490940eea10e34a529881932422cf8a19fe8f85aa8ddfff020e7";
const account = web3.eth.accounts.privateKeyToAccount(privateKey);
console.log(account.address);
async function deploy() {

    //contract
    let jarPath = path.join(__dirname,'jar','ATSTokenContract-1.0-SNAPSHOT.jar');
    var namp = new BN("1000000000000000000");
    var supply = new BN("333333333");
    let totalSupply = namp.mul(supply);
    let data = web3.avm.contract.deploy(jarPath).args(['string','string','int', 'byte[]'],['JENNIJUJU', 'J3N', 1, totalSupply.toArray("be",32)]).init();
    console.log(data);
    //construct a transaction
    const Tx = {
        //from: account.address,
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
