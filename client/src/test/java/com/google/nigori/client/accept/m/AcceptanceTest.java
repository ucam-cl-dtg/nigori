/*
 * Copyright (C) 2012 Daniel Thomas (drt24)
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
package com.google.nigori.client.accept.m;

import java.io.IOException;

import com.google.nigori.client.HTTPNigoriDatastore;
import com.google.nigori.client.HashMigoriDatastore;
import com.google.nigori.client.MigoriDatastore;
import com.google.nigori.client.NigoriCryptographyException;
import com.google.nigori.client.accept.AcceptanceTests;

/**
 * @author drt24
 *
 */
public class AcceptanceTest {

  protected MigoriDatastore getStore() throws NigoriCryptographyException, IOException {
    return new HashMigoriDatastore(new HTTPNigoriDatastore(AcceptanceTests.HOST, AcceptanceTests.PORT, AcceptanceTests.PATH));
  }
}
