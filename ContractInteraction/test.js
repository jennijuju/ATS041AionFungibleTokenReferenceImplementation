const BN = require('bn.js');
const Web3 = require("aion-web3");
let path = require('path');
const web3 = new Web3(new Web3.providers.HttpProvider("http://localhost:8545"));

async function run() {

const receipt = web3.eth.getTransactionReceipt('0xa0e50b1f3819ff12720312e4e77ddb8a4b03f9271ce53f5be47b717d10a030bd')
                        .then(console.log);
}

run();