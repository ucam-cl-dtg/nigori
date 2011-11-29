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
import com.google.nigori.common.Nonce;

public final class AppEngineDatabase implements Database {

  protected static final String STORE = "store";

  private static final PersistenceManagerFactory pmfInstance = JDOHelper
      .getPersistenceManagerFactory("transactions-optional");

  private AEUser getUser(byte[] publicKey, PersistenceManager pm) throws JDOObjectNotFoundException {
    return pm.getObjectById(AEUser.class, AEUser.keyForUser(publicKey));
  }

  private boolean haveUser(byte[] existingUser, PersistenceManager pm) {
    assert pm != null;
    assert existingUser != null;
    try {
      AEUser existing = getUser(existingUser,pm);
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
  public boolean haveUser(byte[] existingUserPK) throws IllegalArgumentException {
    if (existingUserPK == null) {
      throw new IllegalArgumentException("Null existingUser");
    }
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      return haveUser(existingUserPK, pm);
    } finally {
      pm.close();
    }
  }

  @Override
  public boolean addUser(byte[] publicKey) throws IllegalArgumentException {
    AEUser user = new AEUser(publicKey, new Date());
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      if (haveUser(publicKey, pm)) {
        return false;
      }
      pm.makePersistent(user);
      return true;
    } finally {
      pm.close();
    }
  }

  @Override
  public boolean deleteUser(User existingUser) {
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      AEUser existing = pm.getObjectById(AEUser.class, AEUser.keyForUser(existingUser.getPublicKey()));
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
  public AEUser getUser(byte[] publicKey) throws UserNotFoundException {
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      AEUser user = getUser(publicKey, pm);
      return user;
    } catch (JDOObjectNotFoundException e) {
      throw new UserNotFoundException();
    } finally {
      pm.close();
    }
  }

  private Key getLookupKey(User user, byte[] key) {
    if (! (user instanceof AEUser)){
      user = new AEUser(user.getPublicKey(), user.getRegistrationDate());
    }
    return Lookup.makeKey((AEUser)user, key);
  }
  @Override
  public byte[] getRecord(User user, byte[] key) {
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      Key lookupKey = getLookupKey(user,key);
      Lookup lookup = pm.getObjectById(Lookup.class, lookupKey);
      Revision revision = lookup.getCurrentRevision();
      AppEngineRecord record =
          pm.getObjectById(AppEngineRecord.class, KeyFactory.createKey(lookup.getKey(),
              AppEngineRecord.class.getSimpleName(), revision.toString()));
      return record.getValue();
    } catch (JDOObjectNotFoundException e) {
      return null;
    } finally {
      pm.close();
    }
  }

  @Override
  public boolean putRecord(User user, byte[] key, byte[] data) {
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    //TODO(drt24): Do revisions properly
    Revision revision = new IntRevision(0);
    try {
      Key lookupKey = getLookupKey(user,key);
      Lookup lookup;
      try {
        lookup = pm.getObjectById(Lookup.class, lookupKey);
      } catch (JDOObjectNotFoundException e) {
        lookup = new Lookup(lookupKey, revision);
        pm.makePersistent(lookup);
      }
      AppEngineRecord record =
          new AppEngineRecord(KeyFactory.createKey(lookup.getKey(), AppEngineRecord.class
              .getSimpleName(), revision.toString()), revision, data);
      pm.makePersistent(record);
      return true;
    } finally {
      pm.close();
    }
  }

  @Override
  public boolean updateRecord(User user, byte[] key, byte[] data, Revision expected,
      Revision dataRevision) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean deleteRecord(User user, byte[] key) {
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      Key lookupKey = getLookupKey(user,key);
      Lookup lookup = pm.getObjectById(Lookup.class, lookupKey);
      Revision revision = lookup.getCurrentRevision();
      // TODO(drt24) multiple revisions
      try {
        AppEngineRecord record =
            pm.getObjectById(AppEngineRecord.class, KeyFactory.createKey(lookup.getKey(),
                AppEngineRecord.class.getSimpleName(), revision.toString()));
        pm.deletePersistent(record);
      } finally {// even if there is no value the key still needs to be deleted - but we haven't
                 // actually done a delete
        pm.deletePersistent(lookup);
      }
      return true;
    } catch (JDOObjectNotFoundException e) {
      return false;
    } finally {
      pm.close();
    }
  }

  @Override
  public UserFactory getUserFactory() {
    return AEUser.Factory.getInstance();
  }

  @Override
  public boolean checkAndAddNonce(Nonce nonce, byte[] publicKey) {
    if (!nonce.isRecent()) {
      return false;
    }
    PersistenceManager pm = pmfInstance.getPersistenceManager();
    try {
      AENonce aeNonce = new AENonce(nonce,publicKey);
      try {
        pm.getObjectById(AENonce.class,aeNonce.getKey());
        return false;// getObjectById should throw an exception
      } catch (JDOObjectNotFoundException e) {
        // We haven't seen this nonce yet so add it and return true
        pm.makePersistent(aeNonce);
        return true;
      }
    } finally {
      pm.close();
    }
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