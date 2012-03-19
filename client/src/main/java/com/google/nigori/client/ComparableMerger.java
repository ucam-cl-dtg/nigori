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
    assert heads.size() > 0;// caller must ensure that this is true
    List<T> passwords = new ArrayList<T>();
    Map<T, RevValue> mapBack = new HashMap<T, RevValue>();
    for (RevValue value : heads) {
      T password;
      try {
        password = converter.fromBytes(index, value.getValue());
      } catch (ConversionException e) {
        throw new IOException(e);
      }
      passwords.add(password);
      mapBack.put(password, value);
    }
    findEquivalences(store, index, passwords, mapBack);
    T latest = passwords.get(passwords.size() - 1);
    T newPassword = converter.fromLatest(latest);
    return store.put(index, converter.toBytes(newPassword), map(passwords, mapBack).toArray(
        new RevValue[0]));
  }

  private Collection<RevValue> map(List<T> passwords, Map<T, RevValue> mapBack) {
    Collection<RevValue> answer = new ArrayList<RevValue>();
    for (T password : passwords) {
      answer.add(mapBack.get(password));
    }
    return answer;
  }

  private void findEquivalences(MigoriDatastore store, Index index, List<T> passwords,
      Map<T, RevValue> mapBack) throws IOException, NigoriCryptographyException,
      UnauthorisedException {
    Collections.sort(passwords);
    // Daniel's magic find things which are the same and merge them together by means of delicate
    // loops algorithm
    // NEEDS thorough testing, many edge cases.
    Collection<T> equivalence = new ArrayList<T>();
    Collection<T> toRemove = new ArrayList<T>();
    T last = null;
    boolean changed = false;
    for (T password : passwords) {
      if (null == last) {
        last = password;
        continue;
      }
      if (password.equals(last)) {
        equivalence.add(last);
      } else {
        if (!equivalence.isEmpty()) {
          equivalence.add(last);
          TypeValue<T> newValue = putEquivalence(store, index, equivalence, mapBack);
          mapBack.put(newValue.password, newValue.value);
          passwords.add(newValue.password);
          toRemove.addAll(equivalence);
          equivalence.clear();
          changed = true;
        }
      }
      last = password;
    }
    if (!equivalence.isEmpty()) {
      equivalence.add(last);
      TypeValue<T> newValue = putEquivalence(store, index, equivalence, mapBack);
      mapBack.put(newValue.password, newValue.value);
      passwords.add(newValue.password);
      toRemove.addAll(equivalence);
      equivalence.clear();
      changed = true;
    }
    passwords.removeAll(toRemove);
    Collections.sort(passwords);
    if (changed) {
      findEquivalences(store, index, passwords, mapBack);
    }
  }

  private TypeValue<T> putEquivalence(MigoriDatastore store, Index index,
      Collection<T> equivalence, Map<T, RevValue> mapBack) throws IOException,
      NigoriCryptographyException, UnauthorisedException {
    List<RevValue> values = new ArrayList<RevValue>();
    T value = null;
    for (T password : equivalence) {
      value = password;
      values.add(mapBack.get(password));
    }
    T password = converter.fromLatest(value);
    return new TypeValue<T>(password, store.put(index, converter.toBytes(password), values
        .toArray(new RevValue[0])));
  }

  private static class TypeValue<T> {
    public final T password;
    public final RevValue value;

    public TypeValue(T password, RevValue value) {
      this.password = password;
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
