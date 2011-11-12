package com.google.nigori.client;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.google.nigori.client.accept.RegistrationTest;

/**
 * Tests which are expected to fail for client
 * @author drt24
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses ({RegistrationTest.class})
public class FailingClientTests {

}
