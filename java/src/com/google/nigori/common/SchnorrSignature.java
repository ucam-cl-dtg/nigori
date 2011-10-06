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

/**
 * A data object used to store a message and its associated Schnorr signature.
 *
 * @author Alastair Beresford
 */
public class SchnorrSignature {

  private final byte[] s;
  private final byte[] e;
  private final byte[] message;

  /**
   * A data object representing a Schnorr Signature of {@code message}.
   * 
   * Note: {@code s} and {@code e} represent <i>positive</i> big-Endian integers. In particular, 
   * any zero byte padding inserted by java.math.BigInteger should be removed before creating
   * one of these objects.
   * 
   * @param s the <i>s</i> parameter of the Schnorr signature.
   * @param e the <i>e</i> parameter of the Schnorr signature.
   * @param message the message which has been signed.
   */
  public SchnorrSignature(byte[] s, byte[] e, byte[] message) {
    this.s = s;
    this.e = e;
    this.message = message;
  }

  public byte[] getE() {
    return e;
  }

  public byte[] getS() {
    return s;
  }

  public byte[] getMessage() {
    return message;
  }
}