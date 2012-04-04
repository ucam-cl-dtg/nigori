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

import java.util.List;

import com.google.nigori.common.Index;
import com.google.nigori.common.Revision;

/**
 * An asynchronous version of {@link Datastore}
 * 
 * @author drt24
 * 
 */
public interface AsyncDatastore {

  void register(AsyncCallback<Boolean> callback);

  void unregiser(AsyncCallback<Boolean> callback);

  void authenticate(AsyncCallback<Boolean> callback);

  void getIndices(AsyncCallback<List<Index>> callback);

  void getRevision(Index index, Revision revision, AsyncCallback<byte[]> callback);

  /**
   * Use whatever service the AsyncNigoriDatastore is using to provide asynchronous services to run
   * command
   * 
   * @param command
   */
  void execute(Runnable command);
}
