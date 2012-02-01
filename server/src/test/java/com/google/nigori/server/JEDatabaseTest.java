/*
 * Copyright (C) 2011 Daniel R. Thomas (drt24)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.nigori.server;

import java.io.File;

import org.junit.AfterClass;

/**
 * @author drt24
 * 
 */
public class JEDatabaseTest extends AbstractDatabaseTest {

  @Override
  protected Database getDatabase() {
    File dataDir = new File("je-test-dir/");
    dataDir.mkdir();
    return new JEDatabase(dataDir);
  }

  @AfterClass
  public static void deleteDatabase() {
    File dataDir = new File("je-test-dir/");
    if (dataDir.exists()){
      deleteDir(dataDir);
    }
  }

  /**
   * Deletes all files and subdirectories under dir. Returns true if all deletions were successful.
   * If a deletion fails, the method stops attempting to delete and returns false.
   * 
   * From http://www.exampledepot.com/egs/java.io/DeleteDir.html
   * 
   * @param dir
   * @return
   */
  public static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          return false;
        }
      }
    }

    // The directory is now empty so delete it
    return dir.delete();
  }
}
