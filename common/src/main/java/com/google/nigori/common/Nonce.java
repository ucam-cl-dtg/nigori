/*
 * Copyright (C) 2011 Google Inc.
 * Copyright (C) 2011 Alastair R. Beresford
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
package com.google.nigori.common;

import java.io.Serializable;
import java.security.SecureRandom;

/**
 * A data object representation of Nigori's "number used once" to check the freshness of a message.
 * 
 * @author Alastair Beresford
 *
 */
public class Nonce implements Serializable {

  private static final long serialVersionUID = 1L;
  private final static int SINCE_EPOCH_OFFSET = 4;
  private final static int RANDOM_OFFSET = 0;
  private final static SecureRandom randomGenerator = new SecureRandom();
  private static final int TWO_DAYS = 60 * 60 * 24 * 2;
  /**
   * Time into the future which the sinceEpoch is allowed to be - to allow for clock skew.
   */
  private static final int SKEW_ALLOWANCE = 60 * 60;

  private final int sinceEpoch;
  private final int random;

  public Nonce() {
    random = randomGenerator.nextInt();
    sinceEpoch = (int) (System.currentTimeMillis() / 1000);
  }

  /**
   * Only for use in testing where constructing nonces for times other than now is required.
   * @param sinceEpoch
   */
  protected Nonce(int sinceEpoch) {
    random = randomGenerator.nextInt();
    this.sinceEpoch = sinceEpoch;
  }

  public Nonce(byte[] token) {
    this.random = Util.bin2int(token, RANDOM_OFFSET);
    this.sinceEpoch = Util.bin2int(token, SINCE_EPOCH_OFFSET);
  }

  public byte[] toToken() {
    byte[] token = new byte[Util.INT*2];
    Util.int2bin(token, RANDOM_OFFSET, random);
    Util.int2bin(token, SINCE_EPOCH_OFFSET, sinceEpoch);
    return token;
  }
  
  public int getSinceEpoch() {
  	return sinceEpoch;
  }
  public boolean isRecent() {
    return isRecent(sinceEpoch);
  }

  public static boolean isRecent(int sinceEpoch) {
    int currentTime = (int) (System.currentTimeMillis() / 1000);
    int timeAgo = currentTime - sinceEpoch;
    // Less than two days into the past and SKEW_ALLOWANCE into the future
    return timeAgo < TWO_DAYS && timeAgo > -SKEW_ALLOWANCE;
  }
  
  public int getRandon() {
  	return random;
  }
}