/*
 * Copyright (C) 2011 Daniel Thomas (drt24)
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
package com.google.nigori.client;

import org.junit.Test;

import com.google.nigori.common.NigoriCryptographyException;

/**
 * 
 * Tests that the DummyKeyManager does not pass the KeyManager tests so that it can't get used in
 * deployment.
 * 
 * @author drt24
 * 
 */
public class DummyKeyManagerTest extends AbstractKeyManagerTest {

  @Override
  protected KeyManager getKeyManager(String serverName, byte[] userName, byte[] password)
      throws NigoriCryptographyException {
    return new DummyKeyManager(serverName, userName, password);
  }

  @Override
  protected KeyManager getKeyManager(String serverName) throws NigoriCryptographyException {
    return new DummyKeyManager(serverName);
  }

  @Override
  @Test(expected = AssertionError.class)
  public void encryptNotIdentity() throws NigoriCryptographyException {
    super.encryptNotIdentity();
  }

  @Override
  @Test(expected = AssertionError.class)
  public void encryptSameValueGivesDifferentAnswers() throws NigoriCryptographyException {
    super.encryptSameValueGivesDifferentAnswers();
  }
}
