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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.nigori.common.NigoriConstants;
import com.google.nigori.common.Revision;

/**
 * @author drt24
 * 
 */
public class HashDAG implements DAG<Revision> {

  public static final int HASH_SIZE = NigoriConstants.B_SHA1;

  private Set<Node<Revision>> heads;
  private Set<Node<Revision>> starts;
  private Map<RevIndex, RNode> nodes;
  private Map<RevIndex, Collection<RNode>> allSuccessors;

  @Override
  public Collection<Node<Revision>> getHeads() {
    return Collections.unmodifiableCollection(heads);
  }

  @Override
  public Collection<Node<Revision>> getStarts() {
    return Collections.unmodifiableCollection(starts);
  }

  public HashDAG(Collection<Revision> values) {
    heads = new HashSet<Node<Revision>>();
    starts = new HashSet<Node<Revision>>();
    nodes = new HashMap<RevIndex, RNode>();
    allSuccessors = new HashMap<RevIndex, Collection<RNode>>();
    for (Revision value : values) {
      addNode(value);
    }
    for (Node<Revision> node : starts) {
      walkGraph((RNode) node);
    }
    allSuccessors = null;// Allow garbage collection
  }

  /**
   * Walks depth first only adding successors once so as to only visit each branch once
   */
  private void walkGraph(RNode node) {
    Collection<RNode> successors = allSuccessors.get(node.index);
    if (successors == null){// No successors so a head
      heads.add(node);
      return;
    }
    for (RNode successor : successors) {
      if (node.addSuccessor(successor)) {
        walkGraph(successor);
      }
    }
  }

  private Collection<Node<Revision>> collectionCast(Collection<RNode> coll) {
    Collection<Node<Revision>> answer = new ArrayList<Node<Revision>>();
    for (RNode node : coll) {
      answer.add(node);
    }
    return answer;
  }

  private Collection<Node<Revision>> collectionResolve(Collection<RevIndex> coll)
      throws MissingNodeException {
    Collection<Node<Revision>> answer = new ArrayList<Node<Revision>>();
    for (RevIndex index : coll) {
      Node<Revision> node = nodes.get(index);
      if (node != null) {
        answer.add(node);
      } else
        throw new MissingNodeException();

    }
    return answer;
  }

  @Override
  public Collection<Node<Revision>> getPredecessors(Node<Revision> node)
      throws MissingNodeException {
    if (node instanceof RNode) {
      return collectionResolve(((RNode) node).getPredecessors());
    } else
      throw new IllegalStateException("Mixed types");
  }

  @Override
  public Collection<Node<Revision>> getSuccessors(Node<Revision> node) {
    if (node instanceof RNode) {
      return collectionCast(((RNode) node).getSuccessors());
    } else
      throw new IllegalStateException("Mixed types");
  }

  @Override
  public Node<Revision> getCommonPredecessor(Node<Revision> firstNode, Node<Revision> secondNode) {
    throw new UnsupportedOperationException("Not yet implemented");
    // TODO(drt24) implement
  }

  private Node<Revision> addNode(Revision value) {
    RNode node = new RNode(value);
    nodes.put(node.index, node);
    if (node.value.getBytes().length == HASH_SIZE) {
      starts.add(node);
    }
    // Record ourself as a successor of all our predecessors
    for (RevIndex pIndex : node.predecessors) {
      Collection<RNode> psuccessors = allSuccessors.get(pIndex);
      if (psuccessors == null) {
        psuccessors = new ArrayList<RNode>();
        allSuccessors.put(pIndex, psuccessors);
      }
      psuccessors.add(node);
    }
    return node;
  }

  /**
   * @param revBytes
   * @return
   */
  private static Collection<RevIndex> predecessors(byte[] revBytes) {
    int numHashes = revBytes.length / HASH_SIZE;
    if (numHashes > 1) {
      Collection<RevIndex> answer = new ArrayList<RevIndex>();
      for (int i = 1; i < numHashes; ++i) {
        RevIndex idx =
            new RevIndex(Arrays.copyOfRange(revBytes, i * HASH_SIZE, HASH_SIZE * (i + 1)));
        answer.add(idx);
      }
      return answer;
    } else {
      return Collections.emptyList();
    }
  }

