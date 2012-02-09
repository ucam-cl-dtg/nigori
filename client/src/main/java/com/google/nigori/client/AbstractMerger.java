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

import java.io.IOException;
import java.util.Collection;

import com.google.nigori.client.MigoriDatastore.MigoriMerger;
import com.google.nigori.common.Index;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.UnauthorisedException;

/**
 * Handles the putting of the value obtained so that extending classes don't have to.
 * 
 * @author drt24
 * 
 */
public abstract class AbstractMerger implements MigoriMerger {

  @Override
  public RevValue merge(MigoriDatastore store, Index index, Collection<RevValue> heads)
      throws IOException, NigoriCryptographyException {
    try {
      return store.put(index, doMerge(index, heads), heads.toArray(new RevValue[0]));
    } catch (UnauthorisedException e) {
      throw new IOException(e);// Something has gone wrong half way through an operation
    }
  }

  /**
   * @param store
   * @param index
   * @param heads
   * @return the value to use as the merged head, will be put with the correct revision
   */
  protected abstract byte[] doMerge(Index index, Collection<RevValue> heads);

}
