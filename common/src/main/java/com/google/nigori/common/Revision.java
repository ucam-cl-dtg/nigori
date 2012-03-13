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
package com.google.nigori.common;

import static com.google.nigori.common.MessageLibrary.bytesToString;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

/**
 * @author drt24
 * 
 */
public class Revision {

  public static final Revision EMPTY = new Revision(new byte[]{});
  private final byte[] revision;

  public byte[] getBytes() {
    return revision;
  }

  public Revision(byte[] revision) {
    if (revision == null){
      throw new NullPointerException("Null revisons not allowed");
    }
    this.revision = revision;
  }

  /**
   * This methods exists as a convenience for when writing tests, use is not encouraged in general.
   * @param revision
   * @throws UnsupportedEncodingException
   */
  public Revision(String revision) throws UnsupportedEncodingException{
    if (revision == null){
      throw new NullPointerException("Null revisons not allowed");
    }
    this.revision = MessageLibrary.toBytes(revision);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(revision);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Revision other = (Revision) obj;
    if (!Arrays.equals(revision, other.revision))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return bytesToString(Base64.encodeBase64(revision));
  }
}
