package com.google.nigori.common;

import java.util.ArrayList;
import java.util.List;

public final class Util {

  public static final int INT = 4;
  public static final int LONG = 8;
  /**
   * Don't allow instantiation
   */
  private Util() {}

  public static void int2bin(byte[] out, int offset, int i) {
    out[offset + 0] = (byte)(i >> 24);
    out[offset + 1] = (byte)(i >> 16 & 0xff);
    out[offset + 2] = (byte)(i >> 8 & 0xff);
    out[offset + 3] = (byte)(i & 0xff);
  }

  public static byte[] int2bin(int i){
    byte[] out = new byte[4];
    int2bin(out,0,i);
    return out;
  }

  public static void long2bin(byte[] out, int offset, long i) {
    out[offset + 0] = (byte)(i >> 56);
    out[offset + 1] = (byte)(i >> 48 & 0xff);
    out[offset + 2] = (byte)(i >> 40 & 0xff);
    out[offset + 3] = (byte)(i >> 32 & 0xff);
    out[offset + 4] = (byte)(i >> 24 & 0xff);
    out[offset + 5] = (byte)(i >> 16 & 0xff);
    out[offset + 6] = (byte)(i >> 8 & 0xff);
    out[offset + 7] = (byte)(i & 0xff);
  }

  public static byte[] long2bin(long i){
    byte[] answer = new byte[8];
    long2bin(answer, 0, i);
    return answer;
  }

  public static int bin2int(byte[] array, int offset) {
    return ((array[offset + 0] & 0xff) << 24)
    + ((array[offset + 1] & 0xff) << 16)
    + ((array[offset + 2] & 0xff) << 8) 
    + (array[offset + 3] & 0xff);
  }

  public static long bin2long(byte[] array, int offset) {
    return ((array[offset + 0] & 0xffL) << 56) + ((array[offset + 1] & 0xffL) << 48)
        + ((array[offset + 2] & 0xffL) << 40) + ((array[offset + 3] & 0xffL) << 32)
        + ((array[offset + 4] & 0xffL) << 24) + ((array[offset + 5] & 0xffL) << 16)
        + ((array[offset + 6] & 0xffL) << 8) + (array[offset + 7] & 0xffL);
  }
  public static long bin2long(byte[] array){
    return bin2long(array,0);
  }

  public static byte[] joinBytes(byte[]... bytes){
    int length = 0;
    for (byte[] byt : bytes){
      length += byt.length;
    }
    byte[] answer = new byte[length + bytes.length * INT];// Content length + length fields
    int offset = 0;
    for (byte[] byt : bytes){
      System.arraycopy(Util.int2bin(byt.length), 0, answer, offset, INT);
      offset += INT;
      System.arraycopy(byt, 0, answer, offset, byt.length);
      offset += byt.length;
    }
    return answer;
  }

  public static List<byte[]> splitBytes(byte[] bytes){
    if (bytes == null){
      throw new IllegalArgumentException("bytes cannot be null");
    }
    List<byte[]> answer = new ArrayList<byte[]>();
    if (bytes.length != 0){
      if (bytes.length > INT){
        int offset = 0;
        while (offset < bytes.length) {
          int length = Util.bin2int(bytes, offset);
          if (bytes.length >= offset + length) {
            offset += INT;
            byte[] dest = new byte[length];
            System.arraycopy(bytes, offset, dest, 0, length);
            offset += length;
            answer.add(dest);
          } else {
            throw new IllegalArgumentException(
                "bytes not long enough to contain as much as it says it does");
          }
        }
      } else {
        throw new IllegalArgumentException("bytes must be at least big enough to have one length field in it or 0 length but was: " +bytes.length);
      }
    }
    return answer;
  }
}
