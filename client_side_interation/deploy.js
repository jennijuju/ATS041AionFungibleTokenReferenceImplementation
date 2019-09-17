
const BN = require('bn.js');
const Web3 = require("aion-web3");
let path = require('path');

// Set up web3
const web3 = new Web3(new Web3.providers.HttpProvider(""));
const pk = ""


//Create contract interface
let abi = `
1
org.aion.AIP041TokenContract
Clinit: (String, String, int, BigInteger)
public static String AIP041Name()
public static String AIP041Symbol()
public static int AIP041Granularity()
public static BigInteger AIP041TotalSupply()
public static BigInteger AIP041BalanceOf(Address)
public static void AIP041AuthorizeOperator(Address)
public static void AIP041RevokeOperator(Address)
public static boolean AIP041IsOperatorFor(Address, Address)
public static void AIP041Send(Address, BigInteger, byte[])
public static void AIP041OperatorSend(Address, Address, BigInteger, byte[], byte[])
public static void AIP041Burn(BigInteger, byte[])
public static void AIP041OperatorBurn(Address, BigInteger, byte[], byte[])
`;

let abiObj = web3.avm.contract.Interface(abi); 
web3.avm.contract.initBinding(null, abiObj, pk,web3);


async function deploy() {

  //contract
  let jarPath = path.join(__dirname,'../target','AionFungibleToken-1.0-SNAPSHOT.jar');

  //deploy
  let name = "J3NAIP041";
  let symbol = "J3N";
  let granularity = 1;
  let totalsupply =  new BN("1000000000000000000");
  let res =await  web3.avm.contract.deploy(jarPath).args([name, symbol, granularity, totalsupply]).initSend();
  console.log(res);
  return res;  
}

deploy();




