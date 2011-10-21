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

import java.util.Date;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public final class AppEngineDatabase implements Database {

  // private static final String USERSKEY = "users";
  protected static final Key USERSKEY = KeyFactory.createKey("users", "users");
  protected static final String STORE = "store";

  private static final PersistenceManagerFactory pmfInstance = JDOHelper
      .getPersistenceManagerFactory("transactions-optional");

  private boolean haveUser(byte[] existingUser, PersistenceManager pm) {
    assert pm != null;
    assert existingUser != null;
    try {
      User existing = pm.getObjectById(User.class, User.keyForUser(existingUser));
      if (existing != null) {
        return true;
      } else {
        return false;
      }
    } catch (JDOObjectNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean haveUser(byte[] existingUser) {
    if (existingUser == null) {
      throw new IllegalArgumentException("Null existingUser");
    }
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      return haveUser(existingUser, pm);
    } finally {
      pm.close();
    }
  }

  @Override
  public boolean addUser(byte[] publicKey, byte[] newUser) throws IllegalArgumentException {
    if (newUser == null) {
      throw new IllegalArgumentException("Null newUser");
    }
    User user = new User(newUser, publicKey, new Date());
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      if (haveUser(newUser, pm)) {
        return false;
      }
      pm.makePersistent(user);
      return true;
    } finally {
      pm.close();
    }
  }

  @Override
  public boolean deleteUser(byte[] existingUser) {
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      User existing = pm.getObjectById(User.class, User.keyForUser(existingUser));
      if (existing != null) {
        pm.deletePersistent(existing);
        return true;
      } else {
        return true;
      }
    } catch (JDOObjectNotFoundException e) {
      return false;
    } finally {
      pm.close();
    }
  }

  @Override
  public byte[] getRecord(User user, byte[] key) {
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      Key lookupKey = Lookup.makeKey(user, key);
      Lookup lookup = pm.getObjectById(Lookup.class, lookupKey);
      Revision revision = lookup.getCurrentRevision();
      AppEngineRecord record =
          pm.getObjectById(AppEngineRecord.class, KeyFactory.createKey(lookup.getKey(),
              AppEngineRecord.class.getSimpleName(), revision.toString()));
      return record.getValue();
    } finally {
      pm.close();
    }
  }

  @Override
  public boolean putRecord(User user, byte[] key, byte[] data) {
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      
    } finally {
      
    }
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean updateRecord(User user, byte[] key, byte[] data, Revision expected,
      Revision dataRevision) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean deleteRecord(User user, byte[] key) {
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