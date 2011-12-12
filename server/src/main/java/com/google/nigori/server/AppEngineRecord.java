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

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;

/**
 * Record represents a single key-value pair in the Datastore.
 * 
 * Instances of the record class can be stored using Java Data Objects in a datastore such as
 * Google's App Engine framework.
 * 
 * 
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
	@Persistent
	private Blob value;

	@Persistent(serialized = "true",types={com.google.nigori.server.IntRevision.class, com.google.nigori.server.BytesRevision.class})
  private Revision revision;
	
	AppEngineRecord(Key lookup, Revision revision, byte[] value) {
	  this.key = lookup;
	  this.revision = revision;
		this.value = new Blob(value);
	}
	
	Key getKey() {
		return key;
	}
	
	byte[] getValue() {
		return value.getBytes();
	}
	
	Revision getRevision() {
	  return revision;
	}
}