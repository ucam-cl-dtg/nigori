/*
 * Copyright (C) 2012 Daniel R. Thomas (drt24).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.nigori.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.junit.Test;

public class DSATest {

  @Test
  public void signVerifies() throws NoSuchAlgorithmException {
    // Can have bugs which depend on particular bit values (such as sign bits) so iterate to reduce
    // probability of false positive
    for (int i = 0; i < 4; ++i) {
      Random random = new Random();
      byte[] privateKey = new byte[NigoriConstants.B_DSA];
      random.nextBytes(privateKey);
      DSASign signer = new DSASign(privateKey);
      DSASignature sig = signer.sign(MessageLibrary.toBytes("message"));
      DSAVerify verifier = new DSAVerify(signer.getPublicKey());
      assertTrue("Did not verify on iteration: " + i, verifier.verify(sig));
    }
  }

  @Test
  public void paramsPrime() {
    assertTrue(NigoriConstants.DSA_P.isProbablePrime(128));
    assertTrue(NigoriConstants.DSA_Q.isProbablePrime(256));
  }

  @Test
  public void paramGValid() {
    assertTrue(NigoriConstants.DSA_G.compareTo(BigInteger.valueOf(2)) >= 0);
    assertTrue(NigoriConstants.DSA_G.modPow(NigoriConstants.DSA_Q, NigoriConstants.DSA_P)
        .compareTo(BigInteger.valueOf(1)) == 0);
  }

  @Test
  public void paramsSize() {
    assertEquals(NigoriConstants.B_DSA * 8, NigoriConstants.DSA_P.bitLength());
    assertEquals(NigoriConstants.B_SHA256 * 8, NigoriConstants.DSA_Q.bitLength());
    assertTrue(NigoriConstants.B_DSA * 8 - 1 <= NigoriConstants.DSA_G.bitLength());
    assertTrue(NigoriConstants.DSA_G.bitLength() <= NigoriConstants.B_DSA * 8);
  }
}
