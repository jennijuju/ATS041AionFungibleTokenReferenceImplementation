const BN = require('bn.js');
const Web3 = require("aion-web3");
let path = require('path');
const web3 = new Web3(new Web3.providers.HttpProvider("http://localhost:8545"));

async function run() {

const receipt = web3.eth.getTransactionReceipt('0xf812b0efdd2c4820c73c6ed10f0ac3c76c0b28adbff191075269b572614cd462')
                        .then(console.log);
}

run();