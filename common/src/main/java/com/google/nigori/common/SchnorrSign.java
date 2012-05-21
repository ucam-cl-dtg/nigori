/*
 * Copyright (C) 2011 Google Inc.
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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Provides Schnorr Signature functionality.
 *
 * @author Alastair Beresford
 */
public class SchnorrSign extends SchnorrVerify {

  private final BigInteger privateKey;
  private final SecureRandom random = new SecureRandom();

  /**
   * Given {@code privateKey} create a object capable of generating Schnorr signatures with it.
   * 
   * @param privateKey an arbitrary-length big-Endian <i>positive</i> integer.
   */
  public SchnorrSign(byte[] privateKey) {
    super(G.modPow(new BigInteger(Util.positiveIntToTwosCompliment(privateKey)), P));
    this.privateKey = new BigInteger(Util.positiveIntToTwosCompliment(privateKey));
  }

  /**
   * Sign {@code message} using the private key given to the constructor.
   * 
   * @param message the message to be signed.
   * @return
   * @throws NoSuchAlgorithmException thrown is {@code DIGEST_ALGORITHM} is not available.
   */
  public SchnorrSignature sign(byte[] message) throws NoSuchAlgorithmException {
    return sign(message, new BigInteger(Q.bitLength() - 1, random));
  }

  protected SchnorrSignature sign(byte[] message, BigInteger randomValue) 
  throws NoSuchAlgorithmException {

    MessageDigest  m = MessageDigest.getInstance(DIGEST_ALGORITHM);

    BigInteger k = randomValue;
    BigInteger r = G.modPow(k, P);

    byte[] rAsBytes = Util.twosComplimentToPositiveInt(r.toByteArray());
    byte[] messageAndR = new byte[message.length + rAsBytes.length];
    System.arraycopy(message, 0, messageAndR, 0, message.length);
    System.arraycopy(rAsBytes, 0, messageAndR, message.length, rAsBytes.length);
    m.update(messageAndR);
    byte[] e = m.digest();

    BigInteger positiveE = new BigInteger(Util.positiveIntToTwosCompliment(e));
    BigInteger s = k.subtract(privateKey.multiply(positiveE)).mod(Q);
    return new SchnorrSignature(Util.twosComplimentToPositiveInt(s.toByteArray()), e, message);
  }

}