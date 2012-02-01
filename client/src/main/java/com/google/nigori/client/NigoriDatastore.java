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

import com.google.nigori.common.RevValue;

/**
 * Nigori datastore which performs the relevant encryption and communicates with nigori server(s) which store the data.
 * @author drt24
 * 
 */
public interface NigoriDatastore {

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
   */
  public boolean unregister() throws IOException, NigoriCryptographyException;

  /**
   * Evaluate whether this NigoriDatastore can authenticate to the server.
   * 
   * @return true if the account is valid; false otherwise.
   */
  public boolean authenticate() throws IOException, NigoriCryptographyException;

  /**
   * Insert a new index-revision-value pair into the datastore of the server.
   * 
   * @param index the index
   * @param revision the revision
   * @param value the data value associated with the index and revision.
   * @return true if the data was successfully inserted; false otherwise.
   * @throws NigoriCryptographyException
   */
  public boolean put(byte[] index, byte[] revision, byte[] value) throws IOException,
      NigoriCryptographyException;

  /**
   * Retrieve the revision-values associated with {@code index} on the server.
   * 
   * @param index
   * @return a List of revision-values containing the data associated with {@code index} or {@code null} if no data
   *         exists.
   */
  public List<RevValue> get(byte[] index) throws IOException, NigoriCryptographyException;

  /**
   * Retrieve the value for a particular index,revision pair
   * @param index
   * @param revision
   * @return the associated value or null if not present.
   * @throws NigoriCryptographyException
   * @throws IOException
   */
  public byte[] getRevision(byte[] index, byte[] revision) throws IOException,
      NigoriCryptographyException;

  /**
   * Get the revisions for a particular index
   * @param index
   * @return a List of revisions for the index or null if the index does not exist.
   * @throws NigoriCryptographyException
   * @throws IOException
   */
  public List<byte[]> getRevisions(byte[] index) throws NigoriCryptographyException, IOException;

  /**
   * Delete the index (and associated revisions and values) on the server
   * 
   * @param index
   * @return true if the deletion was successful; false if no such index was found or a server error
   *         occurred.
   * @throws IOException
   * @throws NigoriCryptographyException
   */
  public boolean delete(byte[] index) throws NigoriCryptographyException, IOException;
}
