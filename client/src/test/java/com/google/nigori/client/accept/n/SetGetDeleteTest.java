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
package com.google.nigori.client.accept.n;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.google.nigori.client.NigoriDatastore;
import com.google.nigori.client.accept.AcceptanceTests;
import com.google.nigori.common.Index;
import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;
import com.google.nigori.common.UnauthorisedException;
import com.google.nigori.common.Util;

public class SetGetDeleteTest extends AcceptanceTest {

  public SetGetDeleteTest(DatastoreFactory store) {
    super(store);
  }

  protected static class IndexValue {
    public final Index index;
    public final RevValue revvalue;
    public final boolean later;

    public IndexValue(String index, String revision, String value, boolean later) {
      this.index = new Index(index);
      this.revvalue = new RevValue(revision.getBytes(),value.getBytes());
      this.later = later;
    }
  }

  protected static final IndexValue[] testCases =
      {
          new IndexValue("index", "", "value", false),
          new IndexValue("index", "a", "value", true),
          new IndexValue("", " ", "foo", false),
          new IndexValue("", "1", "foo", true),
          new IndexValue("foo", "", "", false),
          new IndexValue("foo", "0", "", true),
          new IndexValue("qwertyuiopasdfghjkl;zxcvbnm,./", " ",
              "jlkvjhskldhjvguyvh78ryfgkjvjhzsahkjrtgagflbakjsdvskjhsjkhdafkjdashlkjajhasfkjsadhf;adshfkjd", false)};

  @Test
  public void setGetDelete() throws NigoriCryptographyException, IOException, UnauthorisedException {
    NigoriDatastore nigori = getStore();

    for (int i = 0; i < AcceptanceTests.REPEAT; ++i) {// check we can do this more than once
      try {
        assertTrue("Not registered" + i, nigori.register());

        for (IndexValue iv : testCases) {// once round for each
          final Index index = iv.index;
          final Revision revision = iv.revvalue.getRevision();
          final byte[] value = iv.revvalue.getValue();

          assertTrue("Not put" + i, nigori.put(index, revision, value));
          List<RevValue> revs = nigori.get(index);
          assertFalse("Revisions must exist", revs.isEmpty());
          assertEquals("Not one revision", 1, revs.size());
          RevValue rv = revs.get(0);
          assertArrayEquals("Got different" + i, value, rv.getValue());
          assertEquals("Got different" + i, revision, rv.getRevision());
          assertTrue("Not deleted" + i, nigori.delete(index,NULL_DELETE_TOKEN));
          assertNull("Not deleted" + i, nigori.get(index));
          assertFalse("Could redelete", nigori.delete(index,NULL_DELETE_TOKEN));
        }
        // allow them to accumulate
        for (IndexValue iv : testCases) {
          final Index index = iv.index;
          final byte[] value = iv.revvalue.getValue();
          final Revision revision = iv.revvalue.getRevision();
          assertTrue("Not put" + i, nigori.put(index, revision, value));
        }
        try {
          for (IndexValue iv : testCases) {
            final Index index = iv.index;
            final byte[] value = iv.revvalue.getValue();
            final Revision revision = iv.revvalue.getRevision();
            assertArrayEquals("Got different" + i, value, nigori.getRevision(index, revision));
          }
        } finally {
          for (IndexValue iv : testCases) {
            final Index index = iv.index;
            if (!iv.later) {
              assertTrue("Not deleted" + i, nigori.delete(index,NULL_DELETE_TOKEN));
            } else {// should have already been deleted
              assertFalse("Not deleted" + i, nigori.delete(index,NULL_DELETE_TOKEN));
            }
          }
        }
        for (IndexValue iv : testCases) {
          final Index index = iv.index;
          assertNull("Not deleted" + i, nigori.get(index));
        }
      } finally {
        assertTrue("Not unregistered", nigori.unregister());
      }
    }
  }

  @Test
  public void getRevisions() throws IOException, NigoriCryptographyException, UnauthorisedException {
    NigoriDatastore nigori = getStore();
    try {
      assertTrue("Not registered", nigori.register());
      final Index index = new Index("index");
      final Revision a = new Revision("a");
      final Revision b = new Revision("b");
      assertTrue("Not put", nigori.put(index, a, "aa".getBytes(MessageLibrary.CHARSET)));
      assertTrue("Not put", nigori.put(index, b, "bb".getBytes(MessageLibrary.CHARSET)));
      try {
        List<Revision> revisions = nigori.getRevisions(index);
        assertNotNull("No revisions", revisions);
        assertEquals("Not correct number of revisions", 2,revisions.size());
        assertThat(revisions,hasItem(a));
        assertThat(revisions,hasItem(b));
      } finally {
        nigori.delete(index,NULL_DELETE_TOKEN);
      }
    } finally {
      assertTrue("Not unregistered", nigori.unregister());
    }
  }

  @Test
  public void getIndices() throws IOException, NigoriCryptographyException, UnauthorisedException {
    NigoriDatastore nigori = getStore();
    try {
      assertTrue("Not registered", nigori.register());
      final Index indexa = new Index("indexa");
      final Index indexb = new Index("indexb");
      final Revision revision = new Revision("a");
      assertTrue("Not put", nigori.put(indexa, revision, "aa".getBytes(MessageLibrary.CHARSET)));
      assertTrue("Not put", nigori.put(indexb, revision, "bb".getBytes(MessageLibrary.CHARSET)));
      try {
        List<Index> indices = nigori.getIndices();
        assertNotNull("No indices", indices);
        assertEquals("Not correct number of indices", 2, indices.size());
        assertThat(indices, hasItem(indexa));
        assertThat(indices, hasItem(indexb));
      } finally {
        nigori.delete(indexa,NULL_DELETE_TOKEN);
        nigori.delete(indexb,NULL_DELETE_TOKEN);
      }
    } finally {
      assertTrue("Not unregistered", nigori.unregister());
    }
  }

  //@Test //TODO(drt24) Do we want to enforce revisions being immutable? Migori doesn't really need this due to the way revisions are generated.
  public void immutableRevisions() throws IOException, NigoriCryptographyException,
      UnauthorisedException {
    NigoriDatastore nigori = getStore();
    try {
      assertTrue("Not registered", nigori.register());
      final Index index = new Index("index");
      final Revision revision = new Revision("rev");
      assertTrue("Not put", nigori.put(index, revision, Util.int2bin(0)));
      assertFalse("Could replace revision value", nigori.put(index, revision, Util.int2bin(1)));
      nigori.delete(index, NULL_DELETE_TOKEN);
    } finally {
      assertTrue("Not unregistered", nigori.unregister());
    }
  }
}
