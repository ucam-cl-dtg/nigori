/*
 * Copyright (C) 2012 Daniel R. Thomas (drt24).
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
package com.google.nigori.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

import org.junit.Test;

public class UtilTest {

  // Wikipedia says this is the big endian representation of 1000
  static final byte[] thousandint = {0, 0, 0x03, (byte) 0xE8};
  static final byte[] thousandlong = {0, 0, 0, 0, 0, 0, 0x03, (byte) 0xE8};
  // thousandint||thousandlong
  static final byte[] thousandThousand = {
      0, 0, 0, 4, 0, 0, 0x03, (byte) 0xE8, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0x03, (byte) 0xE8};

  @Test
  public void int2bin() {
    assertArrayEquals(thousandint, Util.int2bin(1000));
    byte[] out = new byte[4];
    Util.int2bin(out, 0, 1000);
    assertArrayEquals(thousandint, out);
  }

  @Test
  public void long2bin() {
    assertArrayEquals(thousandlong, Util.long2bin(1000));
    byte[] out = new byte[8];
    Util.long2bin(out, 0, 1000);
    assertArrayEquals(thousandlong, out);
  }

  @Test
  public void bin2int() {
    assertEquals(1000, Util.bin2int(thousandint, 0));
  }

  @Test
  public void bin2long() {
    assertEquals(1000L, Util.bin2long(thousandlong, 0));
  }

  @Test
  public void int2bin2int() {
    int2bin2int(2356783);
    int2bin2int(-39859);
    int2bin2int(0);
    int2bin2int(-1);
    int2bin2int(1);
    int2bin2int(Integer.MAX_VALUE);
    int2bin2int(Integer.MIN_VALUE);
  }

  private void int2bin2int(int value) {
    assertEquals(value, Util.bin2int(Util.int2bin(value), 0));
  }

  @Test
  public void long2bin2long() {
    long2bin2long(2356783);
    long2bin2long(-39859);
    long2bin2long(0);
    long2bin2long(-1);
    long2bin2long(1);
    long2bin2long(Integer.MAX_VALUE);
    long2bin2long(Integer.MIN_VALUE);
    long2bin2long(Long.MAX_VALUE);
    long2bin2long(Long.MIN_VALUE);
  }

  private void long2bin2long(long value) {
    assertEquals(value, Util.bin2long(Util.long2bin(value), 0));
  }

  // TODO(drt24) twosCompliemntToPositiveInt, positiveIntToTwosCompliment
  // TODO(drt24) byteToBigInt, bigIntToByte (also rationalise naming)

  @Test
  public void joinBytes() {
    assertArrayEquals(thousandThousand, Util.joinBytes(thousandint, thousandlong));
  }

  @Test
  public void splitBytes() {
    assertThat(Util.splitBytes(thousandThousand), hasItems(thousandint, thousandlong));
  }

  @Test
  public void joinSplitJoinBytes() {
    assertArrayEquals(thousandThousand, Util.joinBytes(Util.listByte2Array(Util.splitBytes(Util
        .joinBytes(thousandint, thousandlong)))));
    assertArrayEquals(thousandThousand, Util.joinBytes(Util.listByte2Array(Util
        .splitBytes(thousandThousand))));
  }
  // TODO(drt24) test the rest of the methods in Util
}
