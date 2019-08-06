[![Build Status](https://travis-ci.com/jennijuju/ATS041AionFungibleTokenReferenceImplementation.svg?branch=master)](https://travis-ci.com/jennijuju/ATS041AionFungibleTokenReferenceImplementation)

# :warning: Pending Audit - Do Not Use For Production :warning:
# ATS041 - Aion Fungible Token Reference Implementation


This project implements the Aion Fungible Token Java smart contracts following the [Aion Token Standard 041](https://github.com/aionnetwork/AIP/issues/41). This porject can be used directly to deploy fungible tokens on Aion Network. The deployment type is `0x2` since it is implemented in Java and is compatible with Aion AVM.

We recommend to use this project as the starting point for Aion Java contracts which will incorporate/extend fungible token functionality.


## Prerequisites

- [Java 10 or up](https://www.oracle.com/technetwork/java/javase/downloads/jdk12-downloads-5295953.html)
- [Apache Maven](https://beta-docs.aion.network/developers/tools/maven-cli/install/)


## Build

1. Pull the latest `avm.jar` for compilation

```java
./mvnw initialize
```

Run this only if you want to pull the latest jar.

2. Run the tests and compile the contract

```java
./mvnw clean install
```

A build which passes all tests will be indicated by:
> [INFO] BUILD SUCCESS

at the bottom of your build. 

After a successful build, you will get your regular Java compiled jar, ABI compiled and optimized contract jar and contract ABI file under target folder. 

We recommend you to deploy the ABI compiled and optimized jar (the one with _original_ prefix) which can save you more energy!

After you have made any changes, run step2 above.

## Main contract class and extending classes

### ATS041TokenContract.java

This is the main class and also the entry point of your contract. If you change the class name of this class, you will need to update the following line in your `pom.xml`:

```xml
...
<properties>
...
        <contract.main.class>org.aion.ATS041TokenContract</contract.main.class>
</properties>
...
```

We recommend you to NOT change the existing method names in this file to make sure the contract ABI includes the standardized one, so that [Aion Dashboard](https://mainnet.aion.network/#/tokens) and wallets that support ATS tokens can list your token.

### ATS041Implementation.java

Actual implementation codes are in this file, and you can add your extending functions here.

### ATS041Event.java

Event logs are written in this file. Events will be triggered with corresponding transactions.  

We recommend you to NOT change the existing events in this file, so that [Aion Dashboard](https://mainnet.aion.network/#/tokens) and wallets that support ATS tokens can list your token. Also to make sure your token and token activities can be found by the dApps that are listening to the ATS041 Events.

### ATS041KeyValueStorage.java

This class is to generate unique keys for Aion key-value storage model.

## License

This project is assigned copyright to Aion Foundation and released under the MIT license
