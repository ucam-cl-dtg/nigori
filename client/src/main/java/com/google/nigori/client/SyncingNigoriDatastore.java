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
import java.util.Arrays;
import java.util.List;

import com.google.nigori.common.Index;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;

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
    List<Index> firstIndices = first.getIndices();
    List<Index> secondIndices = second.getIndices();
    if (!(firstIndices.containsAll(secondIndices) && secondIndices.containsAll(firstIndices))) {
      // indices not already synced
      List<Index> firstMSecond = new ArrayList<Index>(firstIndices);
      firstMSecond.removeAll(secondIndices);
      addAllIndices(firstMSecond, first, second);
      List<Index> secondMFirst = new ArrayList<Index>(secondIndices);
      secondMFirst.removeAll(firstIndices);
      addAllIndices(secondMFirst, second, first);
      // make sure firstIndices has all the indices
      firstIndices.addAll(secondIndices);
    }
    // Now need to sync revisions.
    List<Index> indices = firstIndices;
    for (Index index : indices) {
      syncRevisions(index);
    }
  }
  /**
   * @param index
   * @throws IOException 
   * @throws NigoriCryptographyException 
   */
  private void syncRevisions(Index index) throws NigoriCryptographyException, IOException {
    List<Revision> firstRevisions = first.getRevisions(index);
    List<Revision> secondRevisions = second.getRevisions(index);
    if (!(firstRevisions.containsAll(secondRevisions) && secondRevisions.containsAll(firstRevisions))){
      // revisions not already synced
      List<Revision> firstMSecond = new ArrayList<Revision>(firstRevisions);
      firstMSecond.removeAll(secondRevisions);
      addAllRevisions(index, firstMSecond, first, second);
      List<Revision> secondMFirst = new ArrayList<Revision>(secondRevisions);
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
  private void addAllRevisions(Index index, List<Revision> revisions, NigoriDatastore from,
      NigoriDatastore to) throws IOException, NigoriCryptographyException {
    for (Revision revision : revisions){
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
  private void addAllIndices(List<Index> indices, NigoriDatastore from, NigoriDatastore to) throws IOException, NigoriCryptographyException {
    for (Index index : indices){
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
  public boolean put(Index index, Revision revision, byte[] value) throws IOException,
      NigoriCryptographyException {
    boolean firstPut = first.put(index, revision, value);
    boolean secondPut = second.put(index, revision, value);
    return firstPut && secondPut;
  }

  @Override
  public List<Index> getIndices() throws NigoriCryptographyException, IOException {
    List<Index> firstIndices = first.getIndices();
    List<Index> secondIndices = second.getIndices();
    secondIndices.removeAll(firstIndices);
    firstIndices.addAll(secondIndices);//TODO(drt24) do a proper union and sync here
    return firstIndices;
  }

  @Override
  public List<RevValue> get(Index index) throws IOException, NigoriCryptographyException {
    List<RevValue> firstRevVals = first.get(index);
    List<RevValue> secondRevVals = second.get(index);
    if (firstRevVals == null){
      if (secondRevVals == null){
        return null;
      } else {
        return secondRevVals;
      }
    }
    if (secondRevVals == null){
      return firstRevVals;
    }
    secondRevVals.removeAll(firstRevVals);
    firstRevVals.addAll(secondRevVals);//TODO(drt24) do a proper union and sync here
    return firstRevVals;
  }

  @Override
  public byte[] getRevision(Index index, Revision revision) throws IOException,
      NigoriCryptographyException {
    byte[] firstValue = first.getRevision(index, revision);
    byte[] secondValue = second.getRevision(index, revision);
    if (!Arrays.equals(firstValue, secondValue)){
      throw new IOException("Stores returned different values for the same revision");
    }
    return firstValue;
  }

  @Override
  public List<Revision> getRevisions(Index index) throws NigoriCryptographyException, IOException {
    List<Revision> firstRevisions = first.getRevisions(index);
    List<Revision> secondRevisions = second.getRevisions(index);
    if (firstRevisions == null){
      if (secondRevisions == null){
        return null;
      } else {
        return secondRevisions;
      }
    }
    if (secondRevisions == null){
      return firstRevisions;
    }
    secondRevisions.removeAll(firstRevisions);
    firstRevisions.addAll(secondRevisions);//TODO(drt24) do a proper union and sync here
    return firstRevisions;
  }

  @Override
  public boolean delete(Index index, byte[] token) throws NigoriCryptographyException, IOException {
    boolean firstDel = first.delete(index, token);
    boolean secondDel = second.delete(index, token);
    return firstDel & secondDel;
  }

}
