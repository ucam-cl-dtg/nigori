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
package com.google.nigori.client;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.nigori.common.MessageLibrary;

/**
 * @author drt24
 * 
 */
public class PasswordGeneratorTest {

  private PasswordGenerator generator;

  @Before
  public void getGenerator() {
    generator = new PasswordGenerator();
  }

  @Test
  public void size() {
    byte[] password = generator.generate();
    assertTrue(password.length > 12);
    assertTrue(password.length < 64);
  }

  @Test
  public void repeat() {
    Set<String> set = new HashSet<String>();
    for (int i = 0; i < 1000; ++i) {
      // The conversion to String is much more expensive than we need as we just want something that
      // .equals works for.
      String password = MessageLibrary.bytesToString(generator.generate());
      assertTrue(set.add(password));// Not already in the set
    }
  }
}
