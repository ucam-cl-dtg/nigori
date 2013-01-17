/*
 * Copyright (C) 2012 Daniel Thomas (drt24)
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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;

/**
 * Provides DSA Signature functionality.
 * 
 * @author Daniel Thomas (drt24)
 */
public class DSASign extends DSAVerify {

  private final BigInteger privateKey;
  protected DSAPrivateKeyParameters privateParams;

  /**
   * Given {@code privateKey} create a object capable of generating DSA signatures with it.
   * 
   * @param privateKey an arbitrary-length big-Endian <i>positive</i> integer.
   * @throws NoSuchAlgorithmException
   */
  public DSASign(byte[] privateKey) throws NoSuchAlgorithmException {
    super(NigoriConstants.DSA_G.modPow(Util.byteToBigInt(privateKey), NigoriConstants.DSA_P));
    this.privateKey = Util.byteToBigInt(privateKey);
    this.privateParams = new DSAPrivateKeyParameters(this.privateKey, NigoriConstants.DSA_PARAMS);
  }

  /**
   * Sign {@code message} using the private key given to the constructor.
   * 
   * @param message the message to be signed.
   * @return
   * @throws NoSuchAlgorithmException thrown is {@code DIGEST_ALGORITHM} is not available.
   */
  public DSASignature sign(byte[] message) throws NoSuchAlgorithmException {
    signer.init(true, privateParams);
    MessageDigest hash = MessageDigest.getInstance(DIGEST_ALGORITHM);
    hash.update(message);
    BigInteger[] sig = signer.generateSignature(hash.digest());
    return new DSASignature(Util.bigIntToByte(sig[0]), Util.bigIntToByte(sig[1]), message);
  }

}