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

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.everyItem;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;

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

import com.google.nigori.client.DAG.DAGFactory;
import com.google.nigori.common.NigoriConstants;
import com.google.nigori.common.Revision;

/**
 * @author drt24
 * 
 */
@RunWith(Parameterized.class)
public class DAGTest {

  private static final int HASH = NigoriConstants.B_SHA1;
  private static final Revision roota = new Revision(new byte[HASH]);
  private static final Revision rootb;
  private static final Revision child1;
  private static final Revision child2;
  private static final Revision merge;
  static {
    // DAG:
    //    roota           rootb
    //   /    \
    //  /      \
    // child1  child2
    //  \      /
    //    merge

    // Child1
    byte[] childBytes = new byte[HASH * 2];
    byte[] cb = "child1".getBytes();
    System.arraycopy(cb, 0, childBytes, 0, cb.length);
    System.arraycopy(roota.getBytes(), 0, childBytes, HASH, HASH);
    child1 = new Revision(childBytes);

    // Child2
    byte[] child2Bytes = new byte[HASH * 2];
    byte[] cb2 = "child2".getBytes();
    System.arraycopy(cb2, 0, child2Bytes, 0, cb2.length);
    System.arraycopy(roota.getBytes(), 0, child2Bytes, HASH, HASH);
    child2 = new Revision(child2Bytes);

    // merge
    byte[] mergeBytes = new byte[HASH * 3];
    byte[] mb = "merge".getBytes();
    System.arraycopy(mb, 0, mergeBytes, 0, mb.length);
    System.arraycopy(child1.getBytes(), 0, mergeBytes, HASH, HASH);
    System.arraycopy(child2.getBytes(), 0, mergeBytes, HASH * 2, HASH);
    merge = new Revision(mergeBytes);

    byte[] rootBytes = new byte[HASH];
    byte[] rb = "rootb".getBytes();
    System.arraycopy(rb, 0, rootBytes, 0, rb.length);
    rootb = new Revision(rootBytes);
  }

  @Parameters
  public static Collection<DAGFactory[]> dags() {
    return Arrays.asList(new DAGFactory[][] {{new HashDAG.HashDAGFactory()}});
  }

  private final DAGFactory factory;

  public DAGTest(DAGFactory fact) {
    this.factory = fact;
  }

  @Test
  public void singleton() {
    List<Revision> values = new ArrayList<Revision>(1);
    values.add(roota);
    DAG<Revision> dag = factory.getDag(values);
    Collection<Node<Revision>> starts = dag.getStarts();
    assertEquals(1, starts.size());
    assertThat(starts, everyItem(nodeContains(roota)));

    Collection<Node<Revision>> heads = dag.getHeads();
    assertEquals(1, heads.size());
    assertThat(heads, everyItem(nodeContains(roota)));
  }

  @Test
  public void two() {
    List<Revision> values = new ArrayList<Revision>(2);
    values.add(roota);
    values.add(child1);
    DAG<Revision> dag = factory.getDag(values);
    Collection<Node<Revision>> starts = dag.getStarts();
    assertEquals(1, starts.size());
    assertThat(starts, everyItem(nodeContains(roota)));

    Collection<Node<Revision>> heads = dag.getHeads();
    assertEquals(1, heads.size());
    assertThat(heads, everyItem(nodeContains(child1)));
  }

  @Test
  public void diverge() {
    List<Revision> values = new ArrayList<Revision>(2);
    values.add(roota);
    values.add(child1);
    values.add(child2);
    DAG<Revision> dag = factory.getDag(values);
    Collection<Node<Revision>> starts = dag.getStarts();
    assertEquals(1, starts.size());
    assertThat(starts, everyItem(nodeContains(roota)));

    Collection<Node<Revision>> heads = dag.getHeads();
    assertEquals(2, heads.size());
    assertThat(heads, hasItem(nodeContains(child1)));
    assertThat(heads, hasItem(nodeContains(child2)));
  }

  @Test
  public void merge() {
    List<Revision> values = new ArrayList<Revision>(2);
    values.add(roota);
    values.add(child1);
    values.add(child2);
    values.add(merge);
    DAG<Revision> dag = factory.getDag(values);
    Collection<Node<Revision>> starts = dag.getStarts();
    assertEquals(1, starts.size());
    assertThat(starts, everyItem(nodeContains(roota)));

    Collection<Node<Revision>> heads = dag.getHeads();
    assertEquals(1, heads.size());
    assertThat(heads, everyItem(nodeContains(merge)));
  }

  @SuppressWarnings("unchecked")
  // generic varargs
  @Test
  public void mergemultiroot() {
    List<Revision> values = new ArrayList<Revision>(2);
    values.add(roota);
    values.add(child1);
    values.add(child2);
    values.add(merge);
    values.add(rootb);
    DAG<Revision> dag = factory.getDag(values);
    Collection<Node<Revision>> starts = dag.getStarts();
    assertEquals(2, starts.size());
    assertThat(starts, hasItems(nodeContains(roota), nodeContains(rootb)));

    Collection<Node<Revision>> heads = dag.getHeads();
    assertEquals(2, heads.size());
    assertThat(heads, hasItems(nodeContains(merge), nodeContains(rootb)));
  }

  @SuppressWarnings("unchecked")
  // generic varargs
  @Test
  public void iterator() {
    List<Revision> values = new ArrayList<Revision>(2);
    values.add(roota);
    values.add(child1);
    values.add(child2);
    values.add(merge);
    values.add(rootb);
    DAG<Revision> dag = factory.getDag(values);

    List<Node<Revision>> nodes = new ArrayList<Node<Revision>>();
    for (Node<Revision> next : dag) {
      nodes.add(next);
    }

    assertThat(nodes, hasItems(nodeContains(roota), nodeContains(child1), nodeContains(child2),
        nodeContains(merge), nodeContains(rootb)));
    assertEquals(values.size(), nodes.size());
    assertThat(nodes.indexOf(factory.getNode(merge)), lessThan(nodes.indexOf(factory.getNode(roota))));
    assertThat(nodes.indexOf(factory.getNode(rootb)), lessThan(nodes.indexOf(factory.getNode(child1))));
    assertThat(nodes.indexOf(factory.getNode(rootb)), lessThan(nodes.indexOf(factory.getNode(child2))));
    assertThat(nodes.indexOf(factory.getNode(child1)), lessThan(nodes.indexOf(factory.getNode(roota))));
    assertThat(nodes.indexOf(factory.getNode(child2)), lessThan(nodes.indexOf(factory.getNode(roota))));
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
        description.appendText("" + revision);
      }
    };

  }
}
