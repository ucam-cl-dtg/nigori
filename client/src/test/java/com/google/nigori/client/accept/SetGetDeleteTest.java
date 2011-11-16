package com.google.nigori.client.accept;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.google.nigori.client.NigoriCryptographyException;
import com.google.nigori.client.NigoriDatastore;

public class SetGetDeleteTest {

  protected static class IndexValue {
    public final byte[] index;
    public final byte[] value;

    public IndexValue(String index, String value) {
      this.index = index.getBytes();
      this.value = index.getBytes();
    }
  }

  protected IndexValue[] testCases =
      {
          new IndexValue("index", "value"),
          new IndexValue("", "foo"),
          new IndexValue("foo", ""),
          new IndexValue("qwertyuiopasdfghjkl;zxcvbnm,./",
              "jlkvjhskldhjvguyvh78ryfgkjvjhzsahkjrtgagflbakjsdvskjhsjkhdafkjdashlkjajhasfkjsadhf;adshfkjd")};

  @Test
  public void setGetDelete() throws NigoriCryptographyException, IOException {
    NigoriDatastore nigori = AcceptanceTests.getStore();

    for (int i = 0; i < AcceptanceTests.REPEAT; ++i) {// check we can do this more than once
      assertTrue("Not registered" + i, nigori.register());
      for (IndexValue iv : testCases) {// once round for each
        final byte[] index = iv.index;
        final byte[] value = iv.value;
        assertTrue("Not put" + i, nigori.put(index, value));
        assertArrayEquals("Got different" + i, value, nigori.get(index));
        assertTrue("Not deleted" + i, nigori.delete(index));
        assertNull("Not deleted" + i, nigori.get(index));
      }
      // allow them to accumulate
      for (IndexValue iv : testCases) {
        final byte[] index = iv.index;
        final byte[] value = iv.value;
        assertTrue("Not put" + i, nigori.put(index, value));
      }
      for (IndexValue iv : testCases) {
        final byte[] index = iv.index;
        final byte[] value = iv.value;
        assertArrayEquals("Got different" + i, value, nigori.get(index));
      }
      for (IndexValue iv : testCases) {
        final byte[] index = iv.index;
        assertTrue("Not deleted" + i, nigori.delete(index));
      }
      for (IndexValue iv : testCases) {
        final byte[] index = iv.index;
        assertNull("Not deleted" + i, nigori.get(index));
      }
      assertTrue("Not unregistered", nigori.unregister());
    }
  }

}
