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
package com.google.nigori.common;

/**
 * @see "Constants" section of RFC
 * 
 * @author drt24
 *
 */
public final class NigoriConstants {
  /**
   * Static final utility class
   */
  private NigoriConstants(){}

  //This is the constant "user salt" (in non-terminated ascii) written here as bytes
  public final static byte[] USER_SALT = {117, 115, 101, 114, 32, 115, 97, 108, 116};

/**
 * Number of rounds for PBKDF for each key generation
 */
  public static final int N_SALT = 1000;
  public static final int N_USER = N_SALT + 1;
  public static final int N_ENC = N_SALT + 2;
  public static final int N_MAC = N_SALT + 3;
  public static final int N_IV = N_SALT + 4;
  
  // Sizes in bytes
  public static final int B_AES = 16;
  public static final int B_SHA1 = 20;
  public static final int B_SHA256 = 32;
  /**
   * Size in bytes of MAC
   */
  public static final int B_MAC = B_SHA256;
  /**
   * Block size for symmetric encryption
   */
  public static final int B_SYMENC = B_AES;
  /**
   * for Suser, must be at least 10 for FIPS-198 compliance with HMAC_SHA1
   */
  public static final int B_SUSER = B_AES;
  
  /**
   * for Kuser
   */
  public static final int B_DSA = B_AES;
  /**
   * symmetric encryption key size (AES-128) (Kenc)
   */
  public static final int B_KENC = B_AES;
  /**
   * for Kmac
   */
  public static final int B_KMAC = B_AES;
  /**
   * for Kmaster
   */
  public static final int B_KMASTER = B_AES;
  
  public static final String A_HMAC = "HmacSHA256";
  public static final String A_KMAC = "SHA-256";
  public static final String A_SYMENC = "AES";
  public static final String A_SYMENC_CIPHER = A_SYMENC + "/CBC/PKCS5Padding";
}
