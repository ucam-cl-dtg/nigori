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

import org.apache.commons.codec.binary.Base64;

/**
 * Securely generate passwords using {@link SecureRandom}
 * @author drt24
 * 
 */
public class PasswordGenerator {

  private final Random random = new SecureRandom();

  /**
   * 
   * @return a sufficiently long Base64 encoded random String
   */
  public byte[] generate() {
    byte[] password = new byte[24];
    random.nextBytes(password);
    // Ensure the username and password are printable strings without further encoding
    return Base64.encodeBase64(password);
  }
}
