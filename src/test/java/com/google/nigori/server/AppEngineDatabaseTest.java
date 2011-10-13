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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.nigori.common.MessageLibrary;

/**
 * @author drt24
 * 
 */
public class AppEngineDatabaseTest {

  private static byte[] user;
  private static byte[] publicKey;

  private Database database;
  private static LocalServiceTestHelper helper;

  @BeforeClass
  public static void init() throws UnsupportedEncodingException {
    helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    user = "test-username".getBytes(MessageLibrary.CHARSET);
    publicKey = "test-user's public encryption and signing key".getBytes(MessageLibrary.CHARSET);
  }

  @Before
  public void setup() {
    helper.setUp();
    database = new AppEngineDatabase();
  }

  @Test
  public void addDeleteUser() {
    assertFalse(database.haveUser(user));
    assertTrue(database.addUser(publicKey, user));
    assertTrue(database.haveUser(user));
    assertTrue(database.deleteUser(user));
    assertFalse(database.haveUser(user));
  }

  @Test
  public void deleteNotPresent() {
    assertFalse(database.deleteUser("non existant user".getBytes()));
  }

  @Test
  public void addTwice() {
    assertFalse(database.haveUser(user));
    assertTrue(database.addUser(publicKey, user));
    assertFalse(database.addUser(publicKey, user));
    assertTrue(database.haveUser(user));
    assertTrue(database.deleteUser(user));
    assertFalse(database.haveUser(user));
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }
}
