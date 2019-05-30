const BN = require('bn.js');
const Web3 = require("aion-web3");
const web3 = new Web3(new Web3.providers.HttpProvider("http://localhost:8545"));
const privateKey = "0xb5d819f4f63f40bb52e09e35c0f09267ccbb8b250f2f09cf62013c00b9b01480ea3b1f6b8612427dd23d68bd69ef89534bb182d924f6fc5e7483979d038c6883";
const account = web3.eth.accounts.privateKeyToAccount(privateKey);
console.log(account.address);
async function send() {

    //contract
    var namp = new BN("10000000000000000");
    var amount = new BN("333");
    let amountInNamp = namp.mul(amount);
    let to = "0xa0ad1839a3d6fde58782eb35eeb7f6f8db997733adb4d141534a20aae1951af4";
    var userdata = new Array(32).fill(0);

    let data = web3.avm.contract.method('send').inputs(['address','byte[]','byte[]'],[to,amountInNamp.toArray('be',32),userdata]).encode();
    //console.log(data);
    //construct a transaction
    const Tx = {
        from: account.address,
        to: "0xA062f462032956a99E1Cc3e5314B4e0F004215080B1A174030C62c08b7999a90",
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
//0xa0e50b1f3819ff12720312e4e77ddb8a4b03f9271ce53f5be47b717d10a030bd