  private static class RNode implements Node<Revision> {

    private final Revision value;
    private final RevIndex index;
    private Collection<RevIndex> predecessors;
    private Set<RNode> successors;

    public RNode(Revision value){
      byte[] revBytes = value.getBytes();
      if (revBytes.length < HASH_SIZE) {
        throw new IllegalArgumentException(String.format(
            "Revision too small, must be at least %d bytes long", HASH_SIZE));
      }
      if (revBytes.length % HASH_SIZE != 0) {
        throw new IllegalArgumentException(String.format(
            "Revision length must be a multiple of %d bytes long", HASH_SIZE));
      }

      byte[] revIdx = Arrays.copyOf(revBytes, HASH_SIZE);
      RevIndex index = new RevIndex(revIdx);
      this.value = value;
      this.index = index;
      this.predecessors = predecessors(revBytes);
      this.successors = new HashSet<RNode>();
    }

    @Override
    public Revision getValue() {
      return value;
    }

    public boolean addSuccessor(RNode successor) {
      return successors.add(successor);
    }

    protected Collection<RevIndex> getPredecessors() {
      return predecessors;
    }

    protected Collection<RNode> getSuccessors() {
      return successors;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((index == null) ? 0 : index.hashCode());
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
      RNode other = (RNode) obj;
      if (index == null) {
        if (other.index != null)
          return false;
      } else if (!index.equals(other.index))
        return false;
      return true;
    }
  }

  private static class RevIndex {
    private final byte[] hash;

    public RevIndex(byte[] hash) {
      this.hash = hash;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(hash);
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
      RevIndex other = (RevIndex) obj;
      if (!Arrays.equals(hash, other.hash))
        return false;
      return true;
    }
  }

  @Override
  public Iterator<Node<Revision>> iterator() {
    return new HashIterator();
  }

  /**
   * Do a breadth first walk of the graph avoiding duplicates
   * @author drt24
   *
   */
  private class HashIterator implements Iterator<Node<Revision>> {

    private Set<Node<Revision>> seen;
    private Iterator<Node<Revision>> currentIterator = null;
    private List<Iterator<Node<Revision>>> iteratorList;
    private Node<Revision> next = null;

    /**
     * @param heads
     */
    public HashIterator() {
      seen = new HashSet<Node<Revision>>();
      currentIterator = heads.iterator();
      if (currentIterator.hasNext()){
        next = currentIterator.next();
        seen.add(next);
      }
      iteratorList = new LinkedList<Iterator<Node<Revision>>>();
    }

    @Override
    public boolean hasNext() {
      return (next != null);
    }

    @Override
    public Node<Revision> next() {
      if (next == null) {
        throw new NoSuchElementException("No next element");
      }
      try {
        Collection<Node<Revision>> predecessors = getPredecessors(next);
        iteratorList.add(predecessors.iterator());
        Node<Revision> last = next;
        setNext();
        return last;
      } catch (MissingNodeException e) {
        throw new IllegalStateException(e);
      }
    }

    private void setNext() {
      while (currentIterator.hasNext()) {
        Node<Revision> potential = currentIterator.next();
        if (!seen.contains(potential)) {
          next = potential;
          seen.add(next);
          return;
        }
      }
      if (!iteratorList.isEmpty()) {
        currentIterator = iteratorList.remove(0);
        setNext();
      } else {
        next = null;
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  public static class HashDAGFactory implements DAGFactory {
    public DAG<Revision> getDag(Collection<Revision> values) {
      return new HashDAG(values);
    }

    @Override
    public Node<Revision> getNode(Revision rev) {
      return new RNode(rev);
    }
  }
}
