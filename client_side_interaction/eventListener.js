const Web3 = require("aion-web3");

// Set up node
const web3 = new Web3(new Web3.providers.HttpProvider(""));

//Create contract instance
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
let contract = "0xa0e1037280e97a7c6399fc55f9de421cb3bdccda4a212ea3059bdb77a5754a62";
web3.avm.contract.initBinding(contract, abiObj, null,web3);


//Get past logs
async function logs(){

	//past log obj
	let pastLogObj = {
		"fromBlock": 3293700,
		"topics" : [ //Order matters.
			"0x414950303431546f6b656e437265617465640000000000000000000000000000",
			"0x0000000000000000000000000000000000000000000000000de0b6b3a7640000",
			null /*optional */,		]
	}
	try {      
		res = await web3.avm.contract.getPastLogs(pastLogObj);
		if(res.length){ //Log can be found -> event is triggered.
			console.log("Logs: ", res);
			//..filtering actions here
		}else{
			console.log("Log cannot be found.");
		}
    } catch (error) {
      console.log("Past event error: ",error);
      return false;
    }
 }
 logs();

		
