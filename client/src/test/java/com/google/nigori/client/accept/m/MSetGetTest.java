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
package com.google.nigori.client.accept.m;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItems;

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import com.google.nigori.client.MigoriDatastore;
import com.google.nigori.common.Index;
import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.UnauthorisedException;

/**
 * @author drt24
 * 
 */
public class MSetGetTest extends AcceptanceTest {

  @Test
  public void putGraph() throws NigoriCryptographyException, IOException, UnauthorisedException {
    MigoriDatastore store = getStore();
    store.register();
    try {
      Index index = new Index("test");
      RevValue a = store.put(index, "a".getBytes());
      RevValue b = store.put(index, "b".getBytes(), a);
      RevValue c = store.put(index, "c".getBytes(), a);
      RevValue d = store.put(index, "d".getBytes(), b, c);
      RevValue e = store.put(index, "e".getBytes(), c, b);
      RevValue f = store.put(index, "f".getBytes(), d);
      RevValue g = store.put(index, "g".getBytes(), e);
      Collection<RevValue> heads = store.get(index);
      assertThat(heads, hasItems(f, g));

      assertTrue(store.removeIndex(index, g.getRevision()));
    } finally {
      store.unregister();
    }
  }
}
