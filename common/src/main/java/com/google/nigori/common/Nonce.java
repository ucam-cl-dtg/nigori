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

  private void int2bin(byte[] out, int offset, int i) {
    out[offset + 0] = (byte)(i >> 24); 
    out[offset + 1] = (byte)(i >> 16 & 0xff);
    out[offset + 2] = (byte)(i >> 8 & 0xff); 
    out[offset + 3] = (byte)(i & 0xff);		
  }
  
  private static int bin2int(byte[] array, int offset) {
    return ((array[offset + 0] & 0xff) << 24)
    + ((array[offset + 1] & 0xff) << 16)
    + ((array[offset + 2] & 0xff) << 8) 
    + (array[offset + 3] & 0xff);
  }

  public Nonce() {
    random = randomGenerator.nextInt();
    sinceEpoch = (int) (System.currentTimeMillis() / 1000);
   }

  public Nonce(byte[] token) {
  	this.random = bin2int(token, RANDOM_OFFSET);
  	this.sinceEpoch = bin2int(token, SINCE_EPOCH_OFFSET);
  }

  public byte[] toToken() {
  	byte[] token = new byte[8];
  	int2bin(token, RANDOM_OFFSET, random);
  	int2bin(token, SINCE_EPOCH_OFFSET, sinceEpoch);
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