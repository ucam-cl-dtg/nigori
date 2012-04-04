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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.nigori.common.Index;
import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;
import com.google.nigori.common.UnauthorisedException;
import com.google.nigori.common.Util;

/**
 * TODO(drt24) Potentially we want to make sure calls don't ever block on talking to the remote -
 * currently they do if we think that the local store is missing something the remote has
 * 
 * @author drt24
 * 
 */
public class LocalAsyncRemoteSyncingNigoriDatastore implements NigoriDatastore {

  protected final NigoriDatastore local;
  protected final NigoriDatastore synchronousRemote;
  protected final AsyncNigoriDatastore remote;
  private final Logger log = Logger.getLogger(this.getClass().getCanonicalName());

  private void crypto(NigoriCryptographyException e) {
    log.log(Level.SEVERE, "", e);
  }

  private void unauthorised(UnauthorisedException e) {
    log.log(Level.WARNING, "", e);
  }

  private void unexpected(Throwable t) {
    log.log(Level.SEVERE, "Unexpected exception", t);
  }

  private void failure(Throwable caught) {
    if (caught instanceof IOException) {
      ioException((IOException) caught);
    } else if (caught instanceof NigoriCryptographyException) {
      crypto((NigoriCryptographyException) caught);
    } else if (caught instanceof UnauthorisedException) {
      unauthorised((UnauthorisedException) caught);
    } else {
      unexpected(caught);
    }
  }

  /**
   * When we have a problem talking to the second store we assume it is broken and hence may have
   * missed some of our operations. Later when it works again {@link #ensureSynced()} should fix
   * things up for us.
   * 
   * @param e
   */
  private synchronized void ioException(IOException e) {
    log.log(Level.WARNING, "", e);
    noRemote = true;
  }

  public LocalAsyncRemoteSyncingNigoriDatastore(NigoriDatastore local, NigoriDatastore remote) {
    if (local == null || remote == null) {
      throw new IllegalArgumentException("Datastores must not be null");
    }
    this.local = local;
    this.synchronousRemote = remote;
    this.remote = new AsyncNigoriDatastoreWrapper(remote);
  }

  /**
   * Attempts to register to both stores, if one fails the other may still succeed and false will be
   * returned
   */
  @Override
  public boolean register() throws IOException, NigoriCryptographyException {
    boolean remoteReg = remote.register();
    boolean localReg = local.register();
    return localReg && remoteReg;
  }

  /**
   * Attempts to unregister from both stores, if one fails the other may still succeed and false
   * will be returned
   * 
   * @throws UnauthorisedException
   */
  @Override
  public boolean unregister() throws IOException, NigoriCryptographyException,
      UnauthorisedException {
    boolean remoteReg = remote.unregister();
    boolean localReg = local.unregister();

    return localReg && remoteReg;
  }

  @Override
  public boolean authenticate() throws IOException, NigoriCryptographyException {
    final boolean localAuth = local.authenticate();
    remote.authenticate(new FAsyncCallback<Boolean>() {

      @Override
      public void success(Boolean result) {
        if (localAuth != (boolean) result) {
          log.warning(String.format("local and remote authenticate() differ: %b and %b", localAuth,
              result));
        }
      }
    });
    return localAuth;
  }

  protected boolean noRemote = true;// Before we started we were not connected

  private final Object syncLock = new Object();
  private boolean syncing = false;

  /**
   * Call this when we successfully did something on the second store so we think we can talk to it.
   * 
   * @throws NigoriCryptographyException
   */
  private synchronized void ensureSynced() {
    if (noRemote) {
      synchronized (syncLock) {
        if (!syncing) {
          syncing = true;
          final Throwable from = new Throwable();
          remote.execute(new Runnable() {

            @Override
            public void run() {
              boolean canAuth;
              try {
                canAuth = synchronousRemote.authenticate();

                if (canAuth) {
                  syncAll();
                }
              } catch (Exception e) {
                Util.addFrom(e, from);
                failure(e);
              }
              synchronized (syncLock) {
                syncing = false;
              }
            }
          });
        }
      }
      noRemote = false;
    }
  }

  /**
   * Synchronise all indices and revisions between both stores
   * 
   * @throws NigoriCryptographyException
   * @throws IOException
   * @throws UnauthorisedException
   */
  public synchronized void syncAll() throws IOException, NigoriCryptographyException,
      UnauthorisedException {
    List<Index> firstIndices = local.getIndices();
    List<Index> secondIndices = synchronousRemote.getIndices();
    if (!(firstIndices.containsAll(secondIndices) && secondIndices.containsAll(firstIndices))) {
      // indices not already synced
      List<Index> firstMSecond = new ArrayList<Index>(firstIndices);
      firstMSecond.removeAll(secondIndices);
      addAllIndices(firstMSecond, local, synchronousRemote);
      List<Index> secondMFirst = new ArrayList<Index>(secondIndices);
      secondMFirst.removeAll(firstIndices);
      addAllIndices(secondMFirst, synchronousRemote, synchronousRemote);
      // make sure firstIndices has all the indices
      firstIndices.addAll(secondMFirst);
    }
    // Now need to sync revisions.
    List<Index> indices = firstIndices;
    for (Index index : indices) {
      syncRevisions(index);
    }
  }

