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
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;
import com.google.nigori.common.UnauthorisedException;

/**
 * Nigori datastore which performs the relevant encryption and communicates with nigori server(s)
 * which store the data.
 * 
 * @author drt24
 * 
 */
public interface NigoriDatastore extends Datastore {

  /**
   * Insert a new index-revision-value pair into the datastore of the server.
   * 
   * @param index the index
   * @param revision the revision
   * @param value the data value associated with the index and revision.
   * @return true if the data was successfully inserted; false otherwise.
   * @throws NigoriCryptographyException
   * @throws UnauthorisedException 
   */
  public boolean put(Index index, Revision revision, byte[] value) throws IOException,
      NigoriCryptographyException, UnauthorisedException;

  /**
   * Retrieve the revision-values associated with {@code index} on the server.
   * 
   * @param index
   * @return a List of revision-values containing the data associated with {@code index} or {@code null} if no data
   *         exists.
   * @throws UnauthorisedException 
   */
  public List<RevValue> get(Index index) throws IOException, NigoriCryptographyException, UnauthorisedException;

  /**
   * Get the revisions for a particular index
   * @param index
   * @return a List of revisions for the index or null if the index does not exist.
   * @throws NigoriCryptographyException
   * @throws IOException
   * @throws UnauthorisedException 
   */
  public List<Revision> getRevisions(Index index) throws NigoriCryptographyException, IOException, UnauthorisedException;

  /**
   * Delete the index (and associated revisions and values) on the server
   * 
   * @param index
   * @return true if the deletion was successful; false if no such index was found or a server error
   *         occurred.
   * @throws IOException
   * @throws NigoriCryptographyException
   * @throws UnauthorisedException 
   */
  public boolean delete(Index index, byte[] token) throws NigoriCryptographyException, IOException, UnauthorisedException;

}
