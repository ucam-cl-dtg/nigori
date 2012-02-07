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
package com.google.nigori.server.appengine;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.nigori.common.Nonce;

/**
 * @author drt24
 * 
 */
@PersistenceCapable
public class AENonce {
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Key key;
  @Persistent
  private int nonce;
  @Persistent
  private int sinceEpoch;

  public AENonce(Nonce nonce, byte[] publicKey) {
    this.nonce = nonce.getRandon();
    this.sinceEpoch = nonce.getSinceEpoch();
    this.key =
        KeyFactory.createKey(AEUser.keyForUser(publicKey), AENonce.class.getSimpleName(),
            nonce.getRandon());
  }

  public Key getKey() {
    return key;
  }

  protected int getNonce() {
    return nonce;
  }

  protected int getSinceEpoch() {
    return sinceEpoch;
  }
}
