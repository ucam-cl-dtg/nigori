/*
 * Copyright (C) 2011 Daniel Thomas (drt24)
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
package com.google.nigori.server;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.Nonce;
import com.google.nigori.common.RevValue;

/**
 * @author drt24
 * 
 */
public abstract class AbstractDatabaseTest {

  private static byte[] publicKey;

  private Database database;

  @BeforeClass
  public static void initPublicKey() throws UnsupportedEncodingException {
    publicKey = "test-user's public key".getBytes(MessageLibrary.CHARSET);
  }
  @Before
  public void setupDatabase() {
    database = getDatabase();
  }

  /**
   * @return instance of database to test.
   */
  protected abstract Database getDatabase();

  @Test
  public void addDeleteUser() throws UserNotFoundException {
    assertFalse(database.haveUser(publicKey));
    assertTrue(database.addUser(publicKey));
    assertTrue(database.haveUser(publicKey));
    User user = database.getUser(publicKey);
    assertTrue(database.deleteUser(user));
    assertFalse(database.haveUser(publicKey));
  }

  @Test
  public void deleteNotPresent() {
    assertFalse(database.deleteUser(database.getUserFactory().getUser("non existant user key".getBytes(), new Date())));
  }

  @Test
  public void addTwice() throws UserNotFoundException {
    assertFalse(database.haveUser(publicKey));
    assertTrue(database.addUser(publicKey));
    assertFalse(database.addUser(publicKey));
    assertTrue(database.haveUser(publicKey));
    User user = database.getUser(publicKey);
    assertTrue(database.deleteUser(user));
    assertFalse(database.haveUser(publicKey));
  }

  @Test
  public void newNoncePasses() {
    Nonce nonce = new Nonce();
    assertTrue(database.checkAndAddNonce(nonce,publicKey));
  }

  @Test
  public void repeatedNonceFails() {
    Nonce nonce = new Nonce();
    assertTrue(database.checkAndAddNonce(nonce,publicKey));
    assertFalse(database.checkAndAddNonce(nonce,publicKey));
  }

  @Test
  public void setGetDelete() throws UserNotFoundException, IOException {
    database.addUser(publicKey);
    User user = database.getUser(publicKey);
    try {
      byte[] index = "index".getBytes();
      byte[] revision = "revision".getBytes();
      byte[] value = "value".getBytes();
      assertTrue(database.putRecord(user, index, revision, value));
      Collection<RevValue> revs = database.getRecord(user, index);
      assertEquals(1, revs.size());
      for (RevValue rv : revs) {
        assertArrayEquals(revision, rv.getRevision());
        assertArrayEquals(value, rv.getValue());
      }
      assertTrue(database.deleteRecord(user, index));
      assertNull(database.getRecord(user, index));
      assertFalse(database.deleteRecord(user, index));
    } finally {
      assertTrue(database.deleteUser(user));
    }
  }
  @Test
  public void getRevisions() throws UserNotFoundException, IOException {
    User user = null;
    try {
      assertTrue(database.addUser(publicKey));
      user = database.getUser(publicKey);
      final byte[] index = "index".getBytes(MessageLibrary.CHARSET);
      final byte[] revisiona = "revisiona".getBytes(MessageLibrary.CHARSET);
      final byte[] revisionb = "revisionb".getBytes(MessageLibrary.CHARSET);
      final byte[] a = "a".getBytes(MessageLibrary.CHARSET);
      final byte[] b = "b".getBytes(MessageLibrary.CHARSET);
      assertTrue(database.putRecord(user, index, revisiona, a));
      assertTrue(database.putRecord(user, index, revisionb, b));
      Collection<byte[]> revisions = database.getRevisions(user, index);
      assertNotNull("No revisions",revisions);
      assertEquals(2, revisions.size());
      assertThat(revisions,hasItem(revisiona));
      assertThat(revisions,hasItem(revisionb));
      assertTrue(database.deleteRecord(user, index));
    } finally {
      assertTrue(database.deleteUser(user));
    }
  }
}
