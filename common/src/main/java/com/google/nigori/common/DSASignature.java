/*
 * Copyright (C) 2011 Google Inc.
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

/**
 * A data object used to store a message and its associated DSA signature.
 * 
 * @author Alastair Beresford
 */
public class DSASignature {

  private final byte[] s;
  private final byte[] r;
  private final byte[] message;

  /**
   * A data object representing a DSA Signature of {@code message}.
   * 
   * Note: {@code s} and {@code r} represent <i>positive</i> big-Endian integers. Use
   * {@link Util#bigIntToByte(java.math.BigInteger)} and {@link Util#byteToBigInt(byte[])} to
   * convert between BigInteger and byte[].
   * 
   * @param s the <i>s</i> parameter of the DSA signature.
   * @param r the <i>e</i> parameter of the DSA signature.
   * @param message the message which has been signed.
   */
  public DSASignature(byte[] s, byte[] r, byte[] message) {
    this.s = s;
    this.r = r;
    this.message = message;
  }

  public byte[] getR() {
    return r;
  }

  public byte[] getS() {
    return s;
  }

  public byte[] getMessage() {
    return message;
  }
}