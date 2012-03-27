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

import static com.google.nigori.common.MessageLibrary.bytesToString;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import com.google.nigori.client.ComparableMerger.ConversionException;
import com.google.nigori.client.ComparableMerger.Converter;
import com.google.nigori.common.Index;
import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;
import com.google.nigori.common.UnauthorisedException;

/**
 * @author drt24
 * 
 */
public class ComparableMergerTest {

  private static final Index INDEX = new Index("test");
  private final ComparableMerger<String> merger = new ComparableMerger<String>(
      new Converter<String>() {

        @Override
        public String fromBytes(Index index, byte[] value) throws ConversionException {
          return bytesToString(value);
        }

        @Override
        public String fromLatest(String from) {
          return from;
        }

        @Override
        public byte[] toBytes(String from) {
          return MessageLibrary.toBytes(from);
        }
      });

  @Test
  public void mergeTwoEquiv() throws IOException, NigoriCryptographyException,
      UnauthorisedException {
    Collection<RevValue> heads = new ArrayList<RevValue>();
    byte[] test = MessageLibrary.toBytes("test");
    RevValue rv = new RevValue(Revision.EMPTY, test);
    RevValue rv1 = new RevValue(Revision.EMPTY, test);
    heads.add(rv);
    heads.add(rv1);
    MigoriDatastore store = createMock(MigoriDatastore.class);
    expect(store.put(eq(INDEX), aryEq(test), eq(rv), eq(rv1))).andReturn(rv);
    replay(store);
    RevValue value = merger.merge(store, INDEX, heads);
    verify(store);
    assertEquals(rv, value);
  }

  @Test
  public void mergeTwoEquivOneGreater() throws IOException, NigoriCryptographyException,
      UnauthorisedException {
    Collection<RevValue> heads = new ArrayList<RevValue>();
    byte[] test = MessageLibrary.toBytes("test");
    byte[] test1 = MessageLibrary.toBytes("test1");
    RevValue rv = new RevValue(Revision.EMPTY, test);
    RevValue rv1 = new RevValue(Revision.EMPTY, test);
    RevValue rv2 = new RevValue(Revision.EMPTY, test1);
    heads.add(rv);
    heads.add(rv1);
    heads.add(rv2);
    MigoriDatastore store = createMock(MigoriDatastore.class);
    expect(store.put(eq(INDEX), aryEq(test), eq(rv), eq(rv1))).andReturn(rv);
    expect(store.put(eq(INDEX), aryEq(test1), eq(rv), eq(rv2))).andReturn(rv2);
    replay(store);
    RevValue value = merger.merge(store, INDEX, heads);
    verify(store);
    assertEquals(rv2, value);
  }

  @Test
  public void mergeTwoDiff() throws IOException, NigoriCryptographyException, UnauthorisedException {
    Collection<RevValue> heads = new ArrayList<RevValue>();
    byte[] test = MessageLibrary.toBytes("test");
    byte[] test1 = MessageLibrary.toBytes("test1");
    RevValue rv = new RevValue(Revision.EMPTY, test);
    RevValue rv1 = new RevValue(Revision.EMPTY, test1);
    heads.add(rv);
    heads.add(rv1);
    MigoriDatastore store = createMock(MigoriDatastore.class);
    expect(store.put(eq(INDEX), aryEq(test1), eq(rv), eq(rv1))).andReturn(rv1);
    replay(store);
    RevValue value = merger.merge(store, INDEX, heads);
    verify(store);
    assertEquals(rv1, value);
  }
}