  /**
   * Synchronise the revisions for a particular index
   * 
   * @param index
   * @throws IOException
   * @throws NigoriCryptographyException
   * @throws UnauthorisedException
   */
  private void syncRevisions(Index index) throws NigoriCryptographyException, IOException,
      UnauthorisedException {
    List<Revision> localRevisions = local.getRevisions(index);
    List<Revision> remoteRevisions = synchronousRemote.getRevisions(index);
    if (localRevisions == null) {
      if (remoteRevisions != null) {
        addAllRevisions(index, remoteRevisions, synchronousRemote, local);
      }// else both null
    } else if (remoteRevisions == null) {
      // localRevisions != null
      addAllRevisions(index, localRevisions, local, synchronousRemote);
    } else if (!(localRevisions.containsAll(remoteRevisions) && remoteRevisions
        .containsAll(localRevisions))) {
      // revisions not already synced
      List<Revision> firstMSecond = new ArrayList<Revision>(localRevisions);
      firstMSecond.removeAll(remoteRevisions);
      addAllRevisions(index, firstMSecond, local, synchronousRemote);
      List<Revision> secondMFirst = new ArrayList<Revision>(remoteRevisions);
      secondMFirst.removeAll(localRevisions);
      addAllRevisions(index, secondMFirst, synchronousRemote, local);
    }
  }

  private static void addAllRevValues(Index index, List<RevValue> revValues, NigoriDatastore to)
      throws IOException, NigoriCryptographyException, UnauthorisedException {
    for (RevValue rv : revValues) {
      to.put(index, rv.getRevision(), rv.getValue());
    }
  }

  /**
   * Add all the revisions for a particular index from one NigoriDatastore to another
   * 
   * @param revisions
   * @param from
   * @param to
   * @throws NigoriCryptographyException
   * @throws IOException
   * @throws UnauthorisedException
   */
  private static void addAllRevisions(Index index, List<Revision> revisions, NigoriDatastore from,
      NigoriDatastore to) throws IOException, NigoriCryptographyException, UnauthorisedException {
    for (Revision revision : revisions) {
      byte[] value = from.getRevision(index, revision);
      if (value != null) {
        to.put(index, revision, value);
      }
    }
  }

  /**
   * Add all the indices from one NigoriDatastore to another
   * 
   * @param indices
   * @param from
   * @param to
   * @throws NigoriCryptographyException
   * @throws IOException
   * @throws UnauthorisedException
   */
  private static void addAllIndices(List<Index> indices, NigoriDatastore from, NigoriDatastore to)
      throws IOException, NigoriCryptographyException, UnauthorisedException {
    for (Index index : indices) {
      List<RevValue> revs = from.get(index);
      if (revs != null) {
        for (RevValue rev : revs) {
          to.put(index, rev.getRevision(), rev.getValue());
        }
      }
    }
  }

  private void ensureIndicesSynced(List<Index> localIndices, List<Index> remoteIndices)
      throws IOException, NigoriCryptographyException, UnauthorisedException {
    if (!(localIndices.containsAll(remoteIndices) && remoteIndices.containsAll(localIndices))) {
      // indices not already synced
      List<Index> remoteMLocal = new ArrayList<Index>(remoteIndices);
      remoteMLocal.removeAll(localIndices);
      addAllIndices(remoteMLocal, synchronousRemote, local);

      List<Index> localMRemote = new ArrayList<Index>(localIndices);
      localMRemote.removeAll(remoteIndices);
      addAllIndices(localMRemote, local, synchronousRemote);
    }
  }

  @Override
  public List<Index> getIndices() throws NigoriCryptographyException, IOException,
      UnauthorisedException {
    final List<Index> localIndices = local.getIndices();
    if (localIndices.size() == 0) {
      try {
        final List<Index> remoteIndices = synchronousRemote.getIndices();
        if (remoteIndices.size() != 0) {
          remote.execute(new Runnable() {

            @Override
            public void run() {

              try {
                addAllIndices(remoteIndices, synchronousRemote, local);
              } catch (Exception e) {
                failure(e);
              }

            }
          });
          return remoteIndices;
        }
      } catch (IOException e) {
        ioException(e);
      }
    } else {
      remote.getIndices(new FAsyncCallback<List<Index>>() {

        @Override
        public void success(List<Index> result) throws IOException, NigoriCryptographyException,
            UnauthorisedException {
          ensureIndicesSynced(localIndices, result);
          ensureSynced();
        }

      });
    }
    return localIndices;
  }

