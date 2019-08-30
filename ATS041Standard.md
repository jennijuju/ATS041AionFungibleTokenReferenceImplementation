| Title  |Author(s)   |  Original Author | Type  |  Status | Creation Date | Contact Information |
|---|---|---|---|---|---|----|
| Aion Token Standard  | Jiaying Wang  | Sam Pajot-Phipps, Yao Sun, Stephane Gosselin | ASC (Aion Standards and Conventions)  | Draft  | July 28th, 2019 |  jennifer@aion.network |

## Summary

A fungible token standard to meet the functionality requirements of current dApp developers. This AIP is based on [AIP 004](https://github.com/aionnetwork/AIP/blob/master/AIP-004). The reference implementation is in Java and compatible with Aion AVM. 

## Value Proposition

To enable the creation and management of innovative fungible digital assets on the Aion blockchain.

## Motivation

The primary motivation for the proposal is to provide the dApp's that are building on the Aion blockchain(AVM) with a common standard to implement fungible tokens.

## Non-Goals

The scope of this standard is limited to on-chain functionality for fungible tokens. This standard does not address cross-chain functionalities across bridges.

## Success Metrics

There are two key indicators of success for this standard:

1) Number of contract deployments
2) Number of transactions of deployed contracts

## Description

Fungible tokens have proven to be one of the core building blocks of the current decentralized web era. This next-generation fungible token standard reflects the evolving needs of dApp developers for core functionality and security.

The Aion Fungible Token Standard has been designed to address the shortcomings of token standards on other existing blockchains by blurring the line between layer one and layer two. At the core of the design is the ability to perform token transfers while maintaining security and stable total supply. Additional features include safe transfers, token callbacks, and mint/burn interface.

This standard aims to provide a reliable interface which 3rd party tools can use when building products on top of Aion.

### High-Level Architecture

TBD.

## Specification

### Definitions

- Token Creator: The project/user/ account that owns the Token Contract
- Token Contract: A smart contract that is deployed using this token standard
- Token Holder: An account/user that has ownership over a token balance
- Token Sender: An account/user that is sending a token
- Token Receiver: An account/user that is receiving a token
- Token Name: The name of the token
- Token Symbol: The symbol of the token
- Token Granularity: The granularity of the token
- Token Total Supply: Total number of minted tokens

### Methods

#### **Token Information**

**Functions detailed below MUST be implemented.**

**`AIP041Name` function**

> **returns:** Name of the token, e.g, `"AIP041TestToken"`.

``` java
public static String AIP041Name();
```

The following rules MUST be applied regarding the *token name*:

- The *token name* value MUST be set at creation time.
- The *token name* value MUST NOT be changed ever.

**`AIP041Symbol` function**

> **returns:** Symbol of the token, e.g., `"MYT"`.

``` java
public static String AIP041Symbol();
```

The following rules MUST be applied regarding the *token symbol*:

- The *token symbol* value MUST be set at creation time.
- The *token symbol* value MUST NOT be changed ever.


**`AIP041Granularity` function**

> **returns:** The smallest non-divisible part of the token, e.g., '1'.

``` java
public static int AIP041Granularity();
```

