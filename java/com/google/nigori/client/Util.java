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

import org.apache.commons.codec.binary.Base64;

/**
 * Set of utility functions to cope with Nigori's custom container formats.
 *
 * @author Alastair Beresford
 */
public class Util {

  private static byte[] int2bin(int i) {
    byte[] res = new byte[4];
    Util.int2bin(res,0,i);
    return res;
  }

  private static void int2bin(byte[] out, int offset, int i) {
    out[offset + 0] = (byte)(i >> 24); 
    out[offset + 1] = (byte)(i >> 16 & 0xff);
    out[offset + 2] = (byte)(i >> 8 & 0xff); 
    out[offset + 3] = (byte)(i & 0xff);		
  }

  private static int bin2int(byte[] array) {
    return Util.bin2int(array, 0);
  }

  private static int bin2int(byte[] array, int offset) {
    return ((array[offset + 0] & 0xff) << 24)
    + ((array[offset + 1] & 0xff) << 16)
    + ((array[offset + 2] & 0xff) << 8) 
    + (array[offset + 3] & 0xff);
  }

  /**
   * Return a hexadecimal representation of {@code array} as a {@code String}.
   * 
   * @param array the bytes to encode as a string
   * @return
   */
  public static String byteArray2HexString(byte[] array) {
    char[] hex = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    StringBuffer buf = new StringBuffer();
    for(int i = 0; i< array.length; i++) {
      int val = array[i] & 0xff;
      buf.append(hex[val >> 4]);
      buf.append(hex[val & 0xf]);
    }
    return buf.toString();
  }

  /**
   * Given a hexadecimal string {@code s}, return an equivalent byte array. 
   * 
   * @param s the string used to generate the byte array.
   * @return
   */
  public static byte[] hexString2ByteArray(String s) {
    byte[] res = new byte[s.length() / 2];
    for(int i = 0; i < s.length() - 1; i += 2) {
      int n1 = Character.digit(s.charAt(i), 16);
      int n2 = Character.digit(s.charAt(i+1), 16);
      int val = (n1 << 4) + n2;
      res[i / 2] = (byte) ((val < 128) ? val : val - 256);
    }
    return res;
  }

  /**
   * Flatten {@code data} into single byte array, prefixing each element
   * with a four-byte big-Endian integer containing the number of elements.
   * 
   * If {@code data} is {@code null}, this method returns {@code null}.
   * Ignores null elements in data.
   *   
   * @param data the two-dimensional array to flatten.
   */
  public static byte[] concatAndPrefix(byte[][] data) {
    if (data == null)
      return null;

    int totalLength = 0;
    for(int i = 0; i < data.length; i++) {
      if (data[i] != null) {
        totalLength += 4 + data[i].length;
      }
    }

    byte[] res = new byte[totalLength];
    int p = 0;
    for(int i = 0; i < data.length; i++) {
      if (data[i] != null) {
        byte[] len =  int2bin(data[i].length);
        System.arraycopy(len, 0, res, p, len.length);
        p += len.length;
        System.arraycopy(data[i], 0, res, p, data[i].length);
        p += data[i].length;
      }
    }

    return res;
  }

  /**
   * Given {@code data} containing blocks of binary data, each prefixed with a four-byte
   * big-Endian integer representing the block length, extract each block into a separate array.
   * 
   * @param data the array to extract the data blocks from.
   * @return
   */
  public static byte[][] deconcatWithPrefix(byte[] data) {
    if (data == null || data.length < 4)
      return null;

    int p = 0;
    int index = 0;
    while(p + 3 < data.length) {
      p+= bin2int(data,p) + 4;
      index++;
    }

    byte[][] res = new byte[index][];
    p = 0;
    index = 0;
    while(p + 3 < data.length) {
      byte[] array = new byte[bin2int(data, p)];
      p += 4;
      System.arraycopy(data, p, array, 0, array.length);
      p += array.length;
      res[index++] = array;
    }
    return res;
  }

  /**
   * Convert {@code input} into a base64url-safe byte array as specified in RFC 4648.
   * 
   * Note: The implementation of Apache Commons Codec for this does not appear to function as per
   * RFC 4648 suggests as it doesn't provide suitable padding.
   * 
   * In addition, this method only relies on 1.0 methods of the Apache Commons Codec library and
   * therefore works on Android without an additional jar file.
   * 
   * @param input the byte array to be encoded using base64url.
   * @return
   */
  public static byte[] encodeBase64UrlSafe(byte[] input) {
    String output = new String(Base64.encodeBase64(input));
    output = output.replace('/', '_').replace('+', '-').replace("=", "%3D");
    return output.getBytes();
  }

  /**
   * Decode {@code input} from a base64url-safe array into a raw byte array as per RFC 4648.
   *
   * @param input the array to decode.
   * @return
   */
  public static byte[] decodeBase64UrlSafe(byte[] input) {
    String in = new String(input);
    in = in.replace("%3D", "=").replace('_', '/').replace('-', '+');
    return Base64.decodeBase64(in.getBytes());
  }

  public static void main(String[] args) {

    int fails = 0;

    final byte[][] concatTestInExpected = {{1, 2}, {3, 4, 5}, {}, {7}};
    final byte[] concatTestOutExpected = 
    {0, 0, 0, 2, 1, 2, 0, 0, 0, 3, 3, 4, 5, 0, 0, 0, 0, 0, 0, 0, 1, 7};
    if(Util.concatAndPrefix(null) != null) {
      System.out.println("concatAndPrefix(null) != null!");
      fails++;
    }

    byte[] concatTestOutResult = Util.concatAndPrefix(concatTestInExpected);
    if (concatTestOutResult.length != concatTestOutExpected.length) {
      System.out.println("concatAndPrefix() output length differs from expected");
      fails++;
    } else {
      for(int i = 0; i < concatTestOutResult.length; i++) {
        if (concatTestOutResult[i] != concatTestOutExpected[i]) {
          System.out.println("Failure in concatAndPrefix output at position "+i);
          fails++;
        }
      }
    }

    byte[][] concatTestInResult = Util.deconcatWithPrefix(concatTestOutExpected);
    if (concatTestInResult.length != concatTestInExpected.length) {
      System.out.println("deconcatWithPrefix() output length differs from expected");
    } else {
      for(int i = 0; i < concatTestInResult.length; i++) {
        if (concatTestInResult[i].length != concatTestInResult[i].length) {
          System.out.println("Array length at element "+i+" is wrong.");
          fails++;
        } else {
          for(int j = 0; j < concatTestInResult[i].length; j++) {
            if (concatTestInResult[i][j] != concatTestInExpected[i][j]) {
              System.out.println("Failure in deconcatWithPrefix output array " +
                  i + " at position " + j);
              fails++;
            }
          }
        }
      }
    }

    final int[] intTests = {0,Integer.MAX_VALUE, Integer.MIN_VALUE, -1, 255, 256, -256};
    for(int i = 0; i < intTests.length; i++) {
      if(intTests[i] != Util.bin2int(Util.int2bin(intTests[i]))) {
        System.out.println("int2bin(bin2int()) error with value " + intTests[i]);
        fails++;
      }
    }

    final String hexTest = "78578e5a5d63cb06";
    if (!Util.byteArray2HexString(Util.hexString2ByteArray(hexTest)).equals(hexTest)) {
      System.out.println("Error: hex conversion error");
      fails++;
    }

    System.out.println("Tests complete. "+fails+" failed.");

  }
}
