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
package com.google.nigori.client.accept;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.google.nigori.client.NigoriCryptographyException;
import com.google.nigori.client.HTTPNigoriDatastore;

/**
 * @author drt24
 * 
 */
public class RegistrationTest {

  @Test
  public void test() throws NigoriCryptographyException, IOException {
    HTTPNigoriDatastore nigori = AcceptanceTests.getStore();
    for (int i = 0; i < AcceptanceTests.REPEAT; ++i) {// check we can do this more than once
      assertTrue("Not registered", nigori.register());
      assertTrue("Can't authenticate",nigori.authenticate());
      assertTrue("Can't unregister", nigori.unregister());
      assertFalse("Authenticated after unregistration", nigori.authenticate());
      assertFalse("Could re-unregister", nigori.unregister());
    }
  }

}
