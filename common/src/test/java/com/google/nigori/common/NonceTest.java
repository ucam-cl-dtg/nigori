package com.google.nigori.common;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NonceTest {

  @Test
  public void sinceEpochInPast() {
    Nonce n = new Nonce();
    assertTrue(n.getSinceEpoch() <= System.currentTimeMillis()/1000);
  }
  @Test
  public void sinceEpochRecent() {
    Nonce n = new Nonce();
    assertTrue(n.getSinceEpoch() > (System.currentTimeMillis()/1000) -1);
  }
}
