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

import java.security.SecureRandom;

/**
 * A data object representation of Nigori's "number used once" to check the freshness of a message.
 * 
 * @author Alastair Beresford
 *
 */
public class Nonce {

  private final static int SINCE_EPOCH_OFFSET = 4;
  private final static int RANDOM_OFFSET = 0;
  private final static SecureRandom randomGenerator = new SecureRandom();
  private final int sinceEpoch;
  private final int random;

  public Nonce() {
    random = randomGenerator.nextInt();
    sinceEpoch = (int) (System.currentTimeMillis() / 1000);
   }

  public Nonce(byte[] token) {
    this.random = Util.bin2int(token, RANDOM_OFFSET);
    this.sinceEpoch = Util.bin2int(token, SINCE_EPOCH_OFFSET);
  }

  public byte[] toToken() {
    byte[] token = new byte[8];
    Util.int2bin(token, RANDOM_OFFSET, random);
    Util.int2bin(token, SINCE_EPOCH_OFFSET, sinceEpoch);
    return token;
  }
  
  public int getSinceEpoch() {
  	return sinceEpoch;
  }
  public boolean isRecent() {
    return sinceEpoch - System.currentTimeMillis() / 1000 < 60 * 60 * 24 * 2;
  }
  
  public int getRandon() {
  	return random;
  }
}