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
import java.util.Collection;

import com.google.nigori.common.Index;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;
import com.google.nigori.common.UnauthorisedException;

/**
 * An index-value store which provides merging and synchronisation support through tracking revision
 * history.
 * 
 * @author drt24
 * 
 */
public interface MigoriDatastore extends Datastore {

  /**
   * Set the value for the index to be deleted but preserve all history - this is safe delete in
   * that it can be undone.
   * 
   * @param index the index
   * @param parents the parent revisions
   * @return the RevValue which represents the deleted value and the revision that points to it
   */
  RevValue deleteValue(Index index, RevValue... parents);

  /**
   * Get the head revision for the index, the merger automatically merges multiple heads if there
   * are multiple and the merged head is put before being returned.
   * 
   * @param index the index
   * @param merger the code to automatically merge the heads of any branches of history together
   * @return the single head revision for all current history or null if there are no values for this index
   * @throws IOException 
   * @throws NigoriCryptographyException 
   * @throws UnauthorisedException 
   */
  RevValue getHead(Index index, MigoriMerger merger) throws NigoriCryptographyException, IOException, UnauthorisedException;

  /**
   * Get all the current heads of branches of history for this index.
   * 
   * @see #getHead(Index, MigoriMerger) better to use this where you can as it ensures the number of
   *      branches of history does not grow.
   * @param index the index
   * @return a collection of the heads of the current branches of history or null if this index has no values
   * 
   *         You MUST NOT assume that this collection with have a size of one.
   * @throws IOException 
   * @throws NigoriCryptographyException 
   * @throws UnauthorisedException 
   */
  Collection<RevValue> get(Index index) throws NigoriCryptographyException, IOException, UnauthorisedException;

  /**
   * Put the value for the index specifying that its parents are parents. The parents will be made
   * the parent revisions of value i.e. this represents of a merge of the parents to produce value.
   * 
   * @param index the index
   * @param value the value
   * @param parents the parent revisions
   * @return the value that was put along with its revision.
   * @throws NigoriCryptographyException 
   * @throws IOException 
   * @throws UnauthorisedException 
   */
  RevValue put(Index index, byte[] value, RevValue... parents) throws IOException, NigoriCryptographyException, UnauthorisedException;

  /**
   * Permanently delete all information about an index storing only the revision at which it was
   * deleted to allow synchronisation between different data stores.
   * 
   * @param index
   * @param position the head revision at which this index is being deleted, there must only be one
   *          head at this point or it will be very hard to do synchronisation.
   * @return
   * @throws IOException 
   * @throws NigoriCryptographyException 
   * @throws UnauthorisedException 
   */
  boolean deleteIndex(Index index, Revision position) throws NigoriCryptographyException, IOException, UnauthorisedException;

  /**
   * Get the whole history for an index as a DAG
   * 
   * @param index
   * @return a DAG representing the history for index or null if there is none
   * @throws IOException 
   * @throws NigoriCryptographyException 
   * @throws UnauthorisedException 
   */
  DAG<Revision> getHistory(Index index) throws NigoriCryptographyException, IOException, UnauthorisedException;

  /**
   * Merges together the head revisions of existing branches of history to produce one single
   * current head revision. This will automatically be put before
   * {@link #getHead(Index,MigoriMerger)} returns.
   * 
   * @see #getHead(Index,MigoriMerger)
   * @author drt24
   * 
   */
  public interface MigoriMerger {
    /**
     * 
     * @param store the store we are merging for
     * @param index the index we are merging for
     * @param heads the current heads of the branches of history
     * @return the RevValue which has been put into the store and dominates all the heads.
     * @throws NigoriCryptographyException 
     * @throws IOException 
     * @throws UnauthorisedException 
     */
    RevValue merge(MigoriDatastore store, Index index, Collection<RevValue> heads) throws IOException, NigoriCryptographyException, UnauthorisedException;
  }
}
