const long = require('../index.js').AionLong;
const assert = require('chai').assert;
const BN = require('bn.js');

describe("checks to ensure instances are behaving correctly", () => {
  it("instantiate without new properly", () => {
    const l = long(new BN(0));
    assert(l instanceof long);
    assert('toArray' in l);
  });

  it("instantiate with new properly", () => {
    const l = new long(new BN(0));
    assert(l instanceof long);
    assert('toArray' in l);
  });

  it("throw on instantiate too large", () => {
    let thrown = false;
    try {
      const l = new long(new BN("9223372036854775808"));
    } catch (e) {
      thrown = true;
    }
    assert(thrown === true);
  });

  it("proper detection of Object.create object", () => {
    const copyLong = Object.create(new long(new BN(0)));
    assert(long.isAionLong(copyLong) === true, "expected correct AionLong detection");
  });

  it("propery detection of duck-typed object", () => {
    const copyLong = Object.create(new long(new BN(0)));
    copyLong.__proto__ = {};
    copyLong.__proto__._aionLong = true;

    assert(copyLong instanceof long === false, "expected instanceof to fail");
    assert(long.isAionLong(copyLong) === true, "expected correct AionLong detection");
  });
});