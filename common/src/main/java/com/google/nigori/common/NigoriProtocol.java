/*
 * Copyright (C) 2012 Daniel R. Thomas (drt24)
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
package com.google.nigori.common;

import java.io.IOException;

import com.google.nigori.common.NigoriMessages.AuthenticateRequest;
import com.google.nigori.common.NigoriMessages.DeleteRequest;
import com.google.nigori.common.NigoriMessages.GetIndicesRequest;
import com.google.nigori.common.NigoriMessages.GetIndicesResponse;
import com.google.nigori.common.NigoriMessages.GetRequest;
import com.google.nigori.common.NigoriMessages.GetResponse;
import com.google.nigori.common.NigoriMessages.GetRevisionsRequest;
import com.google.nigori.common.NigoriMessages.GetRevisionsResponse;
import com.google.nigori.common.NigoriMessages.PutRequest;
import com.google.nigori.common.NigoriMessages.RegisterRequest;
import com.google.nigori.common.NigoriMessages.UnregisterRequest;

/**
 * The messages and responses comprising the Nigori protocol
 * @author drt24
 * 
 */
public interface NigoriProtocol {

  boolean authenticate(AuthenticateRequest request) throws IOException;

  boolean register(RegisterRequest request) throws IOException;

  boolean unregister(UnregisterRequest request) throws IOException;

  GetResponse get(GetRequest request) throws IOException;

  GetIndicesResponse getIndices(GetIndicesRequest request) throws IOException;

  GetRevisionsResponse getRevisions(GetRevisionsRequest request) throws IOException;

  boolean put(PutRequest request) throws IOException;

  boolean delete(DeleteRequest request) throws IOException;
}
