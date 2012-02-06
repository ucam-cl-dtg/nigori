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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.everyItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.nigori.client.DAG.Node;
import com.google.nigori.common.NigoriConstants;
import com.google.nigori.common.Revision;

/**
 * @author drt24
 * 
 */
@RunWith(Parameterized.class)
public class DAGTest {

  @Parameters
  public static Collection<DAGFactory[]> dags() {
    return Arrays.asList(new DAGFactory[][] {{new HashDAGFactory()}});
  }

  private interface DAGFactory {
    public DAG<Revision> getDag(Collection<Revision> values);
  }

  private static class HashDAGFactory implements DAGFactory {
    public DAG<Revision> getDag(Collection<Revision> values) {
      return new HashDAG(values);
    }
  }

  private final DAGFactory factory;

  public DAGTest(DAGFactory fact) {
    this.factory = fact;
  }

  @Test
  public void singleton() {
    List<Revision> values = new ArrayList<Revision>(1);
    Revision single = new Revision(new byte[NigoriConstants.B_SHA1]);
    values.add(single);
    DAG<Revision> dag = factory.getDag(values);
    Collection<Node<Revision>> starts = dag.getStarts();
    assertEquals(1, starts.size());
    assertThat(starts, everyItem(nodeContains(single)));

    Collection<Node<Revision>> heads = dag.getHeads();
    assertEquals(1, heads.size());
    assertThat(heads, everyItem(nodeContains(single)));

  }

  private org.hamcrest.Matcher<Node<Revision>> nodeContains(final Revision revision) {
    return new BaseMatcher<Node<Revision>>() {

      @Override
      public boolean matches(Object item) {
        if (!(item instanceof Node)) {
          return false;
        }
        @SuppressWarnings("unchecked")
        Node<Revision> node = (Node<Revision>) item;
        return revision.equals(node.getValue());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("expected " + revision);
      }
    };

  }
}
