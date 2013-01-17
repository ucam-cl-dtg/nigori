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

import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.signers.DSASigner;

/**
 * Checks the validity of a previously generated DSASignature.
 * 
 * @author Daniel Thomas (drt24)
 */
public class DSAVerify {

  static final String DIGEST_ALGORITHM = NigoriConstants.A_KMAC;
  protected final DSASigner signer;

  private final BigInteger publicKey;
  protected DSAPublicKeyParameters publicParams;
  private byte[] publicHash;

  public DSAVerify(byte[] publicKey) throws NoSuchAlgorithmException {
    this(Util.byteToBigInt(publicKey));
  }

  protected DSAVerify(BigInteger publicKey) throws NoSuchAlgorithmException {
    this.publicKey = publicKey;
    this.publicParams = new DSAPublicKeyParameters(this.publicKey, NigoriConstants.DSA_PARAMS);
    this.publicHash = Util.hashKey(getPublicKey());
    signer = new DSASigner();
  }

  /**
   * Provide a big-Endian signed integer representation of the public key.
   * 
   */
  public byte[] getPublicKey() {
    return Util.bigIntToByte(publicKey);
  }

  /**
   * 
   * @return hash using {@link NigoriConstants#A_KEYHASH} of the public key as returned by
   *         {@link #getPublicKey()}
   */
  public byte[] getPublicHash() {
    return publicHash;
  }

  /**
   * Returns {@code true} iff {@code sig} verifies.
   * 
   * @param sig the signature to verify.
   * @throws NoSuchAlgorithmException if {@code DIGEST_ALGORITHM} is not available.
   */
  public boolean verify(DSASignature sig) throws NoSuchAlgorithmException {
    signer.init(false , publicParams);
    MessageDigest hash = MessageDigest.getInstance(DIGEST_ALGORITHM);
    hash.update(sig.getMessage());
    return signer.verifySignature(hash.digest(), Util.byteToBigInt(sig.getR()), Util
        .byteToBigInt(sig.getS()));
  }
}
