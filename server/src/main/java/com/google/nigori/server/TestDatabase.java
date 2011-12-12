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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.nigori.common.Nonce;
import com.google.nigori.common.RevValue;
import com.google.protobuf.ByteString;

/**
 * An in-memory database used for system testing.
 * 
 * Do not use in production as data is not saved to non-volatile storage nor consistent across
 * potentially many instances of App Engine.
 *
 * @author Alastair Beresford
 *
 */
public class TestDatabase implements Database {

	private HashMap<User,Map<ByteString, Map<ByteString,ByteString>>> stores = new HashMap<User,Map<ByteString, Map<ByteString,ByteString>>>();
	private HashMap<ByteString,User> users = new HashMap<ByteString,User>();
	private HashMap<ByteString,Set<Nonce>> nonces = new HashMap<ByteString,Set<Nonce>>();

	@Override
	public boolean addUser(byte[] publicKey) {
		//TODO(beresford): check authority to carry out action
	  if (haveUser(publicKey)){
	    return false;
	  }
	  User user = new JUser(publicKey, new Date());
		users.put(ByteString.copyFrom(publicKey),user);
		stores.put(user, new HashMap<ByteString, Map<ByteString,ByteString>>());
		return true;
	}
	
	@Override
	public boolean haveUser(byte[] publicKey) {
		return users.containsKey(ByteString.copyFrom(publicKey));
	}

	@Override
	public boolean deleteUser(User existingUser) {
		//TODO(beresford): check authority to carry out action
	  User user = users.remove(ByteString.copyFrom(existingUser.getPublicKey()));
	  return user != null && stores.remove(user) != null;
	}

  @Override
  public User getUser(byte[] publicKey) throws UserNotFoundException {
    assert publicKey != null;
    assert publicKey.length > 0;
    User user = users.get(ByteString.copyFrom(publicKey));
    if (user == null) {
      throw new UserNotFoundException();
    }
    return user;
  }

  @Override
	public Collection<RevValue> getRecord(User user, byte[] key) {

    // TODO(beresford): check authority to carry out action
    if (key == null) {
      return null;
    }

    Map<ByteString, ByteString> revisions = stores.get(user).get(ByteString.copyFrom(key));
    if (revisions != null) {
      List<RevValue> answer = new ArrayList<RevValue>(revisions.size());
      for (Map.Entry<ByteString, ByteString> rv : revisions.entrySet()) {
        answer.add(new RevValue(rv.getKey().toByteArray(), rv.getValue().toByteArray()));
      }

      return answer;
    } else {
      return null;
    }
	}

	@Override
  public boolean putRecord(User user, byte[] key, byte[] revision, byte[] value) {
    // TODO(beresford): check authority to carry out action
    if (key == null || revision == null || value == null) {
      return false;
    }
    Map<ByteString, ByteString> revisions = stores.get(user).get(ByteString.copyFrom(key));
    if (revisions == null) {
      revisions = new HashMap<ByteString, ByteString>();
      stores.get(user).put(ByteString.copyFrom(key), revisions);
    }
    ByteString bRevision = ByteString.copyFrom(revision);
    ByteString bValue = ByteString.copyFrom(value);
    ByteString existing = revisions.get(bRevision);
    if (existing == null) {
      revisions.put(bRevision, bValue);
    } else if (!existing.equals(bValue)) {
      return false;
    }
    return true;
  }

	@Override
	public boolean updateRecord(User user, byte[] key, byte[] value, Revision expected, Revision dataRevision) {
		//TODO(beresford): check authority to carry out action
		//TODO(beresford): provide appropriate implementation
		return false;
	}
	
	@Override
	public boolean deleteRecord(User user, byte[] key) {
		//TODO(beresford): check authority to carry out action
	  return stores.get(user).remove(ByteString.copyFrom(key)) != null;
	}

  @Override
  public UserFactory getUserFactory() {
    return JUser.Factory.getInstance();
  }

  @Override
  public boolean checkAndAddNonce(Nonce nonce, byte[] publicKey) {
    //TODO(drt24) old nonces are never pruned
    if (!nonce.isRecent()){
      return false;
    }
    ByteString pk = ByteString.copyFrom(publicKey);
    Set<Nonce> nonceSet = nonces.get(pk);
    if (nonceSet == null){
      nonceSet = new HashSet<Nonce>();
      nonces.put(pk,nonceSet);
    }
    if (nonceSet.contains(nonce))
      return false;
    else {
      nonceSet.add(nonce);
      return true;
    }
  }
}