  @Override
  public byte[] getRevision(final Index index, final Revision revision) throws IOException,
      NigoriCryptographyException, UnauthorisedException {
    final byte[] localValue = local.getRevision(index, revision);
    if (localValue == null) {
      byte[] remoteValue = synchronousRemote.getRevision(index, revision);
      if (remoteValue != null) {
        if (!local.put(index, revision, remoteValue)) {
          log.warning("Failed to put remote value into local store");
        }
        return remoteValue;
      } else {
        return null;
      }
    } else {
      // Now we need to check that the value we have locally is also in the remote
      remote.getRevision(index, revision, new FAsyncCallback<byte[]>() {

        @Override
        public void success(byte[] result) throws IOException, NigoriCryptographyException,
            UnauthorisedException {
          if (result == null) {
            synchronousRemote.put(index, revision, localValue);
          } else if (!Arrays.equals(localValue, result)) {
            log.severe("Local and remote have different values for the same revision (" + revision
                + ")- we can't reconcile that");
          }
        }
      });
      return localValue;
    }
  }

  @Override
  public boolean put(Index index, Revision revision, byte[] value) throws IOException,
      NigoriCryptographyException, UnauthorisedException {
    final boolean localPut = local.put(index, revision, value);
    remote.put(index, revision, value, new BAsyncCallback("put", localPut));

    return localPut;
  }

  @Override
  public List<RevValue> get(final Index index) throws IOException, NigoriCryptographyException,
      UnauthorisedException {
    final List<RevValue> localGet = local.get(index);
    remote.get(index, new FAsyncCallback<List<RevValue>>() {

      @Override
      public void success(List<RevValue> result) throws IOException, NigoriCryptographyException,
          UnauthorisedException {
        if (localGet == null) {
          if (result != null) {
            addAllRevValues(index, result, local);
          }// else equal
        } else {
          if (result != null) {
            List<RevValue> localMRemote = new ArrayList<RevValue>(localGet);
            localMRemote.removeAll(result);
            List<RevValue> remoteMLocal = new ArrayList<RevValue>(result);
            remoteMLocal.removeAll(localGet);
            if (!remoteMLocal.isEmpty()) {
              addAllRevValues(index, remoteMLocal, local);
            }
            if (!localMRemote.isEmpty()) {
              addAllRevValues(index, localMRemote, synchronousRemote);
            }
          } else {
            addAllRevValues(index, localGet, synchronousRemote);
          }
        }
      }
    });

    return localGet;
  }

  @Override
  public List<Revision> getRevisions(final Index index) throws NigoriCryptographyException,
      IOException, UnauthorisedException {
    final List<Revision> localRevisions = local.getRevisions(index);
    remote.getRevisions(index, new FAsyncCallback<List<Revision>>() {

      @Override
      public void success(List<Revision> result) throws IOException, NigoriCryptographyException,
          UnauthorisedException {
        if (localRevisions == null) {
          if (result != null) {
            addAllRevisions(index, result, synchronousRemote, local);
          } // else equal
        } else {
          if (result != null) {
            List<Revision> localMRemote = new ArrayList<Revision>(localRevisions);
            localMRemote.removeAll(result);
            List<Revision> remoteMLocal = new ArrayList<Revision>(result);
            remoteMLocal.removeAll(localRevisions);
            if (!remoteMLocal.isEmpty()) {
              addAllRevisions(index, remoteMLocal, synchronousRemote, local);
            }
            if (!localMRemote.isEmpty()) {
              addAllRevisions(index, localMRemote, local, synchronousRemote);
            }
          } else {
            addAllRevisions(index, localRevisions, local, synchronousRemote);
          }
        }
      }
    });
    return localRevisions;
  }

  @Override
  public boolean delete(Index index, byte[] token) throws NigoriCryptographyException, IOException,
      UnauthorisedException {
    final boolean localDelete = local.delete(index, token);
    remote.delete(index, token, new BAsyncCallback("delete", localDelete));
    return localDelete;
  }

  /**
   * Log onFailure and exceptions thrown by {@link #success(T)}
   * 
   * @author drt24
   * 
   * @param <T>
   */
  protected abstract class FAsyncCallback<T> implements AsyncCallback<T> {

    @Override
    public void onFailure(Throwable caught) {
      failure(caught);
    }

    @Override
    public void onSuccess(T result) {
      try {
        success(result);
      } catch (Exception e) {
        failure(e);
      }
    }

    public abstract void success(T result) throws IOException, NigoriCryptographyException,
        UnauthorisedException;

  }
  protected class BAsyncCallback extends FAsyncCallback<Boolean> {

    protected String method;
    protected boolean localResult;

    public BAsyncCallback(String method, boolean localResult) {
      this.method = method;
      this.localResult = localResult;
    }

    @Override
    public void success(Boolean result) throws IOException, NigoriCryptographyException,
        UnauthorisedException {
      if (localResult != (boolean) result) {
        log.warning(String.format("local and remote " + method + " differ: %b and %b", localResult,
            result));
      } else {
        ensureSynced();
      }
    }
  }

}
