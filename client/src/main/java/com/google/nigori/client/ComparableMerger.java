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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.nigori.client.MigoriDatastore.MigoriMerger;
import com.google.nigori.common.Index;
import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.UnauthorisedException;

public class ComparableMerger<T extends Comparable<T>> implements MigoriMerger {

  private Converter<T> converter;

  public ComparableMerger(Converter<T> converter) {
    if (converter == null) {
      throw new IllegalArgumentException("converter may not be null");
    }
    this.converter = converter;
  }

  @Override
  public RevValue merge(MigoriDatastore store, Index index, Collection<RevValue> heads)
      throws IOException, NigoriCryptographyException, UnauthorisedException {
    assert heads.size() > 1;// caller must ensure that this is true
    List<T> items = new ArrayList<T>(heads.size());
    Map<T, RevValue> mapBack = new HashMap<T, RevValue>();
    for (RevValue value : heads) {
      T item;
      try {
        item = converter.fromBytes(index, value.getValue());
      } catch (ConversionException e) {
        throw new IOException(e);
      }
      items.add(item);
      mapBack.put(item, value);
    }
    findEquivalences(store, index, items, mapBack);
    T latest = items.get(items.size() - 1);
    if (items.size() == 1) {// we have already reduced it to one so don't need to do anything more
      return mapBack.get(latest);
    }
    T newItem = converter.fromLatest(latest);
    return store.put(index, converter.toBytes(newItem), map(items, mapBack)
        .toArray(new RevValue[0]));
  }

  private Collection<RevValue> map(List<T> items, Map<T, RevValue> mapBack) {
    Collection<RevValue> answer = new ArrayList<RevValue>();
    for (T item : items) {
      answer.add(mapBack.get(item));
    }
    return answer;
  }

  private void findEquivalences(MigoriDatastore store, Index index, List<T> items,
      Map<T, RevValue> mapBack) throws IOException, NigoriCryptographyException,
      UnauthorisedException {
    Collections.sort(items);
    // Daniel's magic find things which are the same and merge them together by means of delicate
    // loops algorithm
    // NEEDS thorough testing, many edge cases.
    Collection<T> equivalence = new ArrayList<T>();
    Collection<T> toRemove = new ArrayList<T>();
    Collection<T> toAdd = new ArrayList<T>();
    T last = null;
    boolean changed = false;
    for (T item : items) {
      if (null == last) {
        last = item;
        continue;
      }
      if (item.equals(last)) {
        equivalence.add(last);
      } else {
        if (!equivalence.isEmpty()) {
          equivalence.add(last);
          TypeValue<T> newValue = putEquivalence(store, index, equivalence, mapBack);
          mapBack.put(newValue.item, newValue.value);
          toAdd.add(newValue.item);
          toRemove.addAll(equivalence);
          equivalence.clear();
          changed = true;
        }
      }
      last = item;
    }
    if (!equivalence.isEmpty()) {
      equivalence.add(last);
      TypeValue<T> newValue = putEquivalence(store, index, equivalence, mapBack);
      mapBack.put(newValue.item, newValue.value);
      toRemove.addAll(equivalence);
      equivalence.clear();
      toAdd.add(newValue.item);
      changed = true;
    }
    items.removeAll(toRemove);
    items.addAll(toAdd);

    Collections.sort(items);
    if (changed) {
      findEquivalences(store, index, items, mapBack);
    }
  }

  private TypeValue<T> putEquivalence(MigoriDatastore store, Index index,
      Collection<T> equivalence, Map<T, RevValue> mapBack) throws IOException,
      NigoriCryptographyException, UnauthorisedException {
    assert equivalence.size() > 1;
    List<RevValue> values = new ArrayList<RevValue>();
    T value = null;
    for (T item : equivalence) {
      value = item;
      values.add(mapBack.get(item));
    }
    T item = converter.fromLatest(value);
    RevValue rv = store.put(index, converter.toBytes(item), values.toArray(new RevValue[0]));
    return new TypeValue<T>(item, rv);
  }

  private static class TypeValue<T> {
    public final T item;
    public final RevValue value;

    public TypeValue(T item, RevValue value) {
      this.item = item;
      this.value = value;
    }
  }

  public interface Converter<T> {
    T fromBytes(Index index, byte[] value) throws ConversionException;

    T fromLatest(T from);

    byte[] toBytes(T from);
  }

  public static class ConversionException extends Exception {
    private static final long serialVersionUID = 1L;

    public ConversionException(Exception e) {
      super(e);
    }

    public ConversionException(String message) {
      super(message);
    }

  }
}
