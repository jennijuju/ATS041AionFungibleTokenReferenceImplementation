const BN = require('bn.js');
const Web3 = require("aion-web3");
const web3 = new Web3(new Web3.providers.HttpProvider(""));
const privateKey = "";
const account = web3.eth.accounts.privateKeyToAccount(privateKey);
console.log(account.address);
async function authorize() {

    //contract
   
    let operator = "";
    let ctAddress = " "
    var userdata = new Array(32).fill(0);

    let data = web3.avm.contract.method('authorizeOperator').inputs(['address'],[operator]).encode();
    //console.log(data);
    //construct a transaction
    const Tx = {
        from: account.address,
        to: ctAddress,
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
        to: ctAddress,
        data: data,
    };
    initialResponse = await web3.eth.call(transactionObject);
    let res = await web3.avm.contract.decode("boolean", initialResponse);
    console.log("Is operator: " + res); 

}
authorize();
