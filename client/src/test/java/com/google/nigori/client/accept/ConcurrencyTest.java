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
package com.google.nigori.client.accept;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.google.nigori.client.NigoriCryptographyException;
import com.google.nigori.client.NigoriDatastore;
import com.google.nigori.client.accept.SetGetDeleteTest.IndexValue;
import com.google.nigori.common.Index;
import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.Revision;

public class ConcurrencyTest extends AcceptanceTest {

  public ConcurrencyTest(DatastoreFactory store) {
    super(store);
  }
  private static final int THREADS = 5;
  protected boolean failed = false;

  private void startThenJoinThreads(Thread[] threads){
    for (Thread t : threads){
      t.start();
    }
    for (Thread t : threads){
      try {
        t.join();
      } catch (InterruptedException e) {// just continue
      }
    }
  }
  private void ifFailedPrintFailures(boolean failed, List<Throwable> exceptionList) throws UnsupportedEncodingException{
    if (failed){
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      for (Throwable e : exceptionList){
        e.printStackTrace(ps);
        //ps.println();
      }
      String stacktraces = baos.toString(MessageLibrary.CHARSET);
      fail("Exceptions during concurrency testing\n" + stacktraces);
    }
  }
  /**
   * Test that multiple users can do stuff at the same time.
   * @throws NigoriCryptographyException
   * @throws IOException
   */
  @Test
  public void multiUserConcurrency() throws NigoriCryptographyException, IOException {
    failed = false;
    Thread[] threads = new Thread[THREADS];

    final List<Throwable> exceptionList = Collections.synchronizedList(new LinkedList<Throwable>());
    for (int j = 0; j < THREADS; ++j) {
      threads[j] = new Thread() {
        public void run() {
          boolean succeeded = false;
          try {
            NigoriDatastore nigori = getStore();

            for (int i = 0; i < AcceptanceTests.REPEAT * 10; ++i) {// check we can do this more than
                                                                   // once
              assertTrue("Not registered" + i, nigori.register());
              try {
                for (IndexValue iv : SetGetDeleteTest.testCases) {// once round for each
                  final Index index = iv.index;
                  final byte[] value = iv.revvalue.getValue();
                  final Revision revision = iv.revvalue.getRevision();
                  assertTrue("Not put" + i, nigori.put(index, revision, value));
                  assertArrayEquals("Got different" + i, value, nigori.getRevision(index, revision));
                  assertTrue("Not deleted" + i, nigori.delete(index,NULL_DELETE_TOKEN));
                  assertNull("Not deleted" + i, nigori.get(index));
                }
              } finally {
                assertTrue(nigori.unregister());
              }
            }
            succeeded = true;
          } catch (Throwable e) {
            exceptionList.add(e);
          } finally {
            if (!succeeded && !failed ){
              failed = true;
            }
          }
        }
      };
    }
    startThenJoinThreads(threads);
    ifFailedPrintFailures(failed,exceptionList);
  }
  /**
   * Test that one user can do lots of things at the same time.
   * @throws NigoriCryptographyException
   * @throws IOException
   */
  @Test
  public void singleUserConcurrency() throws NigoriCryptographyException, IOException {
    failed = false;
    Thread[] threads = new Thread[THREADS];

    final NigoriDatastore nigori = getStore();
    assertTrue("Not registered", nigori.register());
    try {
      final List<Throwable> exceptionList = Collections.synchronizedList(new LinkedList<Throwable>());
      for (int j = 0; j < THREADS; ++j) {
        threads[j] = new Thread() {
          public void run() {
            boolean succeeded = false;
            try {
              Random r = new Random();
              for (int i = 0; i < AcceptanceTests.REPEAT * 10; ++i) {// check we can do this more than
                // once
                for (IndexValue iv : SetGetDeleteTest.testCases) {// once round for each
                  final Index index = new Index(new byte[16]);
                  r.nextBytes(index.getBytes());// need to generate some different indices
                  final byte[] value = iv.revvalue.getValue();
                  final Revision revision = iv.revvalue.getRevision();
                  assertTrue("Not put" + i, nigori.put(index, revision, value));
                  try {
                    assertArrayEquals("Got different" + i, value, nigori.getRevision(index,revision));
                  } finally {
                    assertTrue("Not deleted" + i, nigori.delete(index, NULL_DELETE_TOKEN));
                  }
                  assertNull("Not deleted" + i, nigori.get(index));
                }
              }
              succeeded = true;
            } catch (Throwable e) {
              exceptionList.add(e);
            } finally {
              if (!succeeded && !failed ){
                failed = true;
              }
            }
          }
        };
      }
      startThenJoinThreads(threads);

      ifFailedPrintFailures(failed,exceptionList);
    } finally {
      assertTrue(nigori.unregister());
    }
  }

  //TODO(drt24) once we have versioning then test that concurrent execution on the same keys works. 
}
