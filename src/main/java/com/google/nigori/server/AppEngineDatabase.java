/*
 * Copyright (C) 2011 Alastair R. Beresford
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

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

public final class AppEngineDatabase implements Database {

  @SuppressWarnings("unused")
  private static final PersistenceManagerFactory pmfInstance = JDOHelper
      .getPersistenceManagerFactory("transactions-optional");

  @Override
  public boolean addUser(byte[] authority, byte[] newUser) {

    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean haveUser(byte[] existingUser) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean deleteUser(byte[] authority, byte[] existingUser) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public byte[] getRecord(byte[] authority, byte[] key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean putRecord(byte[] authority, byte[] key, byte[] data) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean updateRecord(byte[] authority, byte[] key, byte[] data) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean deleteRecord(byte[] authority, byte[] key) {
    // TODO Auto-generated method stub
    return false;
  }

  	/**
  	 * Given a NewRequest object, insert data into the datastore and report status back to client
  	 *
  	 * @param resp
  	 * @param r
  	private void handleNewRecord(HttpServletResponse resp, NewRequest r) {

  		PersistenceManager pm = PMF.get().getPersistenceManager();
  		try {
  			Record old = new Record("old record".getBytes(), null, null);
  			Record new1 = new Record("...split into two parts".getBytes(), null, old);
  			Record new2 = new Record("a new record which is ...".getBytes(), new1, old);

  			//Recursively writes out new1 and old since these are referenced as children inside new2
  			pm.makePersistentAll(new2);

  			System.out.println("Check: " + new2.getKey().getId() +
  					" == "+ new1.getKey().getParent().getId());

  			//return ""+new2.getKey().getId();

  		} finally {
  			pm.close();
  		}		
  	}
  	 */
}