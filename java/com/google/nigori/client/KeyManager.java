/*
 * Copyright (C) 2011 Google Inc.
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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Manages the set of keys derived from a given (servername, username and password) triple.
 *
 * @author Alastair Beresford
 */
public class KeyManager {

  //This is the constant "user salt" (in non-terminated ascii) written here as bytes
  private final static byte[] USER_SALT = {117, 115, 101, 114, 32, 115, 97, 108, 116};

  //Number of rounds for PBKDF for each key generation
  private final static int NSALT = 1000;
  private final static int NUSER = NSALT + 1;
  private final static int NENC = NSALT + 2;
  private final static int NMAC = NSALT + 3;

  private byte[] userSecretKey;
  private byte[] encryptionSecretKey;
  private byte[] macSecretKey;

  private SecureRandom random = new SecureRandom();

  /**
   * Given a ({@code servername}, {@code username}, {@code password}) triple, generate Nigori keys.
   * 
   * @param servername the domain name of the server used to store data.
   * @param username the registered username of the user at {@code servername}.
   * @param password the password of {@code username} at {@code servername}.
   * @throws KeyManagerCryptographyException
   */
  public KeyManager(byte[] servername, byte[] username, byte[] password) 
  throws KeyManagerCryptographyException {

    byte[] userAndServer = Util.concatAndPrefix(new byte[][]{username,servername});
    byte[] salt = pbkdf2(userAndServer, USER_SALT, NSALT, 16);

    userSecretKey = pbkdf2(password, salt, NUSER, 16);
    encryptionSecretKey = pbkdf2(password, salt, NENC, 16);
    macSecretKey = pbkdf2(password, salt, NMAC, 16);
  }

