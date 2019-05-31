const BN = require('bn.js');
const Web3 = require("aion-web3");
const web3 = new Web3(new Web3.providers.HttpProvider(""));


function toHexString(byteArray) {
  return Array.from(byteArray, function(byte) {
    return ('0' + (byte & 0xFF).toString(16)).slice(-2);
  }).join('')
}

async function getBalance() {


	let tokenCreator = "0xa048630fff033d214b36879e62231cc77d81f45d348f6590d268b9b8cabb88a9";
	let tokenReceiver = "0xa01a69a6b76369dcb7509e8ba575d05005c3ebba5d664122764648e2193b6464";
	let tokenCreatorOperator = "0xa0ff9f75a32bdb897d68ca429da91f97c96fa4d84844bdb761e1f04ffdbfc0b5";
	let tokenReceiverOperator = "0xa028ce37c0c9bc5c588c0b0a6a551c048039be045369e5c016dd7ec3dcc2b3bb";

    let data = web3.avm.contract.method("balanceOf").inputs(['address'],[tokenCreator]).encode();
    console.log(data);
    let transactionObject = {
        to: "",
        data: data,
    };
    let initialResponse = await web3.eth.call(transactionObject);
    let balance = await web3.avm.contract.decode("byte[]", initialResponse);
    console.log("token creator: " + parseInt(toHexString(balance),16)); 


    data = web3.avm.contract.method("balanceOf").inputs(['address'],[tokenReceiver]).encode();
    transactionObject = {
        to: "",
        data: data,
    };
    initialResponse = await web3.eth.call(transactionObject);
    balance = await web3.avm.contract.decode("byte[]", initialResponse);
    console.log("token receiver: " + parseInt(toHexString(balance),16)); 

}

getBalance();

