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
package com.google.nigori.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An abstract database which ensures that {@link #clearOldNonces()} is called with an hour between
 * each call.
 * 
 * @author drt24
 * 
 */
public abstract class AbstractDatabase implements Database {

  private final ScheduledExecutorService scheduler;
  private final ScheduledFuture<?> cleaner;

  public AbstractDatabase() {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    cleaner = scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        clearOldNonces();
      }
    }, 0, 1, TimeUnit.HOURS);
  }

  @Override
  public void finalize() throws Throwable {
    cleaner.cancel(false);
    scheduler.shutdown();
    super.finalize();
  }
}
