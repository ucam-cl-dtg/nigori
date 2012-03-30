/*
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
package com.google.nigori.client;

import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.SchnorrSign;

/**
 * @author drt24
 *
 */
public class DummyKeyManager implements KeyManager {

  private final byte[] username;
  private final byte[] password;
  private final byte[] userSecretKey = "secret".getBytes();

  public DummyKeyManager(byte[] servername){
    this(servername, "username".getBytes(), "password".getBytes());
  }
  public DummyKeyManager(byte[] servername, byte[] username, byte[] password) {
    this.username = username;
    this.password = password;
  }
  @Override
  public byte[] getUsername() {
    return username.clone();
  }
  
  @Override
  public byte[] getPassword() {
    return password.clone();
  }
  
  @Override
  public byte[] decrypt(byte[] ciphertext) throws NigoriCryptographyException {
    return ciphertext;
  }
  
  @Override
  public byte[] decrypt(byte[] encryptionKey, byte[] ciphertext) {
    return ciphertext;
  }
  @Override
  public byte[] encrypt(byte[] plaintext) throws NigoriCryptographyException {
    return plaintext;
  }
  
  @Override
  public byte[] encrypt(byte[] key, byte[] plaintext) throws NigoriCryptographyException {
    return plaintext;
  }

  @Override
  public byte[] encryptDeterministically(byte[] plaintext) throws NigoriCryptographyException {
    return plaintext;
  }
  
  @Override
  public byte[] encryptDeterministically(byte[] key, byte[] plaintext) throws
  NigoriCryptographyException {
    return plaintext;
  }  

  @Override
  public SchnorrSign signer() {
    return new SchnorrSign(userSecretKey);
  }

  public byte[] generateSessionKey() {
    return null;
  }
}
