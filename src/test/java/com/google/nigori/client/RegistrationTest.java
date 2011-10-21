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
package com.google.nigori.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.appengine.tools.development.DevAppServer;
import com.google.appengine.tools.development.DevAppServerFactory;

/**
 * @author drt24
 * 
 */
public class RegistrationTest {

  public static final int PORT = 5005;
  public static final String HOST = "localhost";
  private static DevAppServer server;

  @BeforeClass
  public static void startDevServer() throws Exception {
    server = new DevAppServerFactory().createDevAppServer(new File("war"), HOST, PORT);
    server.start();
  }

  @AfterClass
  public static void stopDevServer() throws Exception {
    server.shutdown();
  }

  @Test
  public void test() throws NigoriCryptographyException, IOException {
    NigoriDatastore nigori = new NigoriDatastore(HOST, PORT, "");
    for (int i = 0; i < 3; ++i) {// check we can do this more than once
      assertTrue(nigori.register());
      assertTrue(nigori.authenticate());
      assertTrue(nigori.unregister());
      assertFalse(nigori.authenticate());
      assertFalse(nigori.unregister());
    }
  }

}
