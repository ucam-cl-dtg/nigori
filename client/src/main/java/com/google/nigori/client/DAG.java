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

import java.util.Collection;

/**
 * A Directed Acyclic Graph of {@link Node Nodes} of type T
 * 
 * @see MigoriDatastore
 * @author drt24
 * 
 */
public interface DAG<T> {

  /**
   * Get the heads of the DAG - those nodes which have no successors
   * 
   * @return
   */
  Collection<Node<T>> getHeads();

  /**
   * Get the starts of the DAG - those nodes which have no predecessors
   * 
   * @return
   */
  Collection<Node<T>> getStarts();

  /**
   * Get the predecessors of a node
   * 
   * @param node the node to get the predecessors of
   * @return a collection of those predecessors - empty if there are none
   * @throws MissingNodeException 
   */
  Collection<Node<T>> getPredecessors(Node<T> node) throws MissingNodeException;

  /**
   * Get the successors of a node
   * 
   * @param node the node to get the successors of
   * @return a collection of those successors - empty if there are none
   */
  Collection<Node<T>> getSuccessors(Node<T> node);

  /**
   * Get the common predecessor of two nodes or null if there is none
   * 
   * @param firstNode
   * @param secondNode
   * @return the common predecessor node or null if there is no common predecessor.
   */
  Node<T> getCommonPredecessor(Node<T> firstNode, Node<T> secondNode);

  /**
   * Nodes which encapsulate the type T
   * 
   * @author drt24
   * 
   * @param <T>
   */
  public interface Node<T> {
    T getValue();
  }
}
