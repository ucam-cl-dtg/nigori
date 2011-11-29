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

import com.google.nigori.common.Nonce;

/**
 * API required by Nigori to save user data.
 * 
 * @author Alastair Beresford
 *
 */
interface Database {
	
  UserFactory getUserFactory();
  
	public boolean addUser(byte[] publicKey);
	public boolean haveUser(byte[] existingUser);
	public boolean deleteUser(User existingUser);
	
	/**
	 * Check that the Nonce has not been used while the timestamp it is for has been valid.
	 * If it has not been used before it now has been and so future calls with that Nonce will return false.
	 * @param nonce
	 * @return
	 */
	// TODO(beresford): Put this constant in server configuration and use this to timeout
	// storage of random nonce values out of the database (when we start to store them!).
	// TODO(beresford): Must avoid race condition when random number is used multiple times
	// quickly
	public boolean checkAndAddNonce(Nonce nonce, byte[] publicKey);

	/**
	 * WARNING: great care must be taken when using this, the user must be authenticated correctly before any user object can be used on their behalf
	 * @param publicKey
	 * @return
	 * @throws UserNotFoundException
	 */
	public User getUser(byte[] publicKey) throws UserNotFoundException;
	
	/**
	 * 
	 * @param user
	 * @param key
	 * @return the record for the key or null if there is no record for that key
	 */
	public byte[] getRecord(User user, byte[] key);
	public boolean putRecord(User user, byte[] key, byte[] data);
	public boolean updateRecord(User user, byte[] key, byte[] data, Revision expected, Revision dataRevision);
	public boolean deleteRecord(User user, byte[] key);
}
