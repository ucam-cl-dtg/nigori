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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.nigori.common.Nonce;
import com.google.nigori.common.RevValue;

/**
 * An in-memory database used for system testing.
 * 
 * Do not use in production as data is not saved to non-volatile storage nor consistent across
 * potentially many instances of App Engine.
 *
 * @author Alastair Beresford
 *
 */
public class HashMapDatabase extends AbstractDatabase implements Serializable {

  private static final long serialVersionUID = 1L;
  // TODO(drt24) provide a WeakHashMap version so this can be used as a cache
	private HashMap<User,Map<Bytes, Map<Bytes,Bytes>>> stores = new HashMap<User,Map<Bytes, Map<Bytes,Bytes>>>();
	private HashMap<Bytes,User> users = new HashMap<Bytes,User>();
	private HashMap<Bytes,Set<Nonce>> nonces = new HashMap<Bytes,Set<Nonce>>();

	@Override
	public boolean addUser(byte[] publicKey) {
		//TODO(beresford): check authority to carry out action
	  if (haveUser(publicKey)){
	    return false;
	  }
	  User user = new JUser(publicKey, new Date());
		users.put(Bytes.copyFrom(publicKey),user);
		stores.put(user, new HashMap<Bytes, Map<Bytes,Bytes>>());
		return true;
	}
	
	@Override
	public boolean haveUser(byte[] publicKey) {
		return users.containsKey(Bytes.copyFrom(publicKey));
	}

	@Override
	public boolean deleteUser(User existingUser) {
		//TODO(beresford): check authority to carry out action
	  User user = users.remove(Bytes.copyFrom(existingUser.getPublicKey()));
	  return user != null && stores.remove(user) != null;
	}

  @Override
  public User getUser(byte[] publicKey) throws UserNotFoundException {
    assert publicKey != null;
    assert publicKey.length > 0;
    User user = users.get(Bytes.copyFrom(publicKey));
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

    Map<Bytes, Bytes> revisions = stores.get(user).get(Bytes.copyFrom(key));
    if (revisions != null) {
      List<RevValue> answer = new ArrayList<RevValue>(revisions.size());
      for (Map.Entry<Bytes, Bytes> rv : revisions.entrySet()) {
        answer.add(new RevValue(rv.getKey().toByteArray(), rv.getValue().toByteArray()));
      }

      return answer;
    } else {
      return null;
    }
	}

  @Override
  public RevValue getRevision(User user, byte[] key, byte[] revision) throws IOException {
    if (key == null) {
      return null;
    }

    Map<Bytes, Bytes> revisions = stores.get(user).get(Bytes.copyFrom(key));
    if (revisions != null) {
      Bytes value = revisions.get(Bytes.copyFrom(revision));
      if (value != null) {
        return new RevValue(revision, value.toByteArray());
      }
    }
    return null;
  }

  @Override
  public Collection<byte[]> getIndices(User user) {
    Set<Bytes> indices = stores.get(user).keySet();
    List<byte[]> answer = new ArrayList<byte[]>(indices.size());
    for (Bytes index : indices){
      answer.add(index.toByteArray());
    }
    return answer;
  }

  @Override
  public Collection<byte[]> getRevisions(User user, byte[] key) {
 // TODO(beresford): check authority to carry out action
    if (key == null) {
      return null;
    }

    Map<Bytes, Bytes> revisions = stores.get(user).get(Bytes.copyFrom(key));
    if (revisions != null) {
      List<byte[]> answer = new ArrayList<byte[]>(revisions.size());
      for (Bytes rev : revisions.keySet()) {
        answer.add(rev.toByteArray());
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
    Map<Bytes, Bytes> revisions = stores.get(user).get(Bytes.copyFrom(key));
    if (revisions == null) {
      revisions = new HashMap<Bytes, Bytes>();
      stores.get(user).put(Bytes.copyFrom(key), revisions);
    }
    Bytes bRevision = Bytes.copyFrom(revision);
    Bytes bValue = Bytes.copyFrom(value);
    Bytes existing = revisions.get(bRevision);
    if (existing == null) {
      revisions.put(bRevision, bValue);
    } else if (!existing.equals(bValue)) {
      return false;
    }
    return true;
  }
	
	@Override
	public boolean deleteRecord(User user, byte[] key) {
		//TODO(beresford): check authority to carry out action
	  return stores.get(user).remove(Bytes.copyFrom(key)) != null;
	}

  @Override
  public UserFactory getUserFactory() {
    return JUser.Factory.getInstance();
  }

  @Override
  public boolean checkAndAddNonce(Nonce nonce, byte[] publicKey) {
    if (!nonce.isRecent()){
      return false;
    }
    Bytes pk = Bytes.copyFrom(publicKey);
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

  @Override
  public void clearOldNonces() {
    for (Set<Nonce> nonceSet : nonces.values()){
      Iterator<Nonce> nonceIterator = nonceSet.iterator();
      Nonce nonce;
      while (nonceIterator.hasNext()){
        nonce = nonceIterator.next();
        if (!nonce.isRecent()){
          nonceIterator.remove();
        }
      }
    }
  }

  private static class Bytes implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] bytes;

    private Bytes(byte[] bytes) {
      this.bytes = bytes;
    }

    /**
     * @param publicKey
     * @return
     */
    public static Bytes copyFrom(byte[] publicKey) {
      return new Bytes(Arrays.copyOf(publicKey, publicKey.length));
    }

    /**
     * @return
     */
    public byte[] toByteArray() {
      return bytes;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(bytes);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Bytes other = (Bytes) obj;
      if (!Arrays.equals(bytes, other.bytes))
        return false;
      return true;
    }

  }
}
