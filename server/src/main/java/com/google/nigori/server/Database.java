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

/**
 * API required by Nigori to save user data.
 * 
 * @author Alastair Beresford
 *
 */
interface Database {
	
	public boolean addUser(byte[] publicKey, byte[] newUserName);
	public boolean haveUser(byte[] existingUser);
	public boolean deleteUser(byte[] existingUser);
	
	public byte[] getRecord(User user, byte[] key);
	public boolean putRecord(User user, byte[] key, byte[] data);
	public boolean updateRecord(User user, byte[] key, byte[] data, Revision expected, Revision dataRevision);
	public boolean deleteRecord(User user, byte[] key);
}
