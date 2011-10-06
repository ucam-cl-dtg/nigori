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

import org.apache.commons.codec.binary.Base64;

import com.google.nigori.common.SchnorrSign;

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

  private byte[] username;
  private byte[] password;
  
  private SecureRandom random = new SecureRandom();

  /**
   * Given a ({@code servername}, {@code username}, {@code password}) triple, generate Nigori keys.
   * 
   * @param servername the domain name of the server used to store data.
   * @param username the registered username of the user at {@code servername}.
   * @param password the password of {@code username} at {@code servername}.
   * @throws NigoriCryptographyException
   */
  public KeyManager(byte[] servername, byte[] username, byte[] password) 
  throws NigoriCryptographyException {

  	initialiseKeys(servername, username, password);
  }

  /**
   * Given a {@code servername}, auto-generate a username and password, then generate Nigori keys.
   * 
   * @param servername the domain name of the server used to store data.
   * @throws NigoriCryptographyException
   */
  public KeyManager(byte[] servername) throws NigoriCryptographyException {

  	byte[] username = new byte[24];
  	byte[] password = new byte[24];
  	random.nextBytes(username);
  	random.nextBytes(password);
  	
  	//Ensure the username and password are printable strings without further encoding
  	byte[] encodedUsername = Base64.encodeBase64(username);
  	byte[] encodedPassword = Base64.encodeBase64(password);
  	
  	initialiseKeys(servername, encodedUsername, encodedPassword);
  }
  
  private void initialiseKeys(byte[] servername, byte[] username, byte[] password) throws
  NigoriCryptographyException {
  	
  	this.username = username;
  	this.password = password;

  	byte[] userAndServer = new byte[username.length + servername.length];
  	System.arraycopy(username, 0, userAndServer, 0, username.length);
  	System.arraycopy(servername, 0, userAndServer, username.length, servername.length);
    byte[] salt = pbkdf2(userAndServer, USER_SALT, NSALT, 16);

    this.userSecretKey = pbkdf2(password, salt, NUSER, 16);
    this.encryptionSecretKey = pbkdf2(password, salt, NENC, 16);
    this.macSecretKey = pbkdf2(password, salt, NMAC, 16);
  }
  
  private static byte[] pbkdf2(byte[] password, byte[] salt, int rounds, int outputByteCount) 
  throws NigoriCryptographyException {

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
      throw new NigoriCryptographyException(e);
    } catch (InvalidKeySpecException e) {
      throw new NigoriCryptographyException(e);
    }
  }

  
  private byte[] generateHMAC(byte[] message) throws NigoriCryptographyException {

    try {
      //TODO(drt24): The spec says SHA256, but this is not available on AppEngine - need to bundle library.
      String hmacAlgorithm = "HmacSHA256";
      Mac mac = Mac.getInstance(hmacAlgorithm);
      SecretKey key = new SecretKeySpec(macSecretKey, "SHA-256");
      mac.init(key);
      return mac.doFinal(message);
    } catch(Exception e) {
      throw new NigoriCryptographyException(e);
    }
  }

  public byte[] getUsername() {
  	return username.clone();
  }
  
  public byte[] getPassword() {
  	return password.clone();
  }
  
  public byte[] decrypt(byte[] ciphertext) throws NigoriCryptographyException {
  	return decrypt(encryptionSecretKey, ciphertext);
  }
  
  /**
   * Use this object's keys to decrypt {@code ciphertext} and return plaintext.
   * 
   * This method expects the IV to be stored in the first 16 bytes and a MAC to be stored in the
   * final 16 bytes.
   * 
   * @param ciphertext the message to decrypt.
   * 
   * @throws NigoriCryptographyException if total length of message <48 bytes, if the MAC does 
   * not match the decoded data, or if something goes wrong with AES/CBC/PKCS5Padding inside the 
   * JCE library.
   */
  public byte[] decrypt(byte[] encryptionKey, byte[] ciphertext) throws NigoriCryptographyException {

    byte[] iv = new byte[16]; 
    byte[] mac = new byte[32];
    Cipher cipher;
    try {
      cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    } catch(NoSuchPaddingException e) {
      throw new NigoriCryptographyException(e);
    } catch(NoSuchAlgorithmException e) {
      throw new NigoriCryptographyException(e);
    }
    
    if (ciphertext.length < iv.length + mac.length + cipher.getBlockSize()) {
      throw new NigoriCryptographyException(
          "Ciphertext is too short to be a valid encrypted message.");
    }
      
    byte[] data = new byte[ciphertext.length - iv.length - mac.length];
    System.arraycopy(ciphertext, 0, iv, 0, iv.length);
    System.arraycopy(ciphertext, ciphertext.length-mac.length, mac, 0, mac.length);
    System.arraycopy(ciphertext, iv.length, data, 0, data.length);

    byte[] macCheck = generateHMAC(data);
    if (mac.length != macCheck.length) {
      throw new NigoriCryptographyException(
          String.format("Length mismatch between provided (%d) and received (%d) HMACs.", mac.length, macCheck.length));
    }
    
    for (int i = 0; i < macCheck.length; i++) {
      if (mac[i] != macCheck[i]) {
        throw new NigoriCryptographyException(
            "HMAC of ciphertext does not match expected value");
      }
    }

    try {
      SecretKey key = new SecretKeySpec(encryptionKey,"AES");
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
      return cipher.doFinal(data);
    } catch(InvalidAlgorithmParameterException e) {
      throw new NigoriCryptographyException(e);
    } catch(InvalidKeyException e) {
      throw new NigoriCryptographyException(e);
    } catch(BadPaddingException e) {
      throw new NigoriCryptographyException(e);
    } catch(IllegalBlockSizeException e) {
      throw new NigoriCryptographyException(e);
    }
  }

  /**
   * Encrypted {@code plaintext} with AES using a random IV.
   * 
   * @param plaintext The message to encrypt.
   * 
   * @throws NigoriCryptographyException
   */
  public byte[] encrypt(byte[] plaintext) throws NigoriCryptographyException {
  	return encrypt(encryptionSecretKey, plaintext, true);
  }
  
  public byte[] encrypt(byte[] key, byte[] plaintext) throws NigoriCryptographyException {
    return encrypt(key, plaintext, true);
  }

  public byte[] encryptWithZeroIv(byte[] plaintext) throws NigoriCryptographyException {
  	return encrypt(encryptionSecretKey, plaintext, false);
  }
  
  public byte[] encryptWithZeroIv(byte[] key, byte[] plaintext) throws 
  NigoriCryptographyException {
    return encrypt(key, plaintext, false);
  }  
  
  private byte[] encrypt(byte[] key, byte[] plaintext, boolean randomPadding) 
  throws NigoriCryptographyException {

    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      SecretKey secret = new SecretKeySpec(key, "AES");
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
      throw new NigoriCryptographyException(e);
    } catch(NoSuchAlgorithmException e) {
      throw new NigoriCryptographyException(e);
    } catch(InvalidAlgorithmParameterException e) {
      throw new NigoriCryptographyException(e);
    } catch(InvalidKeyException e) {
      throw new NigoriCryptographyException(e);
    } catch(BadPaddingException e) {
      throw new NigoriCryptographyException(e);
    } catch(IllegalBlockSizeException e) {
      throw new NigoriCryptographyException(e);
    }
  }

  /**
   * Return an instance of {@code SchnorrSign} which is capable of signing user-encrypted data.
   * 
   */
  public SchnorrSign signer() {
    return new SchnorrSign(userSecretKey);
  }

  public byte[] generateSessionKey() {
  	byte[] key = new byte[16];
  	random.nextBytes(key);
  	return key;
  }
  
  //TODO(beresford): unit tests.
  public static void main(String[] args) throws Exception {
  }
}