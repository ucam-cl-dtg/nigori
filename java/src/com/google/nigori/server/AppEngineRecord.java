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

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

/**
 * Record represents a single key-value pair in the Datastore.
 * 
 * Instances of the record class can be stored using Java Data Objects in a datastore such as
 * Google's App Engine framework. Note that the Record class contains references to other instances
 * of the Record class (next and earlier) which permits storage of data items larger than one 
 * megabyte and keep an historical archive. Doing it in this way means that cascading deletes
 * works (TODO(beresford): check this is true)
 * 
 * @author Alastair Beresford
 *
 */
@PersistenceCapable
class AppEngineRecord {
	
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Key key;
  
	/**
	 * Data associated with the key for this record.
	 */
  //TODO(beresford): consider whether to make this a BLOB
	@Persistent
	private byte[] value;
	
	/**
	 * Singly-linked list of Records whose value fields, in order, represent the data value
	 * associated with a given key. If next is null, then there are no further items in the list.
	 */
	@Persistent
	private Key next;
	
	/**
	 * Singly-linked list of copies of data which were previously associated with this key. If null,
	 * then no earlier records exist.
	 */
	@Persistent
	private Key earlier;
	
	AppEngineRecord(byte[] value, Key next, Key earlier) {
		//TODO(beresford): consider whether to do a deep copy here or not
		this.value = value;
		this.next = next;
		this.earlier = earlier;
	}
	
	Key getKey() {
		return key;
	}
	
	byte[] getValue() {
		return value;
	}
	
	Key getNext() {
		return next;
	}
	
	Key getEarlier() {
		return earlier;
	}
}