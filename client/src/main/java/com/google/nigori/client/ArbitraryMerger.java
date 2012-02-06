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

import java.util.Collection;

import com.google.nigori.client.MigoriDatastore.MigoriMerger;
import com.google.nigori.common.Index;
import com.google.nigori.common.RevValue;

/**
 * Arbitrarily pick a current head and use that
 * 
 * This is very stupid
 * 
 * @author drt24
 * 
 */
public class ArbitraryMerger implements MigoriMerger {

  @Override
  public byte[] merge(Index index, Collection<RevValue> heads) {
    for (RevValue rv : heads) {
      return rv.getValue();
    }
    throw new IllegalStateException("Must be at least one value to merge");
  }

}
