/*
 * Copyright 2015 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon.ds.trie;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Created by mohan.pandian on 20/11/15.
 */
public class Trie<K, V> {
    private final TrieNode<K, V> root;

    public Trie() {
        root = new TrieNode<>();
    }

    /**
     * Check for current node be null,
     * Check if this node matches with the current position node
     * If yes moved to the curr.firstChild and increment currPosition
     * If not then move to curr.rightSibling and repeat from 1st step
     * If no match found and reached the end then add all nodes left as firstChild
     **/
    public void add(List<KeyWrapper<K>> keys, V value) {
        if (root.children.isEmpty()) {
            addChainAsFirstChild(root, keys, value);
            return;
        }

        TrieNode<K, V> currentParent = root;
        for (int i = 0; i < keys.size(); i++) {
            TrieNode<K, V> matchingNode = matchNode(currentParent, keys.get(i));
            if (matchingNode != null) {
                currentParent = matchingNode;
                matchingNode.value = i == keys.size() - 1 ? value : matchingNode.value;
                continue;
            }

            TrieNode<K, V> newNode = new TrieNode<>();
            newNode.key = keys.get(i).key;
            newNode.matchAny = keys.get(i).wildCard;
            newNode.greedyMatchAny = keys.get(i).greedyWildCard;
            newNode.value = i == keys.size() - 1 ? value : null;

            if (newNode.matchAny) {
                currentParent.wildChild = newNode;
            } else if (newNode.greedyMatchAny) {
                currentParent.greedyWildChild = newNode;
            } else {
                currentParent.children.put(newNode.key, newNode);
            }

            addChainAsFirstChild(newNode, keys.subList(i + 1, keys.size()), value);
            return;
        }
    }

    private TrieNode<K, V> matchNode(TrieNode<K, V> parent, KeyWrapper<K> wrapper) {
        if (parent == null) {
            return null;
        }

        if (wrapper.wildCard && parent.wildChild != null) {
            return parent.wildChild;
        } else if (wrapper.greedyWildCard && parent.greedyWildChild != null) {
            return parent.greedyWildChild;
        } else {
            return parent.children.get(wrapper.key);
        }
    }

    private void  addChainAsFirstChild(TrieNode<K, V> node, List<KeyWrapper<K>> keys, V value) {
        TrieNode<K, V> currentNode = node;
        for (int i = 0; i < keys.size(); i++) {
            TrieNode<K, V> newNode = new TrieNode<>();
            newNode.key = keys.get(i).key;
            newNode.matchAny = keys.get(i).wildCard;
            newNode.greedyMatchAny = keys.get(i).greedyWildCard;
            newNode.value = i == keys.size() - 1 ? value : null;

            if (newNode.matchAny) {
                currentNode.wildChild = newNode;
            } else if (newNode.greedyMatchAny) {
                currentNode.greedyWildChild = newNode;
            } else {
                currentNode.children.put(newNode.key, newNode);
            }

            currentNode = newNode;
        }
    }

    public V get(K[] keys) {
        return get(root, 0, keys);
    }

    private V get(TrieNode<K, V> node, int level, K[] keys) {
        if (node == null) {
            return null;
        }

        TrieNode<K, V> concreteChild = node.children.get(keys[level]);
        if (concreteChild != null) {
            V value = evaluateTerminationAndGet(concreteChild, level, keys);
            if (value == null && node.wildChild != null) {
                value = evaluateTerminationAndGet(node.wildChild, level, keys);
            }

            if (value == null && node.greedyWildChild != null) {
                value = get(node.greedyWildChild, level, keys);
            }

            if (value != null) {
                return value;
            }
        }

        if (node.wildChild != null) {
            V value = evaluateTerminationAndGet(node.wildChild, level, keys);
            if (value == null && node.greedyWildChild != null) {
                value = get(node.greedyWildChild, level, keys);
            }

            if (value != null) {
                return value;
            }
        }

        if (node.greedyWildChild != null) {
            if (node.greedyWildChild.children.isEmpty()) {
                return node.greedyWildChild.value;
            } else {
                for (int i = level + 1; i < keys.length; i++) {
                    TrieNode<K, V> currentLookUp = findMatch(node.greedyWildChild, keys[i]);
                    if (currentLookUp != null) {
                        if (i == keys.length - 1) {
                            return currentLookUp.value;
                        } else {
                            return get(currentLookUp, i + 1, keys);
                        }
                    }
                }

                return node.greedyWildChild.value;
            }
        }

        return null;
    }

    private V evaluateTerminationAndGet(TrieNode<K, V> node, int level, K[] keys) {
        if (level == keys.length - 1) {
            return node.value;
        } else {
            return get(node, level + 1, keys);
        }
    }

    public TrieNode<K, V> findMatch(TrieNode<K, V> parent, K key) {
        TrieNode<K, V> node = parent.children.get(key);
        if (node != null) {
            return node;
        }

        if (parent.wildChild != null) {
            return parent.wildChild;
        }

        if (parent.greedyWildChild != null) {
            return parent.greedyWildChild;
        }

        // No match found, no wild or greedy wild child
        return null;
    }

    public List<List<String>> printAllPaths(String separator) {
        List<List<String>> paths = new ArrayList<>();
        traverseAndPrint(root, new ArrayList<>(), separator, paths);
        return paths;
    }

    private void traverseAndPrint(TrieNode<K, V> node, List<String> pathStr, String separator, List<List<String>> paths) {
        if (node == null) {
            return;
        }

        List<String> pathParts = new ArrayList<>(pathStr);
        if (!pathStr.isEmpty() && !separator.equals(pathStr.get(pathStr.size() - 1))) {
            pathParts.add(separator);
        }
        if (node.key != null) {
            pathParts.add(node.key.toString());
        }
        if (node.matchAny) {
            pathParts.add("{}");
        }
        if (node.greedyMatchAny) {
            pathParts.add("**");
        }
        if (node.value != null) {
            paths.add(pathParts);
        }

        node.children.values().forEach(n -> traverseAndPrint(n, pathParts, separator, paths));
    }
}
