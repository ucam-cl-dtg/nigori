/*
 * Copyright (C) 2012 Daniel Thomas (drt24)
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
package com.google.nigori.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.nigori.common.RevValue;

/**
 * @author drt24
 *
 * TODO(drt24) many of the operations here are parallelisable.
 */
public class SyncingNigoriDatastore implements NigoriDatastore {

  private final NigoriDatastore first;
  private final NigoriDatastore second;

  public SyncingNigoriDatastore(NigoriDatastore first, NigoriDatastore second ) throws IOException, NigoriCryptographyException{
    if (first == null || second == null){
      throw new IllegalArgumentException("Datastores must not be null");
    }
    this.first = first;
    this.second = second;
    //syncAll();
  }
  
  /**
   * Synchronise all indices and revisions between both stores
   * 
   * @throws NigoriCryptographyException
   * @throws IOException
   */
  public void syncAll() throws IOException, NigoriCryptographyException {
    List<byte[]> firstIndices = first.getIndices();
    List<byte[]> secondIndices = second.getIndices();
    if (!(firstIndices.containsAll(secondIndices) && secondIndices.containsAll(firstIndices))) {
      // indices not already synced
      List<byte[]> firstMSecond = new ArrayList<byte[]>(firstIndices);
      firstMSecond.removeAll(secondIndices);
      addAllIndices(firstMSecond, first, second);
      List<byte[]> secondMFirst = new ArrayList<byte[]>(secondIndices);
      secondMFirst.removeAll(firstIndices);
      addAllIndices(secondMFirst, second, first);
      // make sure firstIndices has all the indices
      firstIndices.addAll(secondIndices);
    }
    // Now need to sync revisions.
    List<byte[]> indices = firstIndices;
    for (byte[] index : indices) {
      syncRevisions(index);
    }
  }
  /**
   * @param index
   * @throws IOException 
   * @throws NigoriCryptographyException 
   */
  private void syncRevisions(byte[] index) throws NigoriCryptographyException, IOException {
    List<byte[]> firstRevisions = first.getRevisions(index);
    List<byte[]> secondRevisions = second.getRevisions(index);
    if (!(firstRevisions.containsAll(secondRevisions) && secondRevisions.containsAll(firstRevisions))){
      // revisions not already synced
      List<byte[]> firstMSecond = new ArrayList<byte[]>(firstRevisions);
      firstMSecond.removeAll(secondRevisions);
      addAllRevisions(index, firstMSecond, first, second);
      List<byte[]> secondMFirst = new ArrayList<byte[]>(secondRevisions);
      secondMFirst.removeAll(firstRevisions);
      addAllRevisions(index, secondMFirst, second, first);
    }    
  }
  /**
   * @param revisions
   * @param from
   * @param to
   * @throws NigoriCryptographyException 
   * @throws IOException 
   */
  private void addAllRevisions(byte[] index, List<byte[]> revisions, NigoriDatastore from,
      NigoriDatastore to) throws IOException, NigoriCryptographyException {
    for (byte[] revision : revisions){
      to.put(index, revision, from.getRevision(index, revision));
    }    
  }

  /**
   * Add all the indices from one NigoriDatastore to another
   * @param indices
   * @param from
   * @param to
   * @throws NigoriCryptographyException 
   * @throws IOException 
   */
  private void addAllIndices(List<byte[]> indices, NigoriDatastore from, NigoriDatastore to) throws IOException, NigoriCryptographyException {
    for (byte[] index : indices){
      List<RevValue> revs = from.get(index);
      for (RevValue rev : revs){
        to.put(index, rev.getRevision(), rev.getValue());
      }
    }    
  }
  /**
   * Attempts to register to both stores, if one fails the other may still succeed and false will be returned
   */
  @Override
  public boolean register() throws IOException, NigoriCryptographyException {
    boolean firstReg = first.register();
    boolean secondReg = second.register();
    return firstReg && secondReg;
  }

  /**
   * Attempts to unregister from both stores, if one fails the other may still succeed and false will be returned
   */
  @Override
  public boolean unregister() throws IOException, NigoriCryptographyException {
    boolean firstReg = first.unregister();
    boolean secondReg = second.unregister();
    return firstReg && secondReg;
  }

  /**
   * Attempts to authenticate to both stores, if one fails the other may still succeed and false will be returned
   */
  @Override
  public boolean authenticate() throws IOException, NigoriCryptographyException {
    boolean firstAuth = first.authenticate();
    boolean secondAuth = second.authenticate();
    return firstAuth && secondAuth;
  }

  @Override
  public boolean put(byte[] index, byte[] revision, byte[] value) throws IOException,
      NigoriCryptographyException {
    // TODO(drt24) Auto-generated method stub
    return false;
  }

  @Override
  public List<byte[]> getIndices() {
    // TODO(drt24) Auto-generated method stub
    return null;
  }
  @Override
  public List<RevValue> get(byte[] index) throws IOException, NigoriCryptographyException {
    // TODO(drt24) Auto-generated method stub
    return null;
  }

  @Override
  public byte[] getRevision(byte[] index, byte[] revision) throws IOException,
      NigoriCryptographyException {
    // TODO(drt24) Auto-generated method stub
    return null;
  }

  @Override
  public List<byte[]> getRevisions(byte[] index) throws NigoriCryptographyException, IOException {
    // TODO(drt24) Auto-generated method stub
    return null;
  }

  @Override
  public boolean delete(byte[] index) throws NigoriCryptographyException, IOException {
    // TODO(drt24) Auto-generated method stub
    return false;
  }

}
