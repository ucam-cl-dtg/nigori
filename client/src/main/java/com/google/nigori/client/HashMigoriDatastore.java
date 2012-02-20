/*
 * Copyright (C) 2012 Daniel Thomas (drt24)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.nigori.client;

import static com.google.nigori.client.HashDAG.HASH_SIZE;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.codec.binary.Hex;

import com.google.nigori.common.Index;
import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;
import com.google.nigori.common.UnauthorisedException;

/**
 * @author drt24
 * 
 */
public class HashMigoriDatastore implements MigoriDatastore {

  private final NigoriDatastore store;

  public HashMigoriDatastore(NigoriDatastore store) {
    this.store = store;
  }

  @Override
  public boolean register() throws IOException, NigoriCryptographyException {
    return store.register();
  }

  @Override
  public boolean unregister() throws IOException, NigoriCryptographyException, UnauthorisedException {
    return store.unregister();
  }

  @Override
  public boolean authenticate() throws IOException, NigoriCryptographyException {
    return store.authenticate();
  }

  @Override
  public List<Index> getIndices() throws NigoriCryptographyException, IOException, UnauthorisedException {
    return store.getIndices();
  }

  @Override
  public RevValue removeValue(Index index, RevValue... parents) {
    // TODO(drt24) implement removeValue
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public RevValue getMerging(Index index, MigoriMerger merger) throws NigoriCryptographyException,
      IOException, UnauthorisedException {
    List<RevValue> heads = get(index);
    if (heads == null || heads.size() == 0) {
      return null;
    }
    for (RevValue head : heads) {
      validateHash(head.getValue(), head.getRevision());
    }
    if (heads.size() == 1) {
      for (RevValue head : heads) {
        return head;
      }
      throw new IllegalStateException("Can never happen as must be one head to return");
    } else {
      return merger.merge(this, index, heads);
    }
  }

  @Override
  public List<RevValue> get(Index index) throws NigoriCryptographyException, IOException, UnauthorisedException {
    DAG<Revision> history = getHistory(index);
    if (history == null){
      return null;
    }
    Collection<Node<Revision>> heads = history.getHeads();
    List<RevValue> answer = new ArrayList<RevValue>();
    for (Node<Revision> rev : heads) {
      Revision revision = rev.getValue();
      byte[] value = getRevision(index, revision);
      // TODO(drt24) value might be null
      if (value != null) {
        answer.add(new RevValue(revision, value));
      }
    }
    return answer;
  }

  @Override
  public RevValue put(Index index, byte[] value, RevValue... parents) throws IOException,
      NigoriCryptographyException, UnauthorisedException {
    byte[] revBytes = generateHash(value, toIDByte(parents));
    Revision rev = new Revision(revBytes);
    RevValue rv = new RevValue(rev, value);
    boolean success = store.put(index, rev, value);
    if (!success) {
      throw new IOException("Could not put into the store");
    }
    return rv;

  }

  private byte[] toIDByte(RevValue... parents) {
    Arrays.sort(parents);
    byte[] idBytes = new byte[parents.length * HASH_SIZE];
    int insertPoint = 0;
    for (RevValue rev : parents) {
      System.arraycopy(rev.getRevision().getBytes(), 0, idBytes, insertPoint, HASH_SIZE);
      insertPoint += HASH_SIZE;
    }
    return idBytes;
  }

  private byte[] generateHash(byte[] value, byte[] idBytes) throws NigoriCryptographyException {
    byte[] toHash = new byte[value.length + idBytes.length];
    System.arraycopy(value, 0, toHash, 0, value.length);
    System.arraycopy(idBytes, 0, toHash, value.length, idBytes.length);

    MessageDigest crypt;
    try {
      crypt = MessageDigest.getInstance("SHA-1");

      crypt.reset();
      crypt.update(toHash);
      byte[] hashBytes = crypt.digest();
      byte[] revBytes = new byte[HASH_SIZE + idBytes.length];
      System.arraycopy(hashBytes, 0, revBytes, 0, HASH_SIZE);
      System.arraycopy(idBytes, 0, revBytes, HASH_SIZE, idBytes.length);
      return revBytes;
    } catch (NoSuchAlgorithmException e) {
      throw new NigoriCryptographyException(e);
    }
  }

  @Override
  public boolean removeIndex(Index index, Revision position) throws NigoriCryptographyException,
      IOException, UnauthorisedException {
    return store.delete(index, position.getBytes());
  }

  @Override
  public DAG<Revision> getHistory(Index index) throws NigoriCryptographyException, IOException, UnauthorisedException {
    List<Revision> revisions = store.getRevisions(index);
    if (revisions == null) {
      return null;
    }
    return new HashDAG(revisions);
  }

  /**
   * Verify that the value provided gives the correct hash when combined with the Revision
   * 
   * @param value
   * @param revision
   * @return
   * @throws NigoriCryptographyException
   * @throws InvalidHashException
   */
  private byte[] validateHash(byte[] value, Revision revision) throws NigoriCryptographyException,
      InvalidHashException {
    byte[] revBytes = revision.getBytes();
    byte[] hash = Arrays.copyOfRange(generateHash(value, Arrays.copyOfRange(revBytes, HASH_SIZE, revBytes.length)),0,HASH_SIZE);
    byte[] revHash = Arrays.copyOfRange(revBytes, 0, HASH_SIZE);
    if (Arrays.equals(hash, revHash)) {
      return value;
    } else {
      throw new InvalidHashException(revHash, hash);
    }
  }

  @Override
  public byte[] getRevision(Index index, Revision revision) throws IOException,
      NigoriCryptographyException, UnauthorisedException {
    return validateHash(store.getRevision(index, revision), revision);
  }

  public static class InvalidHashException extends IOException {

    private static final long serialVersionUID = 1L;

    public InvalidHashException(byte[] expectedHash, byte[] gotHash) {
      super("Expected: " + new String(Hex.encodeHex(expectedHash)) + " and got: "
          + new String(Hex.encodeHex(gotHash)));
    }

  }
}
