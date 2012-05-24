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

import java.security.NoSuchAlgorithmException;

import com.google.nigori.common.DSASign;
import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.NigoriCryptographyException;

/**
 * @author drt24
 *
 */
public class DummyKeyManager implements KeyManager {

  private final byte[] username;
  private final byte[] password;
  private final String serverName;
  private final byte[] userSecretKey = MessageLibrary.toBytes("secret");

  public DummyKeyManager(String serverName){
    this(serverName, MessageLibrary.toBytes("username"), MessageLibrary.toBytes("password"));
  }

  public DummyKeyManager(String serverName, byte[] username, byte[] password) {
    this.username = username;
    this.password = password;
    this.serverName = serverName;
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
  public String getServerName() {
    return serverName;
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
  public DSASign signer() throws NigoriCryptographyException {
    try {
      return new DSASign(userSecretKey);
    } catch (NoSuchAlgorithmException e) {
      throw new NigoriCryptographyException(e);
    }
  }

  public byte[] generateSessionKey() {
    return null;
  }
}
