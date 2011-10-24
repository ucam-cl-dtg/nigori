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

import static org.junit.Assert.assertArrayEquals;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

import com.google.nigori.common.MessageLibrary;

public class KeyManagerTest {
  
  //TODO(beresford): unit tests.
  @Test
  public void getUsernameAndPassword() throws NigoriCryptographyException, UnsupportedEncodingException {
    byte[] serverName = "serverName".getBytes(MessageLibrary.CHARSET);
    byte[] userName = "userName".getBytes(MessageLibrary.CHARSET);
    byte[] password = "password".getBytes(MessageLibrary.CHARSET);
    KeyManager keyManger = new KeyManager(serverName, userName, password);
    assertArrayEquals("Username different",userName,keyManger.getUsername());
    assertArrayEquals("Username different",password,keyManger.getPassword());
  }
}
