package com.google.nigori.common;

public final class Util {

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
  
  public static int bin2int(byte[] array, int offset) {
    return ((array[offset + 0] & 0xff) << 24)
    + ((array[offset + 1] & 0xff) << 16)
    + ((array[offset + 2] & 0xff) << 8) 
    + (array[offset + 3] & 0xff);
  }
}