The granularity is the smallest number of tokens (the basic unit in the internal denomination, [nAmp](https://github.com/aionnetwork/aion/wiki/Aion-Terminology)) which MAY be minted, sent and burned in any transaction.

The following rules MUST be applied regarding the *token granularity*:

- The *granularity* value MUST be set at creation time.
- The *granularity* value MUST NOT be changed ever.
- The *granularity* value MUST be greater or equal to `1`.
- Any minting, sending and burning of tokens MUST be a multiple of the *granularity* value.
- Any operation that would result in a balance that's not a multiple of the *granularity* value MUST be considered invalid, and the transaction MUST `revert`.

> *NOTE*: Most of the tokens SHOULD have a granularity of `1` unless there is a good reason for not allowing divisibility of the token.


**`AIP041TotalSupply` function**

> **returns:** Get the total number of minted tokens, e.g., '1,000,000,000'

``` java
public static BigInteger AIP041TotalSupply();
```

The following rules MUST be applied regarding the *token total supply*:

- An initial value of *token total supply* MUST be set at creation time.
- The *token total supply* value may be changed by minting and burning.
- The decimals of the token MUST be `18`. Therefore,the initial total supply should be set as in `the desired supply * 10^18` to assure the precision.
  For example, if your desired initial total supply is 1,234 tokens, then it should be set to 1, 234 * 10^18 upon creation.

**`AIP041BalanceOf` function**

> **parameter(s):**  
> `tokenHolder`: Address for which the balance is returned.

> **returns:** Amount of token held by `token holder` in the token contract.

``` java
public static BigInteger AIP041BalanceOf(Address tokenHolder)
```

> *NOTE*: The balance MUST be zero (`0`) or higher.

---
#### **Token Creation**

**`AIP041TokenCreated` event**

Indicate the `totalSupply` of a new token created by `creator` address. This event MUST be emitted upon a token creation process.

> **topics**  
> `"AIP041TokenCreated""`: Name of the event<br>
> `totalSupply`: The initial total supply that is set upon creation <br>
> `creator`: Address that creates the token

> **data**  
> none.

``` java
Blockchain.log("AIP041TokenCreated".getBytes(),
                totalSupply.toByteArray(),
                creator.toByteArray(),
                new byte[0]);
```

> *NOTE*: This event MUST NOT be emitted outside of a token creation process.

---
#### **Operators**

An `operator` is an address which is allowed to send and burn tokens on behalf of another token holder address.

The following rules apply to any *operator*:

- An address MUST always be an *operator* for itself. Hence an address MUST NOT ever be revoked as its own *operator*.
- If an address is an *operator* for a *token holder*, `AIP041IsOperatorFor` MUST return `true`.
- If an address is not an *operator* for a *token holder*, `AIP041IsOperatorFor` MUST return `false`.
- The token contract MUST emit an `AIP041AuthorizedOperator` event with the correct values when a *token holder* authorizes an address as its *operator* as defined in the [`AIP041AuthorizedOperator` Event](authorizedoperator). 
- The token contract MUST emit an `AIP041RevokedOperator` event with the correct values when a *token holder* revokes an address as its *operator* as defined in the [`AIP041RevokedOperator` Event](revokedoperator).

> *NOTE*: <br>
> - A *token holder* MAY have multiple *operators* at the same time. 
> - A *token holder* MAY authorize an already authorized *operator*. An `AIP041AuthorizedOperator` MUST be emitted each time.
> - A *token holder* MAY revoke an already revoked *operator*. An  `AIP041RevokedOperator` MUST be emitted each time.

**`AIP041AuthorizedOperator` event** <a id="authorizedoperator"></a>

Indicates the authorization of `operator` as an *operator* for `tokenHolder`.

> **topics**  
> `"AIP041AuthorizedOperator`: Name of the event<br>
> `operator`: Address which became an *operator* of `tokenHolder`.  
> `tokenHolder`: Address of a token holder which authorized the `operator` address as an *operator*.

> **data**  
> none.

``` java
Blockchain.log("AIP041AuthorizedOperator".getBytes(),
                operator.toByteArray(),
                tokenHolder.toByteArray(),
                new byte[0]);
```

> *NOTE*: This event MUST NOT be emitted outside of an *operator* authorization process.


**`RevokedOperator` event** <a id="revokedoperator"></a>

Indicates the revocation of `operator` as an *operator* for `tokenHolder`.

> **topics**  
> `"AIP041RevokedOperator`: Name of the event.<br>
> `operator`: Address which was revoked as an *operator* of `tokenHolder`.  
> `tokenHolder`: Address of a token holder which revoked the `operator` address as an *operator*.

``` java
 Blockchain.log("AIP041RevokedOperator".getBytes(),
                operator.toByteArray(),
                tokenHolder.toByteArray(),
                new byte[0]); static void RevokedOperator(Address operator, Address tokenHolder)
```

> **data**  
> none.

> *NOTE*: This event MUST NOT be emitted outside of an *operator* revocation process.

***

The `AIP041AuthorizeOperator`, `AIP041RevokeOperator` and `AIP041IsOperatorFor` functions described below MUST be implemented to manage *operators*.
Token contracts MAY implement other functions to manage *operators*.

**`AIP041AuthorizeOperator` function**

Set a third party `operator` address as an *operator* of `Blockchain.getCaller()` to send and burn tokens on its behalf.


> **parameters**  
> `operator`: Address to set as an *operator* for `Blockchain.getCaller()`.

``` java
public static void AIP041AuthorizeOperator(Address operator)
```

> *NOTE*: The *token holder* (`Blockchain.getCaller()`) is always an *operator* for itself. This right MUST NOT be revoked. This function MUST `revert` if it is called to authorize the token holder (`Blockchain.getCaller()`) as an *operator* for itself (i.e. if `operator` is equal to `Blockchain.getCaller()`).


**`AIP041RevokeOperator` function**

Remove the right of the `operator` address to be an *operator* for `Blockchain.getCaller()` and to send and burn tokens on its behalf.

> **parameters**  
> `operator`: Address to rescind as an *operator* for `Blockchain.getCaller()`.

``` java
public static void AIP041RevokeOperator(Address operator)
```

> *NOTE*: The *token holder* (`Blockchain.getCaller()`) is always an *operator* for itself. This right MUST NOT be revoked. This function MUST `revert` if it is called to revoke the token holder (`Blockchain.getCaller()`) as an *operator* for itself (i.e., if `operator` is equal to `Blockchain.getCaller()`).

**`AIP041IsOperatorFor` function** <a id="isOperatorFor"></a>

Indicate whether the `operator` address is an *operator* of the `tokenHolder` address.

> **parameters**  
> `operator`: Address which may be an *operator* of `tokenHolder`.  
> `tokenHolder`: Address of a token holder which may have the `operator` address as an *operator*.

> **returns:** `true` if `operator` is an *operator* of `tokenHolder` and `false` otherwise.

``` java
public static boolean AIP041IsOperatorFor(Address operator, Address tokenHolder) 
```

---

#### **Sending Tokens**

When an *operator* sends an `amount` of tokens from a *token holder* to a *recipient* with the associated `data` and `operatorData`, the token contract MUST apply the following rules:

- Any *token holder* MAY send tokens to any *recipient*, except for `0x0`(buring).
- The balance of the *token holder* MUST be decreased by the `amount`.
- The balance of the *recipient* MUST be increased by the `amount`.
- The balance of the *token holder* MUST be greater or equal to the `amount`&mdash;such that its resulting balance is greater or equal to zero (`0`) after the send.
- The token contract MUST emit a `AIP041Sent` event with the correct values as defined in the [`AIP041Sent` Event](sent).
- The *operator* MAY communicate any information in the `operatorData`.
- The `data` and `operatorData` MUST be immutable during the entire send process&mdash;hence the same `data` and `operatorData` MUST be used to call both hooks and emit the `Sent` event.

The token contract MUST `revert` when sending in any of the following cases:

- The *operator* address is not an authorized operator for the *token holder*.
- The resulting *token holder* balance or *recipient* balance after the send is not a multiple of the *granularity* defined by the token contract.
- The address of the *token holder* or the *recipient* is `0x0`.
- The address of the *recipient* is the token contract itself (Blockchain.getAddress()).
- Any of the resulting balances becomes negative, i.e. becomes less than zero (`0`).

The token contract MAY send tokens from many *token holders*, to many *recipients*, or both. In this case:

- The previous send rules MUST apply to all the *token holders* and all the *recipients*.
- The sum of all the balances incremented MUST be equal to the total sent `amount`.
- The sum of all the balances decremented MUST be equal to the total sent `amount`.
- A `API041Sent` event MUST be emitted for every *token holder* and *recipient* pair with the corresponding amount for each pair.
- The sum of all the amounts from the `API041Sent` event MUST be equal to the total sent `amount`.

> *NOTE*: 
- Mechanisms such as applying a fee on a send is considered as a send to multiple *recipients*: the intended *recipient* and the fee *recipient*.
- Transfer of tokens MAY be chained. For example, if a contract upon receiving tokens sends them further to another address. In this case, the previous send rules apply to each send, in order.
- Sending an amount of zero (`0`) tokens are valid and MUST be treated as a regular send.

*Implementation Requirement*:  
- The token contract MUST call the `callSender` hook *before* updating the state.
- The token contract MUST call the `callRecipient` hook *after* updating the state.  
I.e., `tokensToSend` MUST be called first, then the balances MUST be updated to reflect the send, and finally `tokensReceived` MUST be called *afterward*. Thus a `balanceOf` call within `tokensToSend` returns the balance of the address *before* the send and a `balanceOf` call within `tokensReceived` returns the balance of the address *after* the send.

*NOTE*: The `data` field contains extra information intended for, and defined by the recipient&mdash; similar to the data field in a regular ether send transaction. Typically, `data` is used to describe the intent behind the send. The `operatorData` MUST only be provided by the *operator*. It is intended more for logging purposes and particular cases. (Examples include payment references, cheque numbers, countersignatures and more.) In most of the cases the recipient would ignore the `operatorData`, or at most, it would log the `operatorData`.

**`AIP041Sent` event** <a id="sent"></a>

Indicate a send of `amount` of tokens from the `from` address to the `to` address by the `operator` address.

> **topics**  
> `"AIP041Sent"`: Name of the event<br>
> `operator`: Address which triggered the send.  
> `from`: Token holder.  
> `to`: Token recipient.  

> **data** <br>
> `amount`: Amount of the token was sent <br>
> `holderData.length`: The length of the data attached to the send by the `holder` <br>
> `holderData`: Information attached to the send by the `holder` <br>
> `operatorData.length` : The length of the data attached to the send by the `operator` <br>
> `operatorData` : Information attached to the send by the `operator` <br>

``` java
byte[] data = AionBuffer.allocate(BIGINTEGER_LENGTH + Integer.BYTES + holderData.length + Integer.BYTES + operatorData.length)
                .put32ByteInt(amount)
                .putInt(holderData.length)
                .put(holderData)
                .putInt(operatorData.length)
                .put(operatorData)
                .getArray();

Blockchain.log("AIP041Sent".getBytes(),
                operator.toByteArray(),
                from.toByteArray(),
                to.toByteArray(),
                data);
```

> *NOTE*: This event MUST NOT be emitted outside of a send or an ATS041Token  transfer process.

The `AIP041Send` and `AIP041OperatorSend` functions described below MUST be implemented to send tokens.
Token contracts MAY implement other functions to send tokens.

> *NOTE*: An address MAY send an amount of `0`, which is valid and MUST be treated as a regular send.

**`AIP041Send` function** <a id="send"></a>

Send the `amount` of tokens from the address `Blockchain.getCaller()` to the address `to`.

The *operator* and the *token holder* MUST both be the `Blockchain.getCaller()`.

> **parameters**  
> `to`: Token recipient.  
> `amount`: Number of tokens to send.  
> `data`: Information attached to the send, and intended for the recipient (`to`).

``` java
public static void AIP041Send(Address to, BigInteger amount, byte[] userData)
```

**`AIP041OperatorSend` function** <a id="operatorSend"></a>

Send the `amount` of tokens on behalf of the address `from` to the address `to`.

The *operator* MUST be `Blockchain.getCaller()`. The value of `from` MAY be `0x0`, then the `from` (*token holder*) used for the send MUST be `Blockchain.getCaller()` (the `operator`).

*Reminder*: If the *operator* address is not an authorized operator of the `from` address, then the sending process MUST `revert`.

*NOTE*: `from` and `Blockchain.getCaller()` MAY be the same address. I.e., an address MAY call `AIP041OperatorSend` for itself. This call MUST be equivalent to `send` with the addition that the *operator* MAY specify an explicit value for `operatorData` (which cannot be done with the `send` function).

> **parameters**  
> `from`: Token holder (or `0x0` to set `from` to `Blockchain.getCaller()`).  
> `to`: Token recipient.  
> `amount`: Number of tokens to send.  
> `userData`: Information attached to the send, and intended for the recipient (`to`).  
> `operatorData`: Information attached to the send by the `operator`.

``` java
public static void AIP041OperatorSend(Address from, Address to, BigInteger amount, byte[] userData, byte[] operatorData)
```


#### **Minting Tokens**

Minting tokens is the act of producing new tokens. This standard intentionally does not define specific functions to mint tokens. This intent comes from the wish not to limit the use of the standard as the minting process is generally an implementation detail.

Nonetheless, the rules below MUST be respected when minting for a *recipient*:

- Tokens MAY be minted for any *recipient* address.
- The total supply MUST be increased by the amount of tokens minted.
- The balance of `0x0` MUST NOT be decreased.
- The balance of the *recipient* MUST be increased by the amount of tokens minted.
- The token contract MUST emit a `Minted` event with the correct values as defined in the [`AIP041Minted` Event][minted].
- The `data` and `operatorData` MUST be immutable during the entire mint process&mdash;hence the same `data` and `operatorData` MUST be used to call the `tokensReceived` hook and emit the `Minted` event.

The token contract MUST `revert` when minting in any of the following cases:

- The resulting *recipient* balance after the mint is not a multiple of the *granularity* defined by the token contract.
- The *recipient* is a contract, and it does not implement the `AIP041TokenRecipient` interface via [AIP008].
- The address of the *recipient* is `0x0`.

*NOTE*: The initial token supply at the creation of the token contract MUST be considered as minting for the amount of the initial supply to the address (or addresses) receiving the initial supply.

The token contract MAY mint tokens for multiple *recipients* at once. In this case:

- The previous mint rules MUST apply to all the *recipients*.
- The sum of all the balances incremented MUST be equal to the total minted amount.
- A `AIP041Minted` event MUST be emitted for every *recipient* with the corresponding amount for each *recipient*.
- The sum of all the amounts from the `Minted` event MUST be equal to the total minted `amount`.

*NOTE*: Minting an amount of zero (`0`) tokens is valid and MUST be treated as a regular mint.

*NOTE*: The `data` field contains extra information intended for, and defined by the recipient&mdash; similar to the data field in a regular ether send transaction. Typically, `data` is used to describe the intent behind the mint. The `operatorData` MUST only be provided by the *operator*. It is intended more for logging purposes and particular cases. (Examples include payment references, cheque numbers, countersignatures and more.) In most of the cases the recipient would ignore the `operatorData`, or at most, it would log the `operatorData`.

**`AIP041Minted` event** <a id="minted"></a>

Indicate the minting of `amount` of tokens to the `to` address by the `operator` address.

*NOTE*: This event MUST NOT be emitted outside of a mint process.

> **topics**  
> `"AIP041Minted"`: Name of the event<br>
> `issuer`: Address that minted the new tokens.  
> `to`: Address that receives the tokens.  <br>
> `amount`: Amount of the token was minted.  

> **data** <br>
> `data.length`: The length of the information attached to the minting, and intended for the recipient (to). <br>
> `data`: Information attached to the minting, and intended for the recipient (to) <br>
> `issuerData.length` : The length of the data attached to the minting by the `issuer` <br>
> `issuerData` : Information attached to the minting by the `issuer` <br>


#### **Burning Tokens**

Burning tokens is the act of destroying existing tokens. AIP explicitly defines two functions to burn tokens (`API041burn` and `AIP041OperatorBurn`). These functions facilitate the integration of the burning process in wallets and dapps. However, the token contract MAY prevent some or all *token holders* from burning tokens for any reason. The token contract MAY also define other functions to burn tokens.

The rules below MUST be respected when burning the tokens of a *token holder*:

- Tokens MAY be burned from any *token holder* address.
- The total supply MUST be decreased by the amount of tokens burned.
- The balance of `0x0` MUST NOT be increased.
- The balance of the *token holder* MUST be decreased by amount of tokens burned.
- The token contract MUST emit a `AIP041Burned` event with the correct values as defined in the [`AIP041Burned` Event](burned).
- The `AIP041OperatorData` MUST be immutable during the entire burn process&mdash;hence the same `operatorData` MUST be used to call the `tokensToSend` hook and emit the `Burned` event.

The token contract MUST `revert` when burning in any of the following cases:

- The *operator* address is not an authorized operator for the *token holder*.
- The resulting *token holder* balance after the burn is not a multiple of the *granularity* defined by the token contract.
- The balance of *token holder* is inferior to the amount of tokens to burn (i.e., resulting in a negative balance for the *token holder*).
- The address of the *token holder* is `0x0`.

The token contract MAY burn tokens for multiple *token holders* at once. In this case:

- The previous burn rules MUST apply to each *token holders*.
- The sum of all the balances decremented MUST be equal to the total burned amount.
- A `AIP041Burned` event MUST be emitted for every *token holder* with the corresponding amount for each *token holder*.
- The sum of all the amounts from the `Burned` event MUST be equal to the total burned `amount`.

*NOTE*: Burning an amount of zero (`0`) tokens is valid and MUST be treated as a regular burn.

**`AIP041Burned` event** <a id="burned"></a>

Indicate the burning of `amount` of tokens from the `from` address by the `operator` address.

> **topics**  
> `"AIP041Burned"`: Name of the event <br>
> `operator`: Address which triggered the burn.  
> `from`: Token holder whose tokens are burned.  

> **data** <br>
> `amount`: Amount of the token was burned <br>
> `holderData.length`: The length of the data attached to the burn by the `holder` <br>
> `holderData`: Information attached to the send by the `holder` <br>
> `operatorData.length` : The length of the data attached to the send by the `operator` <br>
> `operatorData` : Information attached to the send by the `operator` <br>

``` java
byte[] data = AionBuffer.allocate(BIGINTEGER_LENGTH + Integer.BYTES + holderData.length + Integer.BYTES + operatorData.length)
                .put32ByteInt(amount)
                .putInt(holderData.length)
                .put(holderData)
                .putInt(operatorData.length)
                .put(operatorData)
                .getArray();

        Blockchain.log("AIP041Burned".getBytes(),
                operator.toByteArray(),
                from.toByteArray(),
                data);
```

> *NOTE*: This event MUST NOT be emitted outside of a burn process.


The `AIP041Burn` and `AIP041OperatorBurn` functions described below MUST be implemented to burn tokens.
Token contracts MAY implement other functions to burn tokens.

**`AIP041Burn` function** <a id="burn"></a>

Burn the `amount` of tokens from the address `Blockchain.getCaller()`.

The *operator* and the *token holder* MUST both be the `Blockchain.getCaller()`.

> **parameters**  
> `amount`: Number of tokens to burn <br>
> `holderData`: Information attached to the send by the `holder` <br>

``` java
public static void AIP041Burn(BigInteger amount, byte[] holderData))
 ```

**`AIP041OperatorBurn` function** <a id="operatorBurn"></a>

Burn the `amount` of tokens on behalf of the address `tokenHolder`.

The *operator* MUST be `Blockchain.getCaller()`. The value of `tokenHolder` MAY be `0x0`, then the `tokenHolder` used for the burn MUST be `Blockchain.getCaller()` (the `operator`).

*Reminder*: If the *operator* address is not an authorized operator of the `tokenHolder` address, then the burn process MUST `revert`.

> **parameters**  
> `tokenHolder`: Token holder whose tokens will be burned (or `0x0` to set `from` to `Blockchain.getCaller()`). <br>
> `amount`: Number of tokens to burn.  <br>
> `holderDara`: Information attached to the burn by the `holder` <br>
> `operatorData`: Information attached to the burn by the *operator*.

``` java
    public static void AIP041OperatorBurn(Address tokenHolder, BigInteger amount, byte[] holderData, byte[] operatorData)
```

>*NOTE*: 
> - The *operator* MAY pass any information via `operatorData`. The `operatorData` MUST only be provided by the *operator*.
> - `tokenHolder` and `Blockchain.getCaller()` MAY be the same address. I.e., an address MAY call `operatorBurn` for itself. This call MUST be equivalent to `burn` with the addition that the *operator* MAY specify an explicit value for `operatorData` (which cannot be done with the `burn` function).

### Java Smart Contract ABI

1 <br>
org.aion.AIP041TokenContract <br>
Clinit: (String, String, int, BigInteger)  <br>
public static String AIP041Name() <br>
public static String AIP041Symbol() <br>
public static int AIP041Granularity() <br>
public static BigInteger AIP041TotalSupply() <br>
public static BigInteger AIP041BalanceOf(Address) <br>
public static void AIP041AuthorizeOperator(Address) <br>
public static void AIP041RevokeOperator(Address) <br>
public static boolean AIP041IsOperatorFor(Address, Address) <br>
public static void AIP041Send(Address, BigInteger, byte[]) <br>
public static void AIP041OperatorSend(Address, Address, BigInteger, byte[], byte[]) <br>
public static void AIP041Burn(BigInteger, byte[]) <br>
public static void AIP041OperatorBurn(Address, BigInteger, byte[], byte[]) <br>


## Logic

This standard is based on [AIP-004](https://github.com/aionnetwork/AIP/blob/master/AIP-004/AIP%23004.md) from Aion Network AIP. The following modifications have been made:

- Implementation is now in Java, that is compatible with AVM.  



## Risks & Assumptions


## Test Cases

https://github.com/jennijuju/ATS041AionFungibleTokenReferenceImplementation/blob/master/src/test/java/org/aion/AIP041TokenContractTest.java
## Implementations

[Reference Implementation](https://github.com/jennijuju/ATS041AionFungibleTokenReferenceImplementation)

>Note: This implementation is still under development and pending an audit for security check.

## Dependencies


## Copyright

All AIP’s are public domain. Copyright waiver: https://creativecommons.org/publicdomain/zero/1.0/