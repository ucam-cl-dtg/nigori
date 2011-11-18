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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.junit.Test;

import com.google.nigori.client.NigoriCryptographyException;
import com.google.nigori.client.NigoriDatastore;
import com.google.nigori.client.accept.SetGetDeleteTest.IndexValue;

public class ConcurrencyTest {

  private static final int THREADS = 8;
  protected boolean failed = false;

  /**
   * Test that multiple users can do stuff at the same time.
   * @throws NigoriCryptographyException
   * @throws IOException
   */
  @Test
  public void multiUserConcurrency() throws NigoriCryptographyException, IOException {
    Thread[] threads = new Thread[THREADS];

    for (int j = 0; j < THREADS; ++j) {
      threads[j] = new Thread() {
        public void run() {
          try {
            NigoriDatastore nigori = AcceptanceTests.getStore();

            for (int i = 0; i < AcceptanceTests.REPEAT * 10; ++i) {// check we can do this more than
                                                                   // once
              assertTrue("Not registered" + i, nigori.register());
              for (IndexValue iv : SetGetDeleteTest.testCases) {// once round for each
                final byte[] index = iv.index;
                final byte[] value = iv.value;
                assertTrue("Not put" + i, nigori.put(index, value));
                assertArrayEquals("Got different" + i, value, nigori.get(index));
                assertTrue("Not deleted" + i, nigori.delete(index));
                assertNull("Not deleted" + i, nigori.get(index));
              }
              assertTrue(nigori.unregister());
            }
          } catch (NullPointerException e) {
          } catch (IOException e) {
            failed = true;
            // TODO(drt24) put the exception somewhere
            e.printStackTrace();
          } catch (NigoriCryptographyException e) {
            // TODO(drt24) put the exception somewhere
            failed = true;
            e.printStackTrace();
          }
        }
      };
    }
    for (Thread t : threads){
      t.start();
    }
    for (Thread t : threads){
      try {
        t.join();
      } catch (InterruptedException e) {// just continue
      }
    }
    assertFalse(failed);
  }
  /**
   * Test that one user can do lots of things at the same time.
   * @throws NigoriCryptographyException
   * @throws IOException
   */
  @Test
  public void singleUserConcurrency() throws NigoriCryptographyException, IOException {
    Thread[] threads = new Thread[THREADS];

    final NigoriDatastore nigori = AcceptanceTests.getStore();
    assertTrue("Not registered", nigori.register());
    for (int j = 0; j < THREADS; ++j) {
      threads[j] = new Thread() {
        public void run() {
          try {
            Random r = new Random();
            for (int i = 0; i < AcceptanceTests.REPEAT * 10; ++i) {// check we can do this more than
                                                                   // once
              for (IndexValue iv : SetGetDeleteTest.testCases) {// once round for each
                final byte[] index = new byte[16];
                r.nextBytes(index);// need to generate some different indices
                final byte[] value = iv.value;
                assertTrue("Not put" + i, nigori.put(index, value));
                assertArrayEquals("Got different" + i, value, nigori.get(index));
                assertTrue("Not deleted" + i, nigori.delete(index));
                assertNull("Not deleted" + i, nigori.get(index));
              }

            }
          } catch (NullPointerException e) {
          } catch (IOException e) {
            failed = true;
            // TODO(drt24) put the exception somewhere
            e.printStackTrace();
          } catch (NigoriCryptographyException e) {
            // TODO(drt24) put the exception somewhere
            failed = true;
            e.printStackTrace();
          }
        }
      };
    }
    for (Thread t : threads){
      t.start();
    }
    for (Thread t : threads){
      try {
        t.join();
      } catch (InterruptedException e) {// just continue
      }
    }
    assertTrue(nigori.unregister());
    assertFalse(failed);
  }

  //TODO(drt24) once we have versioning then test that concurrent execution on the same keys works. 
}
