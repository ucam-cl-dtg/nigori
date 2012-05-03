/*
 * Copyright (C) 2012 Daniel R. Thomas (drt24).
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
package com.google.nigori.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NonceTest {

  @Test
  public void sinceEpochInPast() {
    Nonce n = new Nonce();
    final int now = (int) (System.currentTimeMillis() / 1000);
    assertTrue(n.getSinceEpoch() <= now);
  }

  @Test
  public void sinceEpochRecent() {
    Nonce n = new Nonce();
    final int now = (int) (System.currentTimeMillis() / 1000);
    assertTrue(n.getSinceEpoch() > now - 1);
  }

  @Test
  public void freshNonceRecent() {
    Nonce n = new Nonce();
    assertTrue(n.isRecent());
  }

  @Test
  public void longAgoNotRecent() {
    final int now = (int) (System.currentTimeMillis() / 1000);
    assertFalse(Nonce.isRecent(0));
    assertFalse(Nonce.isRecent(now - 60 * 60 * 24 * 365));
  }

  @Test
  public void distantFutureNotRecent() {
    final int now = (int) (System.currentTimeMillis() / 1000);
    assertFalse(Nonce.isRecent(Integer.MAX_VALUE));
    assertFalse(Nonce.isRecent(now + 60 * 60 * 24 * 365));
    assertFalse(Nonce.isRecent(now / 1000 + 60 * 60 * 24));
  }

  @Test
  public void recentIsRecent() {
    final int now = (int) (System.currentTimeMillis() / 1000);
    assertTrue(Nonce.isRecent(now - 30));
    assertTrue(Nonce.isRecent(now - 30 * 60));
  }

  @Test
  public void verySoonIsRecent() {
    final int now = (int) (System.currentTimeMillis() / 1000);
    assertTrue(Nonce.isRecent(now + 30));
  }
}
