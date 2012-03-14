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
import java.util.Arrays;

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

import com.google.nigori.common.NigoriConstants;
import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.SchnorrSign;

/**
 * Manages the set of keys derived from a given (servername, username and password) triple.
 *
 * @author Alastair Beresford
 */
public class RealKeyManager implements KeyManager {

  /**
   * For authenticating the user
   */
  private byte[] userSecretKey;
  /**
   * For encrypting the data
   */
  private byte[] encryptionSecretKey;
  /**
   * For HMACs on ciphertexts
   */
  private byte[] macSecretKey;
  /**
   * For HMACs on plaintexts, used for initialisation vectors
   */
  private byte[] ivSecretKey;

  private byte[] username;
  private byte[] password;
  
  private final SecureRandom random = new SecureRandom();
  private final PasswordGenerator pwgen = new PasswordGenerator();

  /**
   * Given a ({@code servername}, {@code username}, {@code password}) triple, generate Nigori keys.
   * 
   * @param servername the domain name of the server used to store data.
   * @param username the registered username of the user at {@code servername}.
   * @param password the password of {@code username} at {@code servername}.
   * @throws NigoriCryptographyException
   */
  public RealKeyManager(byte[] servername, byte[] username, byte[] password) 
  throws NigoriCryptographyException {

  	initialiseKeys(servername, username, password);
  }

  /**
   * Given a {@code servername}, auto-generate a username and password, then generate Nigori keys.
   * 
   * @param servername the domain name of the server used to store data.
   * @throws NigoriCryptographyException
   */
  public RealKeyManager(byte[] servername) throws NigoriCryptographyException {
  	initialiseKeys(servername, pwgen.generate(), pwgen.generate());
  }
  
  private void initialiseKeys(byte[] servername, byte[] username, byte[] password) throws
  NigoriCryptographyException {
  	
  	this.username = username;
  	this.password = password;

  	byte[] userAndServer = new byte[username.length + servername.length];
  	System.arraycopy(username, 0, userAndServer, 0, username.length);
  	System.arraycopy(servername, 0, userAndServer, username.length, servername.length);
    byte[] salt = pbkdf2(userAndServer, NigoriConstants.USER_SALT, NigoriConstants.N_SALT, NigoriConstants.B_SUSER);

    this.userSecretKey = pbkdf2(password, salt, NigoriConstants.N_USER, NigoriConstants.B_DSA);
    this.encryptionSecretKey = pbkdf2(password, salt, NigoriConstants.N_ENC, NigoriConstants.B_KENC);
    this.macSecretKey = pbkdf2(password, salt, NigoriConstants.N_MAC, NigoriConstants.B_KMAC);
    this.ivSecretKey = pbkdf2(password, salt, NigoriConstants.N_IV, NigoriConstants.B_KMAC);
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

  
  private byte[] generateCipherHMAC(byte[] message) throws NigoriCryptographyException {
    return generateHMAC(message, macSecretKey);
  }
  private byte[] generatePlaintextHMAC(byte[] message) throws NigoriCryptographyException {
    return generateHMAC(message, ivSecretKey);
  }
  private byte[] generateHMAC(byte[] message, byte[] secretKey) throws NigoriCryptographyException {

    try {
      String hmacAlgorithm = NigoriConstants.A_HMAC;
      Mac mac = Mac.getInstance(hmacAlgorithm);
      SecretKey key = new SecretKeySpec(secretKey, NigoriConstants.A_KMAC);
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
   * This method expects the IV to be stored in the first {@link NigoriConstants#B_AES} bytes and a MAC to be stored in the
   * final {@link NigoriConstants#B_MAC} bytes.
   * 
   * @param ciphertext the message to decrypt.
   * 
   * @throws NigoriCryptographyException if total length of message <48 bytes, if the MAC does 
   * not match the decoded data, or if something goes wrong with AES/CBC/PKCS5Padding inside the 
   * JCE library.
   */
  public byte[] decrypt(byte[] encryptionKey, byte[] ciphertext) throws NigoriCryptographyException {

    byte[] iv = new byte[NigoriConstants.B_SYMENC];
    byte[] mac = new byte[NigoriConstants.B_MAC];
    Cipher cipher;
    try {
      cipher = Cipher.getInstance(NigoriConstants.A_SYMENC_CIPHER);
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

    byte[] macCheck = generateCipherHMAC(data);
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
      SecretKey key = new SecretKeySpec(encryptionKey,NigoriConstants.A_SYMENC);
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

  public byte[] encryptDeterministically(byte[] plaintext) throws NigoriCryptographyException {
  	return encrypt(encryptionSecretKey, plaintext, false);
  }
  
  public byte[] encryptDeterministically(byte[] key, byte[] plaintext) throws
  NigoriCryptographyException {
    return encrypt(key, plaintext, false);
  }  
  
  private byte[] encrypt(byte[] key, byte[] plaintext, boolean randomIV)
  throws NigoriCryptographyException {

    try {
      Cipher cipher = Cipher.getInstance(NigoriConstants.A_SYMENC_CIPHER);
      SecretKey secret = new SecretKeySpec(key, NigoriConstants.A_SYMENC);
      byte[] iv = new byte[NigoriConstants.B_SYMENC];
      if (randomIV) {
        random.nextBytes(iv); 
      } else {
        byte[] ivMac = generatePlaintextHMAC(plaintext);
        xorFill(iv,ivMac);
      }
      IvParameterSpec ips = new IvParameterSpec(iv);
      cipher.init(Cipher.ENCRYPT_MODE, secret, ips);
      byte[] data = cipher.doFinal(plaintext);
      byte[] mac = generateCipherHMAC(data);

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
   * @param iv
   * @param ivMac
   */
  private void xorFill(byte[] iv, byte[] ivMac) {
    for (int macIdx = 0, ivIdx = 0; macIdx<ivMac.length; ++macIdx, ++ivIdx, ivIdx %= iv.length){
      iv[ivIdx] ^= ivMac[macIdx];
    }
  }

  /**
   * Return an instance of {@code SchnorrSign} which is capable of signing user-encrypted data.
   * 
   */
  public SchnorrSign signer() {
    return new SchnorrSign(userSecretKey);
  }

  /**
   * Destroy all the secret data stored in this KeyManager
   */
  public void destroy() {
    Arrays.fill(userSecretKey, (byte) 0);
    Arrays.fill(encryptionSecretKey, (byte) 0);
    Arrays.fill(macSecretKey, (byte) 0);
    Arrays.fill(ivSecretKey, (byte) 0);
    Arrays.fill(username, (byte) 0);
    Arrays.fill(password, (byte) 0);
  }

  @Override
  public void finalize() throws Throwable {
    destroy();
    super.finalize();
  }
}