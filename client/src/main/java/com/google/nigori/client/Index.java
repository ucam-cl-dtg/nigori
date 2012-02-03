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
package com.google.nigori.client;

import java.util.Arrays;

/**
 * @author drt24
 * 
 */
public class Index {

  private final byte[] index;
  private final byte[] token;
  private final boolean isdeleted;

  public byte[] getBytes() {
    return index;
  }

  public boolean isDeleted() {
    return isdeleted;
  }

  public byte[] getDeletionRevision() {
    return token;
  }

  public Index(byte[] index) {
    this.index = index;
    token = null;
    isdeleted = false;
  }
  public Index(byte[] index, byte[] token){
    this.index = index;
    this.token = token;
    this.isdeleted = true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(index);
    result = prime * result + (isdeleted ? 1231 : 1237);
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
    Index other = (Index) obj;
    if (!Arrays.equals(index, other.index))
      return false;
    if (isdeleted != other.isdeleted)
      return false;
    return true;
  }

}
