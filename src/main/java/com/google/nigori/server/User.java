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
package com.google.nigori.server;

import java.security.Principal;
import java.util.Arrays;
import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.nigori.common.NigoriConstants;
import com.google.protobuf.ByteString;

/**
 * @author drt24
 * 
 */
@PersistenceCapable
public class User implements Principal {

  @NotPersistent
  public static final int MIN_USERNAME_LENGTH = 6;
  @NotPersistent
  public static final int MIN_PUBLIC_KEY_LENGTH = NigoriConstants.B_DSA;
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Key key;
  @Persistent
  private final byte[] userName;
  @Persistent
  private final byte[] publicKey;
  @Persistent
  private final Date registrationDate;

  protected User(byte[] userName, byte[] publicKey, Date registrationDate)
      throws IllegalArgumentException {
    assert userName != null;
    assert publicKey != null;
    assert registrationDate != null;
    if (userName.length < MIN_USERNAME_LENGTH) {
      throw new IllegalArgumentException("Username too short, must be at least "
          + MIN_USERNAME_LENGTH + " but was (" + userName.length + ")");
    }
    if (publicKey.length < MIN_PUBLIC_KEY_LENGTH) {
      throw new IllegalArgumentException("Public key too short, must be at least"
          + MIN_PUBLIC_KEY_LENGTH + " but was (" + publicKey.length + ")");
    }
    this.userName = Arrays.copyOf(userName, userName.length);
    this.publicKey = Arrays.copyOf(publicKey, publicKey.length);
    this.registrationDate = registrationDate;
    this.key =
        KeyFactory.createKey(AppEngineDatabase.USERSKEY, User.class.getSimpleName(), getName());
  }

  public static Key keyForUser(byte[] userName) {
    return KeyFactory.createKey(AppEngineDatabase.USERSKEY, User.class.getSimpleName(), ByteString
        .copyFrom(userName).toStringUtf8());
  }

  public Key getKey() {
    return key;
  }

  @Override
  public String getName() {
    return ByteString.copyFrom(userName).toStringUtf8();
  }

  public byte[] getPublicKey() {
    return Arrays.copyOf(publicKey, publicKey.length);
  }

  public Date getRegistrationDate() {
    return registrationDate;
  }

  @Override
  public String toString() {
    return getName();// + " reg: " + registrationDate;//TODO use a sensible date format.
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + Arrays.hashCode(publicKey);
    result = prime * result + Arrays.hashCode(userName);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    User other = (User) obj;
    if (key == null) {
      if (other.key != null)
        return false;
    } else if (!key.equals(other.key))
      return false;
    if (!Arrays.equals(publicKey, other.publicKey))
      return false;
    if (!Arrays.equals(userName, other.userName))
      return false;
    return true;
  }

}
