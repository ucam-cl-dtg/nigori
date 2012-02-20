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

import java.io.IOException;
import java.util.List;

import com.google.nigori.common.Index;
import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.Revision;
import com.google.nigori.common.UnauthorisedException;

/**
 * Encapsulates common datastore functionality from {@link MigoriDatstore} and
 * {@link NigoriDatastore} mainly for user management.
 * 
 * @author drt24
 * 
 */
public interface Datastore {

  /**
   * Register the with the server.
   * 
   * @return true if the registration was successful; false otherwise.
   */
  boolean register() throws IOException, NigoriCryptographyException;

  /**
   * Unregister from the server.
   * 
   * @return true if the unregistration was successful; false otherwise.
   * @throws UnauthorisedException 
   */
  public boolean unregister() throws IOException, NigoriCryptographyException, UnauthorisedException;

  /**
   * Evaluate whether this NigoriDatastore can authenticate to the server.
   * 
   * @return true if the account is valid; false otherwise.
   */
  public boolean authenticate() throws IOException, NigoriCryptographyException;

  /**
   * @return
   * @throws NigoriCryptographyException
   * @throws IOException
   * @throws UnauthorisedException
   */
  List<Index> getIndices() throws NigoriCryptographyException, IOException, UnauthorisedException;

  /**
   * Retrieve the value for a particular index,revision pair
   * 
   * @param index
   * @param revision
   * @return the associated value or null if not present.
   * @throws NigoriCryptographyException
   * @throws IOException
   * @throws UnauthorisedException
   */
  public byte[] getRevision(Index index, Revision revision) throws IOException,
      NigoriCryptographyException, UnauthorisedException;
}
