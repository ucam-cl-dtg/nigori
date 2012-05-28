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

import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.nigori.common.Nonce;
import com.google.nigori.server.appengine.AppEngineDatabase;

/**
 * @author drt24
 * 
 */
public class AppEngineDatabaseTest extends AbstractDatabaseTest {

  private static LocalServiceTestHelper helper;

  @BeforeClass
  public static void initHelper() throws UnsupportedEncodingException {
    helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  }

  @Before
  public void setup() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Override
  protected Database getDatabase() {
    return new AppEngineDatabase();
  }

  @Test
  public void clearOldNonces() throws UserNotFoundException {
    database.clearOldNonces();
    Nonce oldNonce = new LyingNonce(0);
    Nonce freshNonce = new Nonce();
    database.addUser(publicKey,publicHash);
    User user = database.getUser(publicHash);
    try {
      assertTrue(database.checkAndAddNonce(oldNonce, publicHash));
      assertTrue(database.checkAndAddNonce(freshNonce, publicHash));
      database.clearOldNonces();
    } finally {
      database.deleteUser(user);
    }
  }

  private static class LyingNonce extends Nonce {
    private static final long serialVersionUID = 1L;

    public LyingNonce(int sinceEpoch) {
      super(sinceEpoch);
    }

    @Override
    public boolean isRecent() {
      return true;
    }
  }
}
