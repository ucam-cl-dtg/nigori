/*
 * Copyright (C) 2011 Daniel Thomas (drt24)
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
package com.google.nigori.client.accept;

import org.junit.Test;

import com.google.nigori.client.CommandLineExample;

/**
 * @author drt24
 *
 */
public class CommandLineExampleTest {

  @Test
  public void registerSetGetDeleteUnregister() throws Exception {
    String username = "username";
    String password = "password";
    String index = "index";
    String value = "value";
    CommandLineExample.main(new String[]{AcceptanceTests.HOST,"" +AcceptanceTests.PORT, "register", username, password});
    CommandLineExample.main(new String[]{AcceptanceTests.HOST,"" +AcceptanceTests.PORT, "authenticate", username, password});
    CommandLineExample.main(new String[]{AcceptanceTests.HOST,"" +AcceptanceTests.PORT, "put", username, password, index, value});
    CommandLineExample.main(new String[]{AcceptanceTests.HOST,"" +AcceptanceTests.PORT, "get", username, password, index});
    CommandLineExample.main(new String[]{AcceptanceTests.HOST,"" +AcceptanceTests.PORT, "delete", username, password, index});
    CommandLineExample.main(new String[]{AcceptanceTests.HOST,"" +AcceptanceTests.PORT, "unregister", username, password});
  }
}
