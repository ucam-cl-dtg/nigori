/*
 * Copyright (C) 2011 Alastair R. Beresford (beresford)
 * Copyright (C) 2011 Daniel Thomas (drt24)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.nigori.common;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

/**
 * @author drt24
 *
 */
public class SchnorrSignTest {


  private static class SchnorrSignTestCase{
    String password;
    String message;
    BigInteger random;
    BigInteger e;
    BigInteger s;

    SchnorrSignTestCase(String password, String message, String random, String e, String s) {
      this.password = password;
      this.message = message;
      this.random = new BigInteger(random, 16);
      this.e = new BigInteger(e, 16);
      this.s = new BigInteger(s, 16);
    }

    @Override
    public String toString() {
      return "{\n " + password + ",\n " + message + ",\n " 
      + random + ",\n " + e + ",\n " + s + "\n}";
    }
  }

  //TODO(beresford): Add some more test cases.
  private static SchnorrSignTestCase[] test = {
    new SchnorrSignTestCase("password", "message", "1",
        "dd8291f5bbef43c83d35acd6d2592648f0fed44ed655f5b2b346cb1b35c7fac4",
        "72d740a9bba48c30e6bd56a58affd5361e787c0c"
    )};

  @Test
  public void main() throws Exception {

    for (SchnorrSignTestCase t : test) {
      SchnorrSign signer = new SchnorrSign(t.password.getBytes());
      SchnorrSignature sign = signer.sign(t.message.getBytes(), t.random);
      BigInteger e = new BigInteger(SchnorrVerify.positiveIntToTwosCompliment(sign.getE()));
      BigInteger s = new BigInteger(SchnorrVerify.positiveIntToTwosCompliment(sign.getS()));
      assertTrue("Error when testing, t:\n"+t+"\nsign.e: "+e+"\nsign.s: "+s,
          t.e.equals(e) && t.s.equals(s));
    }     
  }
}
