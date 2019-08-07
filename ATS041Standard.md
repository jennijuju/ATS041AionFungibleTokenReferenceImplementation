| Title  |Author(s)   |  Original Author | Type  |  Status | Creation Date | Contact Information |
|---|---|---|---|---|---|----|
| Aion Token Standard  | Jiaying Wang  | Sam Pajot-Phipps, Yao Sun, Stephane Gosselin | ASC (Aion Standards and Conventions)  | Draft  | July 28th, 2019 |  jennifer@aion.network |

## Summary

A fungible token standard to meet the functionality requirements of current dApp developers. This AIP is to remove cross-chain token functionalities from  [AIP 004](https://github.com/aionnetwork/AIP/blob/master/AIP-004). The reference implementation should be in Java and compatible with Aion AVM. 

## Value Proposition

To enable the creation and management of innovative fungible digital assets on the Aion blockchain.

## Motivation

The primary motivation for the proposal is to provide the dApp's that are building on the Aion blockchain with a common standard to implement fungible tokens.

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


### Methods

#### **Token Information**

**Functions detailed below MUST be implemented.**

**`ATS041Name` function**

> **returns:** Name of the token, e.g, `"ATS041TestToken"`.

``` java
public static String ATS041Name();
```

The name of the token MUST be set at creation time.

**`ATS041Symbol` function**

> **returns:** Symbol of the token, e.g., `"MYT"`.

``` java
public static String ATS041Symbol();
```

The symbol of the token MUST be set at creation time.

**`ATS041Granularity` function**

> **returns:** The smallest non-divisible part of the token, e.g., '1'.

``` java
public static int ATS041Granularity();
```

The granularity is the smallest number of tokens (in the basic unit, [nAmp](https://github.com/aionnetwork/aion/wiki/Aion-Terminology)) which MAY be minted, sent and burned in any transaction.

The following rules MUST be applied regarding the *granularity*:

- The *granularity* value MUST be set at creation time.
- The *granularity* value MUST NOT be changed ever.
- The *granularity* value MUST be greater or equal to `1`.
- Any minting, sending and burning of tokens MUST be a multiple of the *granularity* value.
- Any operation that would result in a balance that's not a multiple of the *granularity* value MUST be considered invalid, and the transaction MUST `revert`.

*NOTE*: Most of the tokens SHOULD have a granularity of `1` unless there is a good reason for not allowing divisibility of the token.


**`ATS041TotalSupply` function**

> **returns:** Get the total number of minted tokens, e.g., '1,000,000,000'

``` java
public static BigInteger ATS041TotalSupply();
 ```


**`balanceOf` function**

> **parameter(s):**  
> `tokenHolder`: Address for which the balance is returned.

> **returns:** Amount of token held by `token holder` in the token contract.

``` java
public static byte[] balanceOf(Address tokenHolder)
```

*NOTE*: The balance MUST be zero (`0`) or higher.



#### **Token Creation**

**`ATSTokenCreated` event** <a id="ATSTokenCreated"></a>

Indicate the `totalSupply` of a new token created by `creator` address.

*NOTE*: This event MUST NOT be emitted outside of a token creation process.

> **parameters**  
> `totalSupply`: Total number of the minted token upon token creation.  
> `creator`: Address of who creates the token.  

``` java
protected static void ATSTokenCreated(BigInteger totalSupply, Address creator)
```

#### **Operators**

An `operator` is an address which is allowed to send and burn tokens on behalf of another token holder address.

The following rules apply to any *operator*:

- An address MUST always be an *operator* for itself. Hence an address MUST NOT ever be revoked as its own *operator*.
- If an address is an *operator* for a *token holder*, `isOperatorFor` MUST return `true`.
- If an address is not an *operator* for a *token holder*, `isOperatorFor` MUST return `false`.
- The token contract MUST emit an `AuthorizedOperator` event with the correct values when a *token holder* authorizes an address as its *operator* as defined in the [`AuthorizedOperator` Event][authorizedoperator]. 
- The token contract MUST emit a `RevokedOperator` event with the correct values when a *token holder* revokes an address as its *operator* as defined in the [`RevokedOperator` Event][revokedoperator].

*NOTE*: A *token holder* MAY have multiple *operators* at the same time. 

*NOTE*: A *token holder* MAY authorize an already authorized *operator*. An `AuthorizedOperator` MUST be emitted each time.
*NOTE*: A *token holder* MAY revoke an already revoked *operator*. A `RevokedOperator` MUST be emitted each time.

**`AuthorizedOperator` event** <a id="authorizedoperator"></a>

Indicates the authorization of `operator` as an *operator* for `tokenHolder`.

*NOTE*: This event MUST NOT be emitted outside of an *operator* authorization process.

> **parameters**  
> `operator`: Address which became an *operator* of `tokenHolder`.  
> `tokenHolder`: Address of a token holder which authorized the `operator` address as an *operator*.

``` java
protected static void AuthorizedOperator(Address operator, Address tokenHolder)
```

**`RevokedOperator` event** <a id="revokedoperator"></a>

Indicates the revocation of `operator` as an *operator* for `tokenHolder`.

*NOTE*: This event MUST NOT be emitted outside of an *operator* revocation process.

> **parameters**  
> `operator`: Address which was revoked as an *operator* of `tokenHolder`.  
> `tokenHolder`: Address of a token holder which revoked the `operator` address as an *operator*.

``` java
protected static void RevokedOperator(Address operator, Address tokenHolder)
```

The `authorizeOperator`, `revokeOperator` and `isOperatorFor` functions described below MUST be implemented to manage *operators*.
Token contracts MAY implement other functions to manage *operators*.

**`authorizeOperator` function**

Set a third party `operator` address as an *operator* of `Blockchain.getCaller()` to send and burn tokens on its behalf.

*NOTE*: The *token holder* (`Blockchain.getCaller()`) is always an *operator* for itself. This right MUST NOT be revoked. This function MUST `revert` if it is called to authorize the token holder (`Blockchain.getCaller()`) as an *operator* for itself (i.e. if `operator` is equal to `Blockchain.getCaller()`).

> **parameters**  
> `operator`: Address to set as an *operator* for `Blockchain.getCaller()`.

``` java
public static void authorizeOperator(Address operator)
```

**`revokeOperator` function**

Remove the right of the `operator` address to be an *operator* for `Blockchain.getCaller()` and to send and burn tokens on its behalf.

*NOTE*: The *token holder* (`Blockchain.getCaller()`) is always an *operator* for itself. This right MUST NOT be revoked. This function MUST `revert` if it is called to revoke the token holder (`Blockchain.getCaller()`) as an *operator* for itself (i.e., if `operator` is equal to `Blockchain.getCaller()`).

> **parameters**  
> `operator`: Address to rescind as an *operator* for `Blockchain.getCaller()`.

``` java
public static void revokeOperator(Address operator)
```

**`isOperatorFor` function** <a id="isOperatorFor"></a>

Indicate whether the `operator` address is an *operator* of the `tokenHolder` address.

> **parameters**  
> `operator`: Address which may be an *operator* of `tokenHolder`.  
> `tokenHolder`: Address of a token holder which may have the `operator` address as an *operator*.

> **returns:** `true` if `operator` is an *operator* of `tokenHolder` and `false` otherwise.

``` java
public static boolean isOperatorFor(Address operator, Address tokenHolder) 
```

#### **Sending Tokens**

When an *operator* sends an `amount` of tokens from a *token holder* to a *recipient* with the associated `data` and `operatorData`, the token contract MUST apply the following rules:

- Any *token holder* MAY send tokens to any *recipient*.
- The balance of the *token holder* MUST be decreased by the `amount`.
- The balance of the *recipient* MUST be increased by the `amount`.
- The balance of the *token holder* MUST be greater or equal to the `amount`&mdash;such that its resulting balance is greater or equal to zero (`0`) after the send.
- The token contract MUST emit a `Sent` event with the correct values as defined in the [`Sent` Event][sent].
- The *operator* MAY communicate any information in the `operatorData`.
- The `data` and `operatorData` MUST be immutable during the entire send process&mdash;hence the same `data` and `operatorData` MUST be used to call both hooks and emit the `Sent` event.

The token contract MUST `revert` when sending in any of the following cases:

- The *operator* address is not an authorized operator for the *token holder*.
- The resulting *token holder* balance or *recipient* balance after the send is not a multiple of the *granularity* defined by the token contract.
- The address of the *token holder* or the *recipient* is `0x0`.
- Any of the resulting balances becomes negative, i.e. becomes less than zero (`0`).

The token contract MAY send tokens from many *token holders*, to many *recipients*, or both. In this case:

- The previous send rules MUST apply to all the *token holders* and all the *recipients*.
- The sum of all the balances incremented MUST be equal to the total sent `amount`.
- The sum of all the balances decremented MUST be equal to the total sent `amount`.
- A `Sent` event MUST be emitted for every *token holder* and *recipient* pair with the corresponding amount for each pair.
- The sum of all the amounts from the `Sent` event MUST be equal to the total sent `amount`.

*NOTE*: Mechanisms such as applying a fee on a send is considered as a send to multiple *recipients*: the intended *recipient* and the fee *recipient*.

*NOTE*: Transfer of tokens MAY be chained. For example, if a contract upon receiving tokens sends them further to another address. In this case, the previous send rules apply to each send, in order.

*NOTE*: Sending an amount of zero (`0`) tokens are valid and MUST be treated as a regular send.

*Implementation Requirement*:  
- The token contract MUST call the `tokensToSend` hook *before* updating the state.
- The token contract MUST call the `tokensReceived` hook *after* updating the state.  
I.e., `tokensToSend` MUST be called first, then the balances MUST be updated to reflect the send, and finally `tokensReceived` MUST be called *afterward*. Thus a `balanceOf` call within `tokensToSend` returns the balance of the address *before* the send and a `balanceOf` call within `tokensReceived` returns the balance of the address *after* the send.

*NOTE*: The `data` field contains extra information intended for, and defined by the recipient&mdash; similar to the data field in a regular ether send transaction. Typically, `data` is used to describe the intent behind the send. The `operatorData` MUST only be provided by the *operator*. It is intended more for logging purposes and particular cases. (Examples include payment references, cheque numbers, countersignatures and more.) In most of the cases the recipient would ignore the `operatorData`, or at most, it would log the `operatorData`.

**`Sent` event** <a id="sent"></a>

Indicate a send of `amount` of tokens from the `from` address to the `to` address by the `operator` address.

*NOTE*: This event MUST NOT be emitted outside of a send or an [ERC20] transfer process.

> **parameters**  
> `operator`: Address which triggered the send.  
> `from`: Token holder.  
> `to`: Token recipient.  
> `amount`: Number of tokens to send.  
> `data`: Information attached to the send, and intended for the recipient (`to`).  
> `operatorData`: Information attached to the send by the `operator`.

``` java
protected static void Sent(Address operator, Address from, Address to, BigInteger amount, byte[] holderData, byte[] operatorData)
```

The `send` and `operatorSend` functions described below MUST be implemented to send tokens.
Token contracts MAY implement other functions to send tokens.

*NOTE*: An address MAY send an amount of `0`, which is valid and MUST be treated as a regular send.

**`send` function** <a id="send"></a>

Send the `amount` of tokens from the address `Blockchain.getCaller()` to the address `to`.

The *operator* and the *token holder* MUST both be the `Blockchain.getCaller()`.

> **parameters**  
> `to`: Token recipient.  
> `amount`: Number of tokens to send.  
> `data`: Information attached to the send, and intended for the recipient (`to`).

``` java
public static void send(Address to, byte[] amount, byte[] userData)
```

**`operatorSend` function** <a id="operatorSend"></a>

Send the `amount` of tokens on behalf of the address `from` to the address `to`.

The *operator* MUST be `Blockchain.getCaller()`. The value of `from` MAY be `0x0`, then the `from` (*token holder*) used for the send MUST be `Blockchain.getCaller()` (the `operator`).

*Reminder*: If the *operator* address is not an authorized operator of the `from` address, then the sending process MUST `revert`.

*NOTE*: `from` and `Blockchain.getCaller()` MAY be the same address. I.e., an address MAY call `operatorSend` for itself. This call MUST be equivalent to `send` with the addition that the *operator* MAY specify an explicit value for `operatorData` (which cannot be done with the `send` function).

> **parameters**  
> `from`: Token holder (or `0x0` to set `from` to `Blockchain.getCaller()`).  
> `to`: Token recipient.  
> `amount`: Number of tokens to send.  
> `data`: Information attached to the send, and intended for the recipient (`to`).  
> `operatorData`: Information attached to the send by the `operator`.

``` java
public static void operatorSend(Address from, Address to, byte[] amount, byte[] userData, byte[] operatorData)
```

#### **Minting Tokens**

Minting tokens is the act of producing new tokens. This standard intentionally does not define specific functions to mint tokens. This intent comes from the wish not to limit the use of the standard as the minting process is generally an implementation detail.

Nonetheless, the rules below MUST be respected when minting for a *recipient*:

- Tokens MAY be minted for any *recipient* address.
- The total supply MUST be increased by the amount of tokens minted.
- The balance of `0x0` MUST NOT be decreased.
- The balance of the *recipient* MUST be increased by the amount of tokens minted.
- The token contract MUST emit a `Minted` event with the correct values as defined in the [`Minted` Event][minted].
- The `data` and `operatorData` MUST be immutable during the entire mint process&mdash;hence the same `data` and `operatorData` MUST be used to call the `tokensReceived` hook and emit the `Minted` event.

The token contract MUST `revert` when minting in any of the following cases:

- The resulting *recipient* balance after the mint is not a multiple of the *granularity* defined by the token contract.
- The *recipient* is a contract, and it does not implement the `AIP004TokenRecipient` interface via [AIP008].
- The address of the *recipient* is `0x0`.

*NOTE*: The initial token supply at the creation of the token contract MUST be considered as minting for the amount of the initial supply to the address (or addresses) receiving the initial supply.

The token contract MAY mint tokens for multiple *recipients* at once. In this case:

- The previous mint rules MUST apply to all the *recipients*.
- The sum of all the balances incremented MUST be equal to the total minted amount.
- A `Minted` event MUST be emitted for every *recipient* with the corresponding amount for each *recipient*.
- The sum of all the amounts from the `Minted` event MUST be equal to the total minted `amount`.

*NOTE*: Minting an amount of zero (`0`) tokens is valid and MUST be treated as a regular mint.

*NOTE*: The `data` field contains extra information intended for, and defined by the recipient&mdash; similar to the data field in a regular ether send transaction. Typically, `data` is used to describe the intent behind the mint. The `operatorData` MUST only be provided by the *operator*. It is intended more for logging purposes and particular cases. (Examples include payment references, cheque numbers, countersignatures and more.) In most of the cases the recipient would ignore the `operatorData`, or at most, it would log the `operatorData`.

**`Minted` event** <a id="minted"></a>

Indicate the minting of `amount` of tokens to the `to` address by the `operator` address.

*NOTE*: This event MUST NOT be emitted outside of a mint process.

> **parameters**  
> `operator`: Address which triggered the mint.  
> `to`: Token recipient.  
> `amount`: Number of tokens minted.  
> `data`: Information attached to the minting, and intended for the recipient (`to`).  
> `operatorData`: Information attached to the minting by the `operator`.

``` java
TODO: 
```

#### **Burning Tokens**

Burning tokens is the act of destroying existing tokens. ATS explicitly defines two functions to burn tokens (`burn` and `operatorBurn`). These functions facilitate the integration of the burning process in wallets and dapps. However, the token contract MAY prevent some or all *token holders* from burning tokens for any reason. The token contract MAY also define other functions to burn tokens.

The rules below MUST be respected when burning the tokens of a *token holder*:

- Tokens MAY be burned from any *token holder* address.
- The total supply MUST be decreased by the amount of tokens burned.
- The balance of `0x0` MUST NOT be increased.
- The balance of the *token holder* MUST be decreased by amount of tokens burned.
- The token contract MUST emit a `Burned` event with the correct values as defined in the [`Burned` Event][burned].
- The `operatorData` MUST be immutable during the entire burn process&mdash;hence the same `operatorData` MUST be used to call the `tokensToSend` hook and emit the `Burned` event.
- The `data` field of the `tokensToSend` hook MUST be empty.

The token contract MUST `revert` when burning in any of the following cases:

- The *operator* address is not an authorized operator for the *token holder*.
- The resulting *token holder* balance after the burn is not a multiple of the *granularity* defined by the token contract.
- The balance of *token holder* is inferior to the amount of tokens to burn (i.e., resulting in a negative balance for the *token holder*).
- The address of the *token holder* is `0x0`.

The token contract MAY burn tokens for multiple *token holders* at once. In this case:

- The previous burn rules MUST apply to each *token holders*.
- The sum of all the balances decremented MUST be equal to the total burned amount.
- A `Burned` event MUST be emitted for every *token holder* with the corresponding amount for each *token holder*.
- The sum of all the amounts from the `Burned` event MUST be equal to the total burned `amount`.

*NOTE*: Burning an amount of zero (`0`) tokens is valid and MUST be treated as a regular burn.

**`Burned` event** <a id="burned"></a>

Indicate the burning of `amount` of tokens from the `from` address by the `operator` address.

*NOTE*: This event MUST NOT be emitted outside of a burn process.

> **parameters**  
> `operator`: Address which triggered the burn.  
> `from`: Token holder whose tokens are burned.  
> `amount`: Number of tokens burned.  
> `holderData`: Information attached to the burn by the `holder`.  
> `operatorData`: Information attached to the burn by the `operator`.

``` java
protected static void Burned(Address operator, Address from, BigInteger amount, byte[] holderData, byte[] operatorData)
```

The `burn` and `operatorBurn` functions described below MUST be implemented to burn tokens.
Token contracts MAY implement other functions to burn tokens.

**`burn` function** <a id="burn"></a>

Burn the `amount` of tokens from the address `Blockchain.getCaller()`.

The *operator* and the *token holder* MUST both be the `Blockchain.getCaller()`.

> **parameters**  
> `amount`: Number of tokens to burn.

``` java
public static void burn(byte[] amount, byte[] holderData)
 ```

**`operatorBurn` function** <a id="operatorBurn"></a>

Burn the `amount` of tokens on behalf of the address `from`.

The *operator* MUST be `Blockchain.getCaller()`. The value of `from` MAY be `0x0`, then the `from` (*token holder*) used for the burn MUST be `Blockchain.getCaller()` (the `operator`).

*Reminder*: If the *operator* address is not an authorized operator of the `from` address, then the burn process MUST `revert`.

> **parameters**  
> `from`: Token holder whose tokens will be burned (or `0x0` to set `from` to `Blockchain.getCaller()`).  
> `amount`: Number of tokens to burn.  
> `operatorData`: Information attached to the burn by the *operator*.

``` java
public static void operatorBurn(Address tokenHolder, byte[] amount, byte[] holderData, byte[] operatorData)
```

*NOTE*: The *operator* MAY pass any information via `operatorData`. The `operatorData` MUST only be provided by the *operator*.

*NOTE*: `from` and `Blockchain.getCaller()` MAY be the same address. I.e., an address MAY call `operatorBurn` for itself. This call MUST be equivalent to `burn` with the addition that the *operator* MAY specify an explicit value for `operatorData` (which cannot be done with the `burn` function).

### Java Interface

TBD.

## Logic

This standard is based on [AIP-004](https://github.com/aionnetwork/AIP/blob/master/AIP-004/AIP%23004.md) from Aion Network AIP. The following modifications have been made:

- Implementation is now in Java, that is compatible with AVM.  
- `liquidSupply()`, `thaw()`, `freeze()`, `operatorFreeze()` functions removed from the ATS standard.



## Risks & Assumptions


## Test Cases

N/A

## Implementations

[Reference Implementation](https://github.com/jennijuju/Aion-ATS-Token-Contract-Java)

>Note: This implementation is still under development and pending an audit for security check.

## Dependencies


## Copyright

All AIP’s are public domain. Copyright waiver: https://creativecommons.org/publicdomain/zero/1.0/