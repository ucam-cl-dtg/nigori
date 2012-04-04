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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.nigori.common.Index;
import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;
import com.google.nigori.common.UnauthorisedException;
import com.google.nigori.common.Util;

/**
 * @author drt24
 * 
 */
public class AsyncNigoriDatastoreWrapper implements AsyncNigoriDatastore {

  private static final int NUM_THREADS = 2;
  private NigoriDatastore store;
  private ExecutorService executor;

  public AsyncNigoriDatastoreWrapper(NigoriDatastore store) {
    this.store = store;
    this.executor = Executors.newFixedThreadPool(NUM_THREADS);
  }

  @Override
  public void finalize() throws Throwable {
    executor.shutdown();
  }

  @Override
  public boolean register() throws IOException, NigoriCryptographyException {
    return store.register();
  }

  @Override
  public boolean unregister() throws IOException, NigoriCryptographyException, UnauthorisedException {
    try {
      // Wait for the outstanding tasks to be completed before unregistering
      await(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      // Continue with unregistration
    }
    return store.unregister();
  }

  @Override
  public void authenticate(final AsyncCallback<Boolean> callback) {
    final Throwable from = new Throwable();
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.authenticate());
        } catch (Throwable t) {
          Util.addFrom(t, from);
          callback.onFailure(t);
        }
      }

    });
  }

  @Override
  public void getIndices(final AsyncCallback<List<Index>> callback) {
    final Throwable from = new Throwable();
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.getIndices());
        } catch (Throwable t) {
          Util.addFrom(t, from);
          callback.onFailure(t);
        }
      }
    });
  }

  @Override
  public void getRevision(final Index index, final Revision revision,
      final AsyncCallback<byte[]> callback) {
    final Throwable from = new Throwable();
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.getRevision(index, revision));
        } catch (Throwable t) {
          Util.addFrom(t, from);
          callback.onFailure(t);
        }
      }
    });
  }

  @Override
  public void put(final Index index, final Revision revision, final byte[] value,
      final AsyncCallback<Boolean> callback) {
    final Throwable from = new Throwable();
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.put(index, revision, value));
        } catch (Throwable t) {
          Util.addFrom(t, from);
          callback.onFailure(t);
        }
      }
    });
  }

  @Override
  public void get(final Index index, final AsyncCallback<List<RevValue>> callback) {
    final Throwable from = new Throwable();
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.get(index));
        } catch (Throwable t) {
          Util.addFrom(t, from);
          callback.onFailure(t);
        }
      }
    });

  }

  @Override
  public void getRevisions(final Index index, final AsyncCallback<List<Revision>> callback) {
    final Throwable from = new Throwable();
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.getRevisions(index));
        } catch (Throwable t) {
          Util.addFrom(t, from);
          callback.onFailure(t);
        }
      }
    });
  }

  @Override
  public void delete(final Index index, final byte[] token, final AsyncCallback<Boolean> callback) {
    final Throwable from = new Throwable();
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.delete(index, token));
        } catch (Throwable t) {
          Util.addFrom(t, from);
          callback.onFailure(t);
        }
      }
    });

  }

  @Override
  public void execute(Runnable command) {
    executor.execute(command);
  }

  /**
   * Wait until all the tasks currently in the executor are done or the timeout is exceeded
   * Point {@link #executor} to a new ExecutorService and then shut the old one down and wait until this is done.
   * @param timeout
   * @param unit
   * @throws InterruptedException
   */
  private void await(long timeout, TimeUnit unit) throws InterruptedException {
    
    synchronized (executor) {
      ExecutorService lexecutor = executor;
      executor = Executors.newFixedThreadPool(NUM_THREADS);
      lexecutor.shutdown();
      lexecutor.awaitTermination(timeout, unit);
    }
  }

}