  private static byte[] pbkdf2(byte[] password, byte[] salt, int rounds, int outputByteCount) 
  throws KeyManagerCryptographyException {

    //Standard Java PBKDF2 takes the lower 8 bits of each element of a char array as input.
    //Therefore, rewrite byte array into char array, preserving lower 8-bit pattern
    char[] charPassword = new char[password.length];
    for (int i = 0; i < charPassword.length; i++) {
       if (password[i] >= 0) {
        charPassword[i] = (char) password[i];
      } else {
        charPassword[i] = (char) (password[i] + 256); //cope with signed -> unsigned
      }
    }
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      KeySpec spec = new PBEKeySpec(charPassword, salt, rounds, 8*outputByteCount);
      return factory.generateSecret(spec).getEncoded();     
    } catch (NoSuchAlgorithmException e) {
      throw new KeyManagerCryptographyException(e);
    } catch (InvalidKeySpecException e) {
      throw new KeyManagerCryptographyException(e);
    }
  }

  
  private byte[] generateHMAC(byte[] message) throws KeyManagerCryptographyException {

    try {
      //TODO(beresford): The spec says SHA256, but using MD5 as this is available on AppEngine.
      String hmacAlgorithm = "HmacMD5";
      Mac mac = Mac.getInstance(hmacAlgorithm);
      SecretKey key = new SecretKeySpec(macSecretKey, "MD5");
      mac.init(key);
      return mac.doFinal(message);
    } catch(Exception e) {
      throw new KeyManagerCryptographyException(e);
    }
  }

  /**
   * Use this object's keys to decrypt {@code ciphertext} and return plaintext.
   * 
   * This method expects the IV to be stored in the first 16 bytes and a MAC to be stored in the
   * final 16 bytes.
   * 
   * @param ciphertext the message to decrypt.
   * 
   * @throws KeyManagerCryptographyException if total length of message <48 bytes, if the MAC does 
   * not match the decoded data, or if something goes wrong with AES/CBC/PKCS5Padding inside the 
   * JCE library.
   */
  public byte[] decrypt(byte[] ciphertext) throws KeyManagerCryptographyException {

    byte[] iv = new byte[16]; 
    byte[] mac = new byte[16];
    Cipher cipher;
    try {
      cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    } catch(NoSuchPaddingException e) {
      throw new KeyManagerCryptographyException(e);
    } catch(NoSuchAlgorithmException e) {
      throw new KeyManagerCryptographyException(e);
    }
    
    if (ciphertext.length < iv.length + mac.length + cipher.getBlockSize()) {
      throw new KeyManagerCryptographyException(
          "Ciphertext is too short to be a valid encrypted message.");
    }
      
    byte[] data = new byte[ciphertext.length - iv.length - mac.length];
    System.arraycopy(ciphertext, 0, iv, 0, iv.length);
    System.arraycopy(ciphertext, ciphertext.length-mac.length, mac, 0, mac.length);
    System.arraycopy(ciphertext, iv.length, data, 0, data.length);

    byte[] macCheck = generateHMAC(data);
    if (mac.length != macCheck.length) {
      throw new KeyManagerCryptographyException(
          "Length mismatch between provided and received HMACs.");
    }
    
    for (int i = 0; i < macCheck.length; i++) {
      if (mac[i] != macCheck[i]) {
        throw new KeyManagerCryptographyException(
            "HMAC of ciphertext does not match expected value");
      }
    }

    try {
      SecretKey key = new SecretKeySpec(encryptionSecretKey,"AES");
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
      return cipher.doFinal(data);
    } catch(InvalidAlgorithmParameterException e) {
      throw new KeyManagerCryptographyException(e);
    } catch(InvalidKeyException e) {
      throw new KeyManagerCryptographyException(e);
    } catch(BadPaddingException e) {
      throw new KeyManagerCryptographyException(e);
    } catch(IllegalBlockSizeException e) {
      throw new KeyManagerCryptographyException(e);
    }
  }

  /**
   * Encrypt {@code plaintext} using AES with a fixed IV of all zero bytes.
   * 
   * @param plaintext The message to encrypt.
   * @return
   * @throws KeyManagerCryptographyException
   */
  //TODO(beresford): The IV needs to be removed and there is no HMAC. Normalise with decrypt()?
  public byte[] permute(byte[] plaintext) throws KeyManagerCryptographyException {
    byte[] crypt = encrypt(plaintext, false);
    byte[] cryptNoIV = new byte[crypt.length - 16];
    System.arraycopy(crypt, 16, cryptNoIV, 0, cryptNoIV.length);
    return cryptNoIV;
  }

  /**
   * Decrypt {@code ciphertext} using AES with a fixed IV of all zero bytes.
   * 
   * @param ciphertext The message to decrypt.
   * 
   * @throws KeyManagerCryptographyException
   */
  //TODO(beresford): This method can be removed (and decrypt() used) if normalised.
  public byte[] dePermute(byte[] ciphertext) throws KeyManagerCryptographyException {
    byte[] ciphertextWithIV = new byte[ciphertext.length + 16];
    System.arraycopy(ciphertext, 0, ciphertextWithIV, 16, ciphertext.length);
    return decrypt(ciphertextWithIV);
  }

  /**
   * Encrypted {@code plaintext} with AES using a random IV.
   * 
   * @param plaintext The message to encrypt.
   * 
   * @throws KeyManagerCryptographyException
   */
  public byte[] encrypt(byte[] plaintext) throws KeyManagerCryptographyException {
    return encrypt(plaintext, true);
  }

  private byte[] encrypt(byte[] plaintext, boolean randomPadding) 
  throws KeyManagerCryptographyException {

    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      SecretKey secret = new SecretKeySpec(encryptionSecretKey, "AES");
      byte[] iv = new byte[16];
      if (randomPadding) {
        random.nextBytes(iv); 
      }
      IvParameterSpec ips = new IvParameterSpec(iv);
      cipher.init(Cipher.ENCRYPT_MODE, secret, ips);
      byte[] data = cipher.doFinal(plaintext);
      byte[] mac = generateHMAC(data);

      byte[] ciphertext = new byte[iv.length + data.length + mac.length];
      System.arraycopy(iv, 0, ciphertext, 0, iv.length);
      System.arraycopy(data, 0, ciphertext, iv.length, data.length);
      System.arraycopy(mac, 0, ciphertext, iv.length + data.length, mac.length);

      return ciphertext;
    } catch(NoSuchPaddingException e) {
      throw new KeyManagerCryptographyException(e);
    } catch(NoSuchAlgorithmException e) {
      throw new KeyManagerCryptographyException(e);
    } catch(InvalidAlgorithmParameterException e) {
      throw new KeyManagerCryptographyException(e);
    } catch(InvalidKeyException e) {
      throw new KeyManagerCryptographyException(e);
    } catch(BadPaddingException e) {
      throw new KeyManagerCryptographyException(e);
    } catch(IllegalBlockSizeException e) {
      throw new KeyManagerCryptographyException(e);
    }
  }

  /**
   * Return an instance of {@code SchnorrSign} which is capable of signing user-encrypted data.
   * 
   */
  public SchnorrSign signer() {
    return new SchnorrSign(userSecretKey);
  }

  //TODO(beresford): unit tests.
  public static void main(String[] args) throws Exception {
  }
}