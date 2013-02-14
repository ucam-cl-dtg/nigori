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

import org.apache.commons.codec.binary.Base64;

/**
 * @author drt24
 * 
 */
public class BytesRevision implements Revision {

  private static final long serialVersionUID = 1L;

  private final byte[] revision;

  public BytesRevision(byte[] revision) {
    this.revision = revision;
  }

  @Override
  public int compareTo(Revision o) {
    if (o == null) {
      return 0;
    } else {
      return this.toString().compareTo(o.toString());
    }
  }

  @Override
  public String toString() {
    return "" + Base64.encodeBase64String(revision);
  }

  @Override
  public byte[] getBytes() {
    return revision;
  }
}
