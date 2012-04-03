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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.nigori.common.Index;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;

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
  public void register(final AsyncCallback<Boolean> callback) {
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.register());
        } catch (Throwable e) {
          callback.onFailure(e);
        }
      }

    });
  }

  @Override
  public void unregiser(final AsyncCallback<Boolean> callback) {
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.unregister());
        } catch (Throwable t) {
          callback.onFailure(t);
        }
      }

    });
  }

  @Override
  public void authenticate(final AsyncCallback<Boolean> callback) {
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.authenticate());
        } catch (Throwable t) {
          callback.onFailure(t);
        }
      }

    });
  }

  @Override
  public void getIndices(final AsyncCallback<List<Index>> callback) {
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.getIndices());
        } catch (Throwable t) {
          callback.onFailure(t);
        }
      }
    });
  }

  @Override
  public void getRevision(final Index index, final Revision revision,
      final AsyncCallback<byte[]> callback) {
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.getRevision(index, revision));
        } catch (Throwable t) {
          callback.onFailure(t);
        }
      }
    });
  }

  @Override
  public void put(final Index index, final Revision revision, final byte[] value,
      final AsyncCallback<Boolean> callback) {
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.put(index, revision, value));
        } catch (Throwable t) {
          callback.onFailure(t);
        }
      }
    });
  }

  @Override
  public void get(final Index index, final AsyncCallback<List<RevValue>> callback) {
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.get(index));
        } catch (Throwable t) {
          callback.onFailure(t);
        }
      }
    });

  }

  @Override
  public void getRevisions(final Index index, final AsyncCallback<List<Revision>> callback) {
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.getRevisions(index));
        } catch (Throwable t) {
          callback.onFailure(t);
        }
      }
    });
  }

  @Override
  public void delete(final Index index, final byte[] token, final AsyncCallback<Boolean> callback) {
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          callback.onSuccess(store.delete(index, token));
        } catch (Throwable t) {
          callback.onFailure(t);
        }
      }
    });

  }

}
