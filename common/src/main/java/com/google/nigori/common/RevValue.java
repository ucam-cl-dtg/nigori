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

/**
 * @author drt24
 *
 */
public class RevValue {

  private final byte[] revision;
  private final byte[] value;
  public RevValue(byte[] revision, byte[] value) {
    this.revision = revision;
    this.value = value;
  }
  /**
   * @return the revision
   */
  public byte[] getRevision() {
    return revision;
  }
  /**
   * @return the value
   */
  public byte[] getValue() {
    return value;
  }
}
