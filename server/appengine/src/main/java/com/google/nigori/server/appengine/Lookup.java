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
package com.google.nigori.server.appengine;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.apache.commons.codec.binary.Base64;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * @author drt24
 * 
 */
@PersistenceCapable
public class Lookup {
  @PrimaryKey
  @Persistent
  private Key key;

  @Persistent
  private byte[] index;

  public Lookup(Key key, byte[] index) {
    this.key = key;
    this.index = index;
  }

  public Key getKey() {
    return key;
  }

  public static Key makeKey(AEUser user, byte[] key) {
    return KeyFactory.createKey(KeyFactory.createKey(user.getKey(), AppEngineDatabase.STORE,
        AppEngineDatabase.STORE), Lookup.class.getSimpleName(), Base64.encodeBase64String(key));
  }

  public byte[] getIndex() {
    return index;
  }
}
