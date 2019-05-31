## SYNOPSIS

## INSTALL

`npm install aion-rlp`   

install with `-g` if you want to use the cli.

## USAGE

~~~~javascript
var RLP = require('aion-rlp');
var assert = require('assert');

var nestedList = [ [], [[]], [ [], [[]] ] ];
var encoded = RLP.encode(nestedList);
var decoded = RLP.decode(encoded);
assert.deepEqual(nestedList, decoded);
~~~~

## API

`rlp.encode(plain)` - RLP encodes an `Array`, `Buffer` or `String` and returns a `Buffer`.

`rlp.decode(encoded, [skipRemainderCheck=false])` - Decodes an RLP encoded `Buffer`, `Array` or `String` and returns a `Buffer` or an `Array` of `Buffers`. If `skipRemainderCheck` is enabled, `rlp` will just decode the first rlp sequence in the buffer. By default, it would throw an error if there are more bytes in Buffer than used by rlp sequence.

The difference between ``aion-rlp`` and ``rlp`` is the alternative encoding of longs, therefore `rlp.encode(plain)` accepts an extra type `AionLong`, in addition to the base supported types.

## CLI

`rlp decode <hex string>`   
`rlp encode <json String>`  

## TESTS

Test uses mocha. To run `npm test`

## CODE COVERAGE

Install dev dependencies
`npm install`

Run
`npm run coverage`

The results are at
`coverage/lcov-report/index.html`
