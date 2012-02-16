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

import static com.google.nigori.client.accept.AcceptanceTests.REPEAT;
import static com.google.nigori.client.accept.n.ConcurrencyTest.REPEAT_FACTOR;
import static com.google.nigori.client.accept.n.ConcurrencyTest.THREADS;
import static com.google.nigori.client.accept.n.ConcurrencyTest.ifFailedPrintFailures;
import static com.google.nigori.client.accept.n.ConcurrencyTest.startThenJoinThreads;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.google.nigori.client.MigoriDatastore;
import com.google.nigori.client.NigoriCryptographyException;
import com.google.nigori.common.Index;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.UnauthorisedException;
import com.google.nigori.common.Util;

/**
 * @author drt24
 * 
 */
public class MConcurrencyTest extends AcceptanceTest {

  private boolean failed;
  private static final int REPEATS = REPEAT * REPEAT_FACTOR;

  @Test
  public void deterministicEqual() throws NigoriCryptographyException, IOException,
      UnauthorisedException {
    failed = false;
    Thread[] threads = new Thread[THREADS];

    final MigoriDatastore migori = getStore();
    final Index index = new Index("Concurrency");
    assertTrue("Not registered", migori.register());
    try {
      final List<Throwable> exceptionList =
          Collections.synchronizedList(new LinkedList<Throwable>());
      for (int j = 0; j < THREADS; ++j) {
        threads[j] = new Thread() {
          public void run() {
            boolean succeeded = false;
            try {

              RevValue last = migori.put(index, Util.int2bin(0));
              for (int i = 0; i < REPEATS; ++i) {
                last = migori.put(index, Util.int2bin(i), last);
              }
              succeeded = true;
            } catch (Throwable e) {
              exceptionList.add(e);
            } finally {
              if (!succeeded && !failed) {
                failed = true;
              }
            }
          }
        };
      }
      startThenJoinThreads(threads);

      ifFailedPrintFailures(failed, exceptionList);

      Collection<RevValue> heads = migori.get(index);
      assertEquals(1, heads.size());
      RevValue head = heads.toArray(new RevValue[1])[0];
      int total = Util.bin2int(head.getValue(), 0);
      assertEquals(REPEATS - 1, total);
      assertTrue(migori.removeIndex(index, head.getRevision()));

    } finally {
      assertTrue(migori.unregister());
    }
  }

  @Test
  public void nonInterferenceOfSeparateHistories() throws NigoriCryptographyException, IOException,
      UnauthorisedException {
    failed = false;
    Thread[] threads = new Thread[THREADS];

    final MigoriDatastore migori = getStore();
    final Index index = new Index("Concurrency");
    assertTrue("Not registered", migori.register());
    try {
      final List<Throwable> exceptionList =
          Collections.synchronizedList(new LinkedList<Throwable>());
      for (int j = 0; j < THREADS; ++j) {
        final int startFactor = j;
        threads[j] = new Thread() {
          public void run() {
            boolean succeeded = false;
            try {

              RevValue last = migori.put(index, Util.int2bin(0));
              for (int i = startFactor * REPEATS; i < startFactor * REPEATS + REPEATS; ++i) {
                last = migori.put(index, Util.int2bin(i), last);
              }
              succeeded = true;
            } catch (Throwable e) {
              exceptionList.add(e);
            } finally {
              if (!succeeded && !failed) {
                failed = true;
              }
            }
          }
        };
      }
      startThenJoinThreads(threads);

      ifFailedPrintFailures(failed, exceptionList);

      Collection<RevValue> heads = migori.get(index);
      assertEquals(4, heads.size());
      int total = 0;
      RevValue deleteAt = null;
      for (RevValue head : heads) {
        deleteAt = head;
        int value =Util.bin2int(head.getValue(), 0);
        total += value;
      }
      assertEquals(206, total);
      assertTrue(migori.removeIndex(index, deleteAt.getRevision()));

    } finally {
      assertTrue(migori.unregister());
    }
  }

}
