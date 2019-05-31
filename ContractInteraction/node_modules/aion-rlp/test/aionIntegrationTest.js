const assert = require('chai').assert;
const RLP = require('../index.js');
const BN = require('bn.js');
const AionLong = RLP.AionLong;

const assertHex = (input, output) =>
  assert.equal(RLP.encode(input).toString('hex'), output)

describe("Aion flavoured RLP integration", () => {

  it("should properly encode from AionLong", () => {
    const al = new AionLong(new BN(1));
    console.log(AionLong.aionEncodeLong(al).toString('hex'));
  });

  it("should match an equivalent Aion encoded transaction", () => {
    const expectedOutput = "f84801a09aabf5b86690ca4cae3fada8c72b280c4b9302dd8dd5e17bd788f241d7e3045c01a0a035872d6af8639ede962dfe7536b0c150b590f3234a922fb7064cd11971b58e80010101";
    const txArray = new Array();

    txArray.push(new BN(1));
    txArray.push("0x9aabf5b86690ca4cae3fada8c72b280c4b9302dd8dd5e17bd788f241d7e3045c");
    txArray.push(new BN(1));
    txArray.push("0xa035872d6af8639ede962dfe7536b0c150b590f3234a922fb7064cd11971b58e");
    txArray.push(null);
    txArray.push(new AionLong(new BN(1)));
    txArray.push(new AionLong(new BN(1)));
    txArray.push(new BN(1));

    const output = RLP.encode(txArray);
    assert.equal(output.toString('hex'), expectedOutput);
  });

  it("should match an equivalent Aion encoded transaction", () => {
    const expectedOutput = "f85301a09aabf5b86690ca4cae3fada8c72b280c4b9302dd8dd5e17bd788f241d7e3045c01a0a035872d6af8639ede962dfe7536b0c150b590f3234a922fb7064cd11971b58e80831e848088000009184e72a00001";
    const txArray = new Array();

    txArray.push(new BN(1));
    txArray.push("0x9aabf5b86690ca4cae3fada8c72b280c4b9302dd8dd5e17bd788f241d7e3045c");
    txArray.push(new BN(1));
    txArray.push("0xa035872d6af8639ede962dfe7536b0c150b590f3234a922fb7064cd11971b58e");
    txArray.push(null);
    txArray.push(new AionLong(new BN(2000000)));
    txArray.push(new AionLong(new BN("10000000000000")));
    txArray.push(new BN(1));

    const output = RLP.encode(txArray);
    assert.equal(output.toString('hex'), expectedOutput);
  });

  /*

  ENCODE TESTS

  ported from:
  https://github.com/aionnetwork/aion/blob/dev/modRlp/test/org/aion/rlp/RLPSpecTest.java
  https://github.com/aionnetwork/aion/blob/dev/modRlp/test/org/aion/rlp/RLPSpecExtraTest.java

  */

  it("testEncodeEmptyString", () => {
    assertHex("", "80");
  });

  it("testEncodeShortString1", () => {
    assertHex("dog", "83646f67");
  });

  it("testEncodeShortString2", () => {
    assertHex("Lorem ipsum dolor sit amet, consectetur adipisicing eli", "b74c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c69");
  });

  it("testEncodeLongString1", () => {
    assertHex("Lorem ipsum dolor sit amet, consectetur adipisicing elit", "b8384c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c6974")
  });

  // testEncodeLongString2

  it("testEncodeZero", () => {
    assertHex(0, "80");
  });

  it("testEncodeByte1", () => {
    assertHex(1, "01");
  });

  it("testEncodeByte2", () => {
    assertHex(16, "10");
  });

  it("testEncodeByte3", () => {
    assertHex(79, "4f");
  });

  it("testEncodeByte4", () => {
    assertHex(127, "7f");
  });

  it("testEncodeShort1", () => {
    assertHex(128, "8180");
  });

  it("testEncodeShort2", () => {
    assertHex(1000, "8203e8");
  });

  it("testEncodeShort3", () => {
    assertHex(32767, "827fff");
  });

  it("testEncodeShort4", () => {
    assertHex(120, "78");
  });

  it("testEncodeShort5", () => {
    assertHex(30303, "82765f");
  });

  it("testEncodeShort6", () => {
    assertHex(20202, "824eea");
  });

  it("testEncodeInt1", () => {
    assertHex(100000, "830186a0");
  });

  it("testEncodeInt2", () => {
    assertHex(32768, "828000");
  });

  it("testEncodeInt3", () => {
    assertHex(2147483647, "847fffffff");
  });

  it("testEncodeLong1", () => {
    assertHex(2147483648, "8480000000");
  });

  it("testEncodeLong2", () => {
    assertHex(4294967295, "84ffffffff");
  });

  it("testEncodeLong3", () => {
    assertHex(4294967296, "850100000000");
  });

  it("testEncodeLong4", () => {
    assertHex(4295000060, "850100007ffc");
  });

  it("testEncodeBigInt1", () => {
    assertHex(
      new BN("83729609699884896815286331701780722"),
      "8f102030405060708090a0b0c0d0e0f2"
    );
  });

  it("testEncodeBigInt2", () => {
    assertHex(
      new BN("105315505618206987246253880190783558935785933862974822347068935681"),
      "9c0100020003000400050006000700080009000a000b000c000d000e01"
    );
  });

  it("testEncodeBigInt3", () => {
    assertHex(
      new BN("115792089237316195423570985008687907853269984665640564039457584007913129639936"),
      "a1010000000000000000000000000000000000000000000000000000000000000000"
    );
  });

  it("testEncodeBigInt4", () => {
    assertHex(
      new BN("9223372036854775808"),
      "888000000000000000"
    );
  });

  it("testEncodeBigInt5", () => {
    assertHex(
      new BN("8069310865484966410942242126479002031594628301973179630276682693877502501814741239079205017247892462230036091241820433244370452243766979557247714691108723"),
      "b8409a11f84bc53681cc0a2982765fb840d8d60c2580fa795cfc0313efdeba869d2194e79e7cb2b522f782ffa0392cbbab8d1bac301208b137e0de4998334f3bcf73"
    );
  });

  it("testEncodeBigInt6", () => {
    assertHex(
      new BN("9650128800487972697726795438087510101805200020100629942070155319087371611597658887860952245483247188023303607186148645071838189546969115967896446355306572"),
      "b840b840d8d60c2580fa795cfc0313efdeba869d2194e79e7cb2b522f782ffa0392cbbab8d1bac301208b137e0de4998334f3bcf73fa117ef213f87417089feaf84c"
    );
  });

  // testEncodeEmptyList

  it("testEncodeStringList", () => {
    assertHex(["dog", "god", "cat"], "cc83646f6783676f6483636174");
  });

  it("testEncodeMultiList", () => {
    const input = [ "zw", [4], 1 ];
    const output = "c6827a77c10401";
    assertHex(input, output);
    // assert.deepEqual(RLP.decode(output), input)
  });

  it("testEncodeMaxShortList", () => {
    const input = ["asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer"];
    const output = "f784617364668471776572847a78637684617364668471776572847a78637684617364668471776572847a78637684617364668471776572";
    assertHex(input, output);
  });

  it("testEncodeLongList1", () => {
    const input = [
      ["asdf", "qwer", "zxcv"],
      ["asdf", "qwer", "zxcv"],
      ["asdf", "qwer", "zxcv"],
      ["asdf", "qwer", "zxcv"]
    ];
    const output = "f840cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376";
    assertHex(input, output);
  });

  // testEncodeLongList2

  it("testEncodeListOfLists1", () => {
    assertHex([ [ [], [] ], [] ], "c4c2c0c0c0");
  });

  it("testEncodeListofLists2", () => {
    assertHex([ [], [[]], [ [], [[]] ]], "c7c0c1c0c3c0c1c0");
  });

  it("testEncodeDictList", () => {
    const input = [
      ["key1", "val1"],
      ["key2", "val2"],
      ["key3", "val3"],
      ["key4", "val4"],
    ];
    const output = "ecca846b6579318476616c31ca846b6579328476616c32ca846b6579338476616c33ca846b6579348476616c34";
    assertHex(input, output);
  });

  it("testEncodeByteString1", () => {
    assertHex("\u0000", "00");
  });

  it("testEncodeByteString2", () => {
    assertHex("\u0001", "01");
  });

  it("testEncodeByteString3", () => {
    assertHex("\u007F", "7f");
  });



  // Other TESTS

  it("number 10", () => {
    assertHex(10, "0a");
  });

  it("number 100", () => {
    assertHex(100, "64");
  });

  it("letter d", () => {
    assertHex("d", "64");
  });

  it("string cat", () => {
    assertHex("cat", "83636174");
  });

  it("string array", () => {
    assertHex(["cat", "dog"], "c88363617483646f67");
  });

  it("BN 01", () => {
    assertHex(
      new BN("115792089237316195423570985008687907853269984665640564039457584007913129639935"),
      "a0ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
    );
  });

  it("numbers and blank array", () => {
    const input = [1, 2, []]
    const output = "c30102c0"
    assertHex(input, output);
    // console.log(RLP.decode('0x' + output))
    // assert.deepEqual(RLP.decode('0x' + output), input)
  });

  /*

  ported from:
  https://github.com/aionnetwork/aion/blob/master/modRlp/test/org/aion/rlp/RLPTest.java

  */

  const assertAionLong = val => {
    const longNum = new AionLong(new BN(val));
    const encoded = RLP.encode(longNum);
    const decoded = new BN(RLP.decode(encoded));
    assert.equal(val, decoded.toString());
  }

  it('long 01', () => {
    assertAionLong('314159');
  });

  it('long 02', () => {
    assertAionLong(new BN('0xFFFFFFFFF', 'hex').toString());
  });

  it('long 03', () => {
    assertAionLong('1');
  });

  //
  // some others not present in the other tests
  //

  it('long 04', () => {
    assertAionLong('7332199412131513');
  });

  it("testEncodeLong5", () => {
    assertAionLong("72057594037927935");
  });

  it("testEncodeLong6", () => {
    assertAionLong("72057594037927936");
  });

  it("testEncodeLong7", () => {
    assertAionLong("9223372036854775807");
  });

  it('long max', () => {
    assertAionLong('9223372036854775807');
  });

  it('long over max throws', () => {
    assert.throws(() => assertAionLong('9223372036854775808'));
  });
});
