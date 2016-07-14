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
import java.util.Arrays;
import java.util.List;

/**
 * Created by mohan.pandian on 20/11/15.
 */
public class Trie<K, V> {
    private TrieNode<K, V> root;

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
    public void add(K[] keys, V value) {
        if (root.firstChild == null) {
            addChainAsFirstChild(root, keys, value);
            return;
        }

        TrieNode currentNode = root.firstChild;
        TrieNode currentParent = root;
        for (int i = 0; i < keys.length; i++) {
            TrieNode matchingNode = findMatchingNode(currentNode, keys[i]);
            if (matchingNode != null) {
                currentParent = matchingNode;
                currentNode = matchingNode.firstChild;
                matchingNode.value = i == keys.length - 1 ? value : matchingNode.value;
                continue;
            }

            TrieNode newNode = new TrieNode();
            newNode.key = keys[i];
            newNode.matchAny = keys[i] == null;
            newNode.value = i == keys.length - 1 ? value : null;

            if (currentNode == null) {
                currentParent.firstChild = newNode;
            } else if (newNode.key != null) {
                newNode.rightSibling = currentParent.firstChild;
                currentParent.firstChild = newNode;
            } else {
                while (currentNode.rightSibling != null) {
                    currentNode = currentNode.rightSibling;
                }
                currentNode.rightSibling = newNode;
                currentParent.wildChild = newNode;
            }

            addChainAsFirstChild(newNode, Arrays.copyOfRange(keys, i + 1, keys.length), value);
            return;
        }
    }

    private TrieNode findMatchingNode(TrieNode node, K key) {
        if (node == null) {
            return null;
        }

        TrieNode correctNode = null;
        if ((node.matchAny && key == null) || (!node.matchAny && node.key.equals(key))) {
            correctNode = node;
        } else {
            TrieNode current = node.rightSibling;
            while (current != null) {
                if ((current.matchAny && key == null) || (!current.matchAny && current.key.equals(key))) {
                    correctNode = current;
                    break;
                }
                current = current.rightSibling;
            }
        }
        return correctNode;
    }

    private void addChainAsFirstChild(TrieNode node, K[] keys, V value) {
        TrieNode currentNode = node;
        for (int i = 0; i < keys.length; i++) {
            TrieNode newNode = new TrieNode();
            newNode.key = keys[i];
            newNode.matchAny = keys[i] == null;
            newNode.value = i == keys.length - 1 ? value : null;

            currentNode.firstChild = newNode;
            if (newNode.matchAny) {
                currentNode.wildChild = newNode;
            }
            currentNode = currentNode.firstChild;
        }
    }

    public V get(K[] keys) {
        return get(root, 0, keys);
    }

    /**
     * 1. First check if the given string is matched with a placeholder node (matchAll:true)?
     * 2. If yes, assign the value to a path variable
     * 3. where the input url ends, check and return value from
     * that node else ERROR msg
     * 4. back-tracking rules to be defined
     **/
    private V get(TrieNode<K, V> node, int level, K[] keys) {
        V value = null;
        if (node != null) {
            TrieNode<K, V> matchingChild = node.firstChild;
            TrieNode<K, V> wildChild = node.wildChild;

            while (matchingChild != null) {
                if (matchingChild.matchAny || matchingChild.key.equals(keys[level])) {
                    break;
                }
                matchingChild = matchingChild.rightSibling;
            }

            if (matchingChild == null) {
                if (node.matchAny) {
                    return node.value;
                }
                return null;
            }

            if (level == keys.length - 1) {
                value = matchingChild.value;
            } else {
                value = get(matchingChild, level + 1, keys);
            }

            if (value == null && !matchingChild.matchAny && wildChild != null) {
                if (level == keys.length - 1) {
                    value = wildChild.value;
                } else {
                    value = get(wildChild, level + 1, keys);
                }
            }
        }

        // Case if node == null, a null value for buildable will be returned
        return value;
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
            pathParts.add("*");
        }
        if (node.value != null) {
            paths.add(pathParts);
        }

        traverseAndPrint(node.firstChild, pathParts, separator, paths);
        traverseAndPrint(node.rightSibling, pathStr, separator, paths);
    }
}
