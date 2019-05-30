const BN = require('bn.js');
const Web3 = require("aion-web3");
const web3 = new Web3(new Web3.providers.HttpProvider("https://aion.api.nodesmith.io/v1/mastery/jsonrpc?apiKey=ec13c1ff5f65488fa6432f5f79e595f6"));


async function getTokenInfo() {


	let tokenCreator = "0xa048630fff033d214b36879e62231cc77d81f45d348f6590d268b9b8cabb88a9";
	let tokenReceiver = "0xa01a69a6b76369dcb7509e8ba575d05005c3ebba5d664122764648e2193b6464";
	let tokenCreatorOperator = "0xa0ff9f75a32bdb897d68ca429da91f97c96fa4d84844bdb761e1f04ffdbfc0b5";
	let tokenReceiverOperator = "0xa028ce37c0c9bc5c588c0b0a6a551c048039be045369e5c016dd7ec3dcc2b3bb";

    let data = web3.avm.contract.method("getBalanceOf").inputs(['address'],[tokenCreator]).encode();
    let transactionObject = {
        to: "0xA0dF59C5C94f481AFb08073ee70d0139cf2d8Fc3C72cdAbAaD2B6323f55F1AeC",
        data: data,
    };
    let initialResponse = await web3.eth.call(transactionObject);
    let balance = await web3.avm.contract.decode("string", initialResponse);
    console.log("token creator: " + balance); 


    data = web3.avm.contract.method("getBalanceOf").inputs(['address'],[tokenReceiver]).encode();
    transactionObject = {
        to: "0xA0dF59C5C94f481AFb08073ee70d0139cf2d8Fc3C72cdAbAaD2B6323f55F1AeC",
        data: data,
    };
    initialResponse = await web3.eth.call(transactionObject);
    balance = await web3.avm.contract.decode("string", initialResponse);
    console.log("token receiver: " + balance); 

}

getTokenInfo();


/*
//initialization
token creator: 333333333000000000000000000
token receiver: 0

//send 3330000000000000000
token creator: 333333000000000000000000000
token receiver: 333000000000000000000

//send 3330000000000000000
token creator: 333333000000000000000000000
token receiver: 333000000000000000000

//operatorsend 3330000000000000000
token creator: 333332663670000000000000000
token receiver: 669330000000000000000


*/