/*
 * Copyright (C) 2011 Daniel R. Thomas (drt24)
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
package com.google.nigori.server.appengine;

import com.google.nigori.common.Util;

/**
 * 
 * @author drt24
 * 
 */
public class IntRevision implements Revision {
  private static final long serialVersionUID = 1L;

  private final int version;

  public IntRevision(int version) {
    this.version = version;
  }

  public int getVersion() {
    return version;
  }

  @Override
  public int compareTo(Revision o) {
    if (o instanceof IntRevision) {
      if (version < ((IntRevision) o).version) {
        return -1;
      } else if (version > ((IntRevision) o).version) {
        return 1;
      } else
        return 0;
    }
    if (o == null) {
      return 0;
    } else {
      return this.toString().compareTo(o.toString());
    }
  }

  @Override
  public String toString() {
    return "" + version;
  }

  @Override
  public byte[] getBytes() {
    return Util.int2bin(version);
  }

}
