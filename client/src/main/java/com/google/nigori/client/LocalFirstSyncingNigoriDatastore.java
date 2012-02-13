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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.google.nigori.common.Index;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;
import com.google.nigori.common.UnauthorisedException;

/**
 * Prefer the first store to the second and so if the second is inaccessible still keep going using
 * the first, when we notice the second come back then ensure that we have got everything synced
 * 
 * @author drt24
 * 
 */
public class LocalFirstSyncingNigoriDatastore extends SyncingNigoriDatastore {

  protected boolean noRemote = true;// Before we started we were not connected
  private static final Logger log = Logger.getLogger(LocalFirstSyncingNigoriDatastore.class
      .getName());

  /**
   * @param first
   * @param second
   * @throws IOException
   * @throws NigoriCryptographyException
   */
  public LocalFirstSyncingNigoriDatastore(NigoriDatastore first, NigoriDatastore second)
      throws IOException, NigoriCryptographyException {
    super(first, second);
  }

  /**
   * When we have a problem talking to the second store we assume it is broken and hence may have
   * missed some of our operations. Later when it works again {@link #ensureSynced()} should fix
   * things up for us.
   * 
   * @param e
   */
  private synchronized void ioException(IOException e) {
    log.warning(e.toString());
    noRemote = true;
    // TODO(drt24): trigger a thread to retry doing an exponential backoff
  }

  /**
   * Call this when we successfully did something on the second store so we think we can talk to it.
   * 
   * @throws NigoriCryptographyException
   */
  private synchronized void ensureSynced() throws NigoriCryptographyException {
    try {
      if (noRemote) {
        boolean canAuth = first.authenticate();
        if (canAuth) {
          syncAll();
        }
        noRemote = false;
      }
    } catch (IOException e) {
      log.warning(e.toString());
    } catch (UnauthorisedException e) {
      log.severe(e.toString());
    }
  }

  @Override
  public boolean authenticate() throws IOException, NigoriCryptographyException {
    boolean firstAuth = first.authenticate();
    boolean secondAuth = true;
    try {
      secondAuth = second.authenticate();
      ensureSynced();
    } catch (IOException e) {
      ioException(e);
    }
    return firstAuth && secondAuth;
  }

  @Override
  public boolean put(Index index, Revision revision, byte[] value) throws IOException,
      NigoriCryptographyException, UnauthorisedException {
    boolean firstPut = first.put(index, revision, value);
    boolean secondPut = true;
    try {
      secondPut = second.put(index, revision, value);
      ensureSynced();
    } catch (IOException e) {
      ioException(e);
    }
    return firstPut && secondPut;
  }

  @Override
  public List<Index> getIndices() throws NigoriCryptographyException, IOException,
      UnauthorisedException {
    List<Index> firstIndices = first.getIndices();
    List<Index> secondIndices = new ArrayList<Index>();
    try {
      secondIndices = second.getIndices();
      ensureSynced();
    } catch (IOException e) {
      ioException(e);
    }
    secondIndices.removeAll(firstIndices);
    firstIndices.addAll(secondIndices);// TODO(drt24) do a proper union and sync here
    return firstIndices;
  }

  @Override
  public List<RevValue> get(Index index) throws IOException, NigoriCryptographyException,
      UnauthorisedException {
    List<RevValue> firstRevVals = first.get(index);
    List<RevValue> secondRevVals = null;
    try {
      secondRevVals = second.get(index);
      ensureSynced();
    } catch (IOException e) {
      ioException(e);
    }
    if (firstRevVals == null) {
      if (secondRevVals == null) {
        return null;
      } else {
        return secondRevVals;
      }
    }
    if (secondRevVals == null) {
      return firstRevVals;
    }
    secondRevVals.removeAll(firstRevVals);
    firstRevVals.addAll(secondRevVals);// TODO(drt24) do a proper union and sync here
    return firstRevVals;
  }

  @Override
  public byte[] getRevision(Index index, Revision revision) throws IOException,
      NigoriCryptographyException, UnauthorisedException {
    byte[] firstValue = first.getRevision(index, revision);
    try {
      byte[] secondValue = second.getRevision(index, revision);
      if (!Arrays.equals(firstValue, secondValue)) {
        throw new IOException("Stores returned different values for the same revision");
      }
      ensureSynced();

    } catch (IOException e) {
      ioException(e);
    }

    return firstValue;
  }

  @Override
  public List<Revision> getRevisions(Index index) throws NigoriCryptographyException, IOException,
      UnauthorisedException {
    List<Revision> firstRevisions = first.getRevisions(index);
    List<Revision> secondRevisions = null;
    try {
      secondRevisions = second.getRevisions(index);
      ensureSynced();
    } catch (IOException e) {
      ioException(e);
    }
    if (firstRevisions == null) {
      if (secondRevisions == null) {
        return null;
      } else {
        return secondRevisions;
      }
    }
    if (secondRevisions == null) {
      return firstRevisions;
    }
    secondRevisions.removeAll(firstRevisions);
    firstRevisions.addAll(secondRevisions);// TODO(drt24) do a proper union and sync here
    return firstRevisions;
  }

  // TODO(drt24) since deletes won't sync properly at the moment we require both stores to work
  @Override
  public boolean delete(Index index, byte[] token) throws NigoriCryptographyException, IOException,
      UnauthorisedException {
    boolean firstAt = first.authenticate();
    boolean secondAuth = second.authenticate();
    if (!(firstAt & secondAuth)) {
      return false;// can't delete from both atm
    }
    boolean firstDel = first.delete(index, token);
    boolean secondDel = second.delete(index, token);
    return firstDel & secondDel;
  }
}
