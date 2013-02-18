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

import static com.google.nigori.common.MessageLibrary.toBytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.nigori.common.NigoriConstants;
import com.google.nigori.common.Nonce;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Util;

/**
 * @author drt24
 * 
 */
public abstract class AbstractDatabaseTest {

  protected static byte[] publicKey;
  protected static byte[] publicHash;
  private static byte[] notKey;
  private static byte[] notHash;

  protected Database database;

  @BeforeClass
  public static void initPublicKey() throws NoSuchAlgorithmException {

    publicKey = new byte[NigoriConstants.B_DSA];
    Random r = new Random();
    r.nextBytes(publicKey);
    publicHash = Util.hashKey(publicKey);
    notKey = new byte[NigoriConstants.B_DSA];
    r.nextBytes(notKey);
    notHash = Util.hashKey(notKey);
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
    assertFalse(database.haveUser(publicHash));
    assertTrue(database.addUser(publicKey,publicHash));
    assertTrue(database.haveUser(publicHash));
    User user = database.getUser(publicHash);
    assertTrue("User not deleted", database.deleteUser(user));
    assertFalse(database.haveUser(publicHash));
  }

  @Test
  public void deleteNotPresent() {
    assertFalse(database.deleteUser(database.getUserFactory().getUser(
        notKey,notHash, new Date())));
  }

  @Test
  public void addTwice() throws UserNotFoundException {
    assertFalse(database.haveUser(publicHash));
    assertTrue(database.addUser(publicKey, publicHash));
    assertFalse("Could add user twice", database.addUser(publicKey, publicHash));
    assertTrue(database.haveUser(publicHash));
    User user = database.getUser(publicHash);
    assertTrue(database.deleteUser(user));
    assertFalse(database.haveUser(publicHash));
  }

  @Test
  public void newNoncePasses() {
    Nonce nonce = new Nonce();
    assertTrue(database.checkAndAddNonce(nonce, publicHash));
  }

  @Test
  public void repeatedNonceFails() {
    Nonce nonce = new Nonce();
    assertTrue(database.checkAndAddNonce(nonce, publicHash));
    assertFalse(database.checkAndAddNonce(nonce, publicHash));
  }

  @Test
  public void setGetDelete() throws UserNotFoundException, IOException {
    database.addUser(publicKey, publicHash);
    User user = database.getUser(publicHash);
    try {
      byte[] index = "index".getBytes();
      byte[] revision = "revision".getBytes();
      byte[] value = "value".getBytes();
      assertTrue(database.putRecord(user, index, revision, value));
      Collection<RevValue> revs = database.getRecord(user, index);
      assertEquals(1, revs.size());
      for (RevValue rv : revs) {
        assertArrayEquals(revision, rv.getRevision().getBytes());
        assertArrayEquals(value, rv.getValue());
      }
      assertTrue(database.deleteRecord(user, index));
      assertNull(database.getRecord(user, index));
      assertFalse(database.deleteRecord(user, index));
    } finally {
      assertTrue("User not deleted", database.deleteUser(user));
    }
  }

  @Test
  public void getRevisions() throws UserNotFoundException, IOException {
    User user = null;
    try {
      assertTrue(database.addUser(publicKey, publicHash));
      user = database.getUser(publicHash);
      final byte[] index = toBytes("index");
      final byte[] revisiona = toBytes("revisiona");
      final byte[] revisionb = toBytes("revisionb");
      final byte[] a = toBytes("a");
      final byte[] b = toBytes("b");
      assertTrue(database.putRecord(user, index, revisiona, a));
      assertTrue(database.putRecord(user, index, revisionb, b));
      Collection<byte[]> revisions = database.getRevisions(user, index);
      assertNotNull("No revisions", revisions);
      assertEquals(2, revisions.size());
      assertThat(revisions, hasItem(revisiona));
      assertThat(revisions, hasItem(revisionb));
      assertArrayEquals(a, database.getRevision(user, index, revisiona).getValue());
      assertArrayEquals(b, database.getRevision(user, index, revisionb).getValue());
      assertTrue(database.deleteRecord(user, index));
    } finally {
      assertTrue("User not deleted", database.deleteUser(user));
    }
  }

  @Test
  public void getNoRevisions() throws UserNotFoundException, IOException {
    User user = null;
    try {
      assertTrue(database.addUser(publicKey,publicHash));
      user = database.getUser(publicHash);
      final byte[] index = toBytes("index");
      assertNull(database.getRevisions(user, index));
    } finally {
      assertTrue("User not deleted", database.deleteUser(user));
    }
  }

  @Test
  public void userDataDeletion() throws UserNotFoundException, IOException {
    User user = null;
    try {
      assertTrue(database.addUser(publicKey,publicHash));
      user = database.getUser(publicHash);
      assertTrue(database.putRecord(user, toBytes("foo"), toBytes("bar"), toBytes("baz")));
    } finally {
      if (user != null)
        database.deleteUser(user);
    }
    try {
      assertTrue(database.addUser(publicKey,publicHash));
      user = database.getUser(publicHash);
      assertNull(database.getRevision(user, toBytes("foo"), toBytes("bar")));
    } finally {
      if (user != null)
        database.deleteUser(user);
    }
  }

  @Test
  public void getIndices() throws UserNotFoundException, IOException {
    User user = null;
    try {
      assertTrue(database.addUser(publicKey,publicHash));
      user = database.getUser(publicHash);
      final byte[] indexa = toBytes("indexa");
      final byte[] indexb = toBytes("indexb");
      final byte[] revisiona = toBytes("revisiona");
      final byte[] revisionb = toBytes("revisionb");
      final byte[] a = toBytes("a");
      final byte[] b = toBytes("b");
      assertTrue(database.putRecord(user, indexa, revisiona, a));
      assertTrue(database.putRecord(user, indexb, revisionb, b));
      Collection<byte[]> indices = database.getIndices(user);
      assertNotNull("No indices", indices);
      assertEquals(2, indices.size());
      assertThat(indices, hasItem(indexa));
      assertThat(indices, hasItem(indexb));
      assertTrue(database.deleteRecord(user, indexa));
      assertTrue(database.deleteRecord(user, indexb));
    } finally {
      assertTrue("User not deleted", database.deleteUser(user));
    }
  }
}
