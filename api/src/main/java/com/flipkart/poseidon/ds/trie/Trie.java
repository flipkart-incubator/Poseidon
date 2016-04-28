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

import java.util.Arrays;

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

            if (newNode.key != null) {
                newNode.rightSibling = currentParent.firstChild;
                currentParent.firstChild = newNode;
            } else {
                currentNode.rightSibling = newNode;
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
            currentNode = currentNode.firstChild;
        }
    }

    public V get(K[] keys) {
        return get(root.firstChild, 0, keys);
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
            // Normal matching scenario
            if (node.matchAny || node.key.equals(keys[level])) {
                if (level == (keys.length - 1)) {
                    value = node.value;
                } else if (level < (keys.length - 1)) {
                    value = get(node.firstChild, level + 1, keys);
                }

                // If value is null here then backtrack
                if (value == null) {
                    TrieNode<K, V> currentNode = node;
                    TrieNode<K, V> nextNode = node.rightSibling;
                    while (nextNode != null) {
                        currentNode = nextNode;
                        nextNode = nextNode.rightSibling;
                    }

                    if (currentNode.matchAny) {
                        if (level == (keys.length - 1)) {
                            value = currentNode.value;
                        } else {
                            value = get(node.firstChild, level + 1, keys);
                        }
                    }
                } else {
                    return value;
                }
            } else {
                value = get(node.rightSibling, level, keys);
            }
        }

        // Case if node == null, a null value for buildable will be returned
        return value;
    }

    public void printAllPaths(String separator) {
        traverseAndPrint(root, "", separator);
    }

    private void traverseAndPrint(TrieNode<K, V> node, String pathStr, String separator) {
        if (node == null) {
            return;
        }

        StringBuilder strBuilder = new StringBuilder(pathStr);
        if (!pathStr.endsWith(separator)) {
            strBuilder.append(separator);
        }
        if (node.key != null) {
            strBuilder.append(node.key);
        }
        if (node.matchAny) {
            strBuilder.append("*");
        }
        if (node.value != null) {
            System.out.println(strBuilder);
        }

        traverseAndPrint(node.firstChild, strBuilder.toString(), separator);
        traverseAndPrint(node.rightSibling, pathStr, separator);
    }
}
