/*
 * Copyright (C) 2011 Daniel Thomas (drt24)
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
package com.google.nigori.client;

import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.SchnorrSign;

/**
 * @author drt24
 *
 */
interface KeyManager {
  byte[] getUsername();
  
  byte[] getPassword();
  
  byte[] decrypt(byte[] ciphertext) throws NigoriCryptographyException;
  byte[] decrypt(byte[] encryptionKey, byte[] ciphertext) throws NigoriCryptographyException;

  byte[] encrypt(byte[] plaintext) throws NigoriCryptographyException;  
  byte[] encrypt(byte[] key, byte[] plaintext) throws NigoriCryptographyException;

  byte[] encryptDeterministically(byte[] plaintext) throws NigoriCryptographyException;
  byte[] encryptDeterministically(byte[] key, byte[] plaintext) throws NigoriCryptographyException;

  SchnorrSign signer();

  String getServerName();
}
