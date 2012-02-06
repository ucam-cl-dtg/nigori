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

import java.util.Arrays;

/**
 * @author drt24
 *
 */
public class RevValue implements Comparable<RevValue> {

  private final Revision revision;
  private final byte[] value;

  public RevValue(Revision revision, byte[] value) {
    this.revision = revision;
    this.value = value;
  }

  public RevValue(byte[] revision, byte[] value) {
    this.revision = new Revision(revision);
    this.value = value;
  }

  /**
   * @return the revision
   */
  public Revision getRevision() {
    return revision;
  }
  /**
   * @return the value
   */
  public byte[] getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((revision == null) ? 0 : revision.hashCode());
    result = prime * result + Arrays.hashCode(value);
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
    RevValue other = (RevValue) obj;
    if (revision == null) {
      if (other.revision != null)
        return false;
    } else if (!revision.equals(other.revision))
      return false;
    if (!Arrays.equals(value, other.value))
      return false;
    return true;
  }

  @Override
  public int compareTo(RevValue arg0) {
    if (arg0 == null){
      return -1;
    }
    byte[] thisBytes = this.revision.getBytes();
    byte[] argBytes = arg0.revision.getBytes();
    if (Arrays.equals(thisBytes, argBytes)){
      return 0;
    }
    for (int i = 0; i < thisBytes.length; ++i){
      if (thisBytes[i] < argBytes[i]){
        return -1;
      } else if (thisBytes[i] > argBytes[i]){
        return 1;
      }
    }
    return 0;
  }
  @Override
  public String toString() {
    return revision + " : "+ new String(value);
  }
}
