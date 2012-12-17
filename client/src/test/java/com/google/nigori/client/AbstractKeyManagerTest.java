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

import static com.google.nigori.common.MessageLibrary.toBytes;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.nigori.common.NigoriCryptographyException;

public abstract class AbstractKeyManagerTest {

  protected abstract KeyManager getKeyManager(String serverName, byte[] userName, byte[] password) throws NigoriCryptographyException;
  protected abstract KeyManager getKeyManager(String serverName) throws NigoriCryptographyException;

  private final String serverName = "serverName";

  @Test
  public void getUsernameAndPassword() throws NigoriCryptographyException {
    
    byte[] userName = toBytes("userName");
    byte[] password = toBytes("password");
    KeyManager keyManger = getKeyManager(serverName, userName, password);
    assertArrayEquals("Username different",userName,keyManger.getUsername());
    assertArrayEquals("Username different",password,keyManger.getPassword());
  }
  @Test
  public void decryptReversesEncrypt() throws NigoriCryptographyException {
    KeyManager keyManager = getKeyManager(serverName);
    byte[] plaintext = toBytes("plaintext");
    assertArrayEquals(plaintext,keyManager.decrypt(keyManager.encrypt(plaintext)));
  }
  @Test
  public void encryptNotIdentity() throws NigoriCryptographyException{
    KeyManager keyManager = getKeyManager(serverName);
    byte[] plaintext = toBytes("plaintext");
    assertThat(plaintext, not(equalTo(keyManager.encrypt(plaintext))));
  }
  @Test
  public void encryptSameValueGivesDifferentAnswers() throws NigoriCryptographyException{
    KeyManager keyManager = getKeyManager(serverName);
    byte[] plaintext = toBytes("plaintext");
    assertThat(keyManager.encrypt(plaintext), not(equalTo(keyManager.encrypt(plaintext))));
  }
  @Test
  public void encryptDeterministicallySameValueGivesSameAnswer() throws NigoriCryptographyException{
    KeyManager keyManager = getKeyManager(serverName);
    byte[] plaintext = toBytes("plaintext");
    assertThat(keyManager.encryptDeterministically(plaintext), equalTo(keyManager.encryptDeterministically(plaintext)));
  }
}
