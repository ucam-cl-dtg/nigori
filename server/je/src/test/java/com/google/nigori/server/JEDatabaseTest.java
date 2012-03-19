/*
 * Copyright (C) 2011 Daniel R. Thomas (drt24)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Test;

import com.google.nigori.common.Util;

/**
 * @author drt24
 * 
 */
public class JEDatabaseTest extends AbstractDatabaseTest {

  @Override
  protected Database getDatabase() {
    File dataDir = new File("je-test-dir/");
    dataDir.mkdir();
    dataDir.deleteOnExit();
    return JEDatabase.getInstance(dataDir);
  }

  @Test
  public void getInstance() throws UserNotFoundException {
    Database newDatabase = getDatabase();
    assertEquals("Get same database", database, newDatabase);
    assertTrue(database.addUser(publicKey));
    User user = newDatabase.getUser(publicKey);
    assertNotNull(user);
    assertTrue(database.deleteUser(user));
  }

  @AfterClass
  public static void deleteDatabase() {
    File dataDir = new File("je-test-dir/");
    if (dataDir.exists()) {
      Util.deleteDir(dataDir);
      dataDir.delete();
    }
  }
}
