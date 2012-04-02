/*
 * Copyright (C) 2012 Daniel Thomas (drt24)
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

import java.security.SecureRandom;
import java.util.Random;

/**
 * Securely generate impossible to remember passwords using {@link SecureRandom} these are not
 * intended to ever be displayed to the user or input from them, but are typeable and so can be if
 * necessary.
 * 
 * @author drt24
 * 
 */
public class PasswordGenerator {

  private final Random random = new SecureRandom();

  private static final char[] VALID_CHARS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890\\,./<>?;:'@#~[]{}-=+`|!\"$%^&*()"
          .toCharArray();

  /**
   * 
   * @return a sufficiently long random String
   */
  public byte[] generate() {
    byte[] password = new byte[30];// 192 bits of entropy
    for (int i = 0; i < password.length; ++i) {
      int index = random.nextInt(VALID_CHARS.length);
      password[i] = (byte) VALID_CHARS[index];
    }
    return password;
  }
}
