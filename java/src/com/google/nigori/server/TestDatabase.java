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

import java.util.HashMap;
import java.util.HashSet;

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

	private HashMap<ByteString, ByteString> map = new HashMap<ByteString, ByteString>();
	private HashSet<ByteString> users = new HashSet<ByteString>();

	@Override
	public boolean addUser(byte[] authority, byte[] user) {
		//TODO(beresford): check authority to carry out action
		users.add(ByteString.copyFrom(user));
		return true;
	}
	
	@Override
	public boolean haveUser(byte[] user) {
		return users.contains(ByteString.copyFrom(user));
	}

	@Override
	public boolean deleteUser(byte[] authority, byte[] existingUser) {
		//TODO(beresford): check authority to carry out action
		//TODO(beresford): provide appropriate implementation
		return false;
	}
	
	@Override
	public byte[] getRecord(byte[] authority, byte[] key) {

		//TODO(beresford): check authority to carry out action
		if (key == null) {
			return null;
		}

		ByteString value = map.get(ByteString.copyFrom(key));

		if (value == null) {
			return null;
		} else {		
			byte[] v = value.toByteArray();
			return v;
		}
	}

	@Override
	public boolean putRecord(byte[] authority, byte[] key, byte[] value) {
		//TODO(beresford): check authority to carry out action
		if (key == null || value == null) {
			return false;
		}
		
		map.put(ByteString.copyFrom(key), ByteString.copyFrom(value));
		return true;
	}

	@Override
	public boolean updateRecord(byte[] authority, byte[] key, byte[] value) {
		//TODO(beresford): check authority to carry out action
		//TODO(beresford): provide appropriate implementation
		return false;
	}
	
	@Override
	public boolean deleteRecord(byte[] authority, byte[] key) {
		//TODO(beresford): check authority to carry out action
		map.remove(ByteString.copyFrom(key));
		return true;
	}
}
