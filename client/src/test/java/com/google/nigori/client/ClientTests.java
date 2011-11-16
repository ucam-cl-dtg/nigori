package com.google.nigori.client;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.google.nigori.client.accept.AcceptanceTests;

/**
 * Tests which are expected to pass for client
 * @author drt24
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses ({
  KeyManagerTest.class,
  AcceptanceTests.class
  })
public class ClientTests {

}
