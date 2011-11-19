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
import java.util.HashMap;
import java.util.Map;

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

	private HashMap<User,Map<ByteString, ByteString>> stores = new HashMap<User,Map<ByteString, ByteString>>();
	private HashMap<ByteString,User> users = new HashMap<ByteString,User>();

	@Override
	public boolean addUser(byte[] publicKey) {
		//TODO(beresford): check authority to carry out action
	  if (haveUser(publicKey)){
	    return false;
	  }
	  User user = new User(publicKey, new Date());
		users.put(ByteString.copyFrom(publicKey),user);
		stores.put(user, new HashMap<ByteString, ByteString>());
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
	public byte[] getRecord(User user, byte[] key) {

		//TODO(beresford): check authority to carry out action
		if (key == null) {
			return null;
		}

		ByteString value = stores.get(user).get(ByteString.copyFrom(key));

		if (value == null) {
			return null;
		} else {		
			byte[] v = value.toByteArray();
			return v;
		}
	}

	@Override
	public boolean putRecord(User user, byte[] key, byte[] value) {
		//TODO(beresford): check authority to carry out action
		if (key == null || value == null) {
			return false;
		}
		
		stores.get(user).put(ByteString.copyFrom(key), ByteString.copyFrom(value));
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
}
