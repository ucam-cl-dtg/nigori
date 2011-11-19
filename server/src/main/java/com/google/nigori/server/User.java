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

import org.apache.commons.codec.binary.Hex;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.ShortBlob;
import com.google.nigori.common.NigoriConstants;
import com.google.protobuf.ByteString;

/**
 * @author drt24
 * 
 */
@PersistenceCapable
public class User implements Principal {

  @NotPersistent
  protected static final Key USERSKEY = KeyFactory.createKey("users", "users");

  @NotPersistent
  public static final int MIN_USERNAME_LENGTH = 6;
  @NotPersistent
  public static final int MIN_PUBLIC_KEY_LENGTH = NigoriConstants.B_DSA;

  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Key key;
  @Persistent
  private ShortBlob publicKey;
  @Persistent
  private Date registrationDate;

  public static Key keyForUser(byte[] publicKey) {
    return KeyFactory.createKey(USERSKEY, User.class.getSimpleName(), ByteString
        .copyFrom(publicKey).toStringUtf8());
  }

  protected User(byte[] publicKey, Date registrationDate)
      throws IllegalArgumentException {
    assert publicKey != null;
    assert registrationDate != null;
    if (publicKey.length < MIN_PUBLIC_KEY_LENGTH) {
      throw new IllegalArgumentException("Public key too short, must be at least"
          + MIN_PUBLIC_KEY_LENGTH + " but was (" + publicKey.length + ")");
    }
    this.publicKey = new ShortBlob(publicKey);
    this.registrationDate = registrationDate;
    this.key = keyForUser(publicKey);
  }

  public Key getKey() {
    return key;
  }

  @Override
  public String getName() {
    return Hex.encodeHexString(getPublicKey());
  }

  public byte[] getPublicKey() {
    byte[] pkB = publicKey.getBytes();
    return Arrays.copyOf(pkB, pkB.length);
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
    result = prime * result + publicKey.hashCode();
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
    if (!publicKey.equals(other.publicKey))
      return false;
    return true;
  }

}
