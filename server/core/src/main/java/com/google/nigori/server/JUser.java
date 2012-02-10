/*
 * Copyright (C) 2011 Daniel R. Thomas (drt24)
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
package com.google.nigori.server;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;

import com.google.nigori.common.MessageLibrary;

/**
 * Plain java implementation of User
 * 
 * @author drt24
 * 
 */
public class JUser implements User, Serializable {

  private static final long serialVersionUID = 1L;

  private final byte[] publicKey;
  private final Date registrationDate;

  public JUser(byte[] publicKey, Date registrationDate) {
    this.publicKey = Arrays.copyOf(publicKey, publicKey.length);
    this.registrationDate = registrationDate;
  }

  @Override
  public String getName() {
    try {
      return new String(Base64.encodeBase64(getPublicKey()),MessageLibrary.CHARSET);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] getPublicKey() {
    return Arrays.copyOf(publicKey, publicKey.length);
  }

  @Override
  public Date getRegistrationDate() {
    return registrationDate;
  }

  public static class Factory implements UserFactory {

    private static final Factory instance = new Factory();
    public static Factory getInstance() {
      return instance;
    }
    @Override
    public User getUser(byte[] publicKey, Date registrationDate) {
      return new JUser(publicKey, registrationDate);
    }
    
  }
}
