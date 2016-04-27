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

/**
 * Created by mohan.pandian on 20/11/15.
 */
public class Trie<K,V> {
    private TrieNode<K,V> root;

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
        Boolean EXIT_STATUS = false;
        TrieNode<K,V> currNode, prevNode;

        int noOfParts = keys.length - 1;
        int currPosition = 0;

        currNode = root.firstChild;
        prevNode = root;
        Boolean insertAsRightSibling = false;

        while (!EXIT_STATUS && currPosition <= noOfParts) {
            if (currNode == null) {          // this means insert all parts of url as firstChild
                for (int pos = currPosition; pos <= noOfParts; pos++) {
                    if (!insertAsRightSibling) {
                        currNode = prevNode.firstChild = new TrieNode<>();
                        currNode.parent = prevNode;
                    } else {
                        currNode = prevNode.rightSibling = new TrieNode<>();
                        currNode.parent = prevNode.parent;
                        insertAsRightSibling = false;
                    }
                    currNode.key = keys[pos];

                    // Mark this node as a placeholder if key is null
                    if (currNode.key == null) {
                        currNode.matchAny = true;
                    }

                    if (pos == noOfParts) {
                        currNode.value = value;
                    }

                    // Check if new node is on right of placeHolder Node, if not move it to right
                    if (currNode.parent.matchAny && currNode.parent.rightSibling == currNode) {
                        currNode.parent = prevNode.parent;
                        if (prevNode == prevNode.parent.rightSibling) {
                            prevNode.parent.rightSibling = currNode;
                        } else {
                            prevNode.parent.firstChild = currNode;
                        }

                        prevNode.rightSibling = null;
                        currNode.rightSibling = prevNode;
                        prevNode.parent = currNode;
                    }

                    prevNode = currNode;
                    currNode = currNode.firstChild;
                    currPosition++;
                }
                EXIT_STATUS = true;
            } else if ((currNode.key == null && keys[currPosition] == null) || (currNode.key != null && currNode.key.equals(keys[currPosition]))) {
                if (currPosition == noOfParts) {
                    currNode.value = value;
                    EXIT_STATUS = true;
                } else {
                    currPosition++;
                    prevNode = currNode;
                    currNode = currNode.firstChild;
                    insertAsRightSibling = false;
                }
            } else {
                prevNode = currNode;
                currNode = currNode.rightSibling;
                insertAsRightSibling = true;
            }
        }
    }

    public V get(K[] keys) {
        return get(root.firstChild, 0, keys);
    }

    /**
     * 1. First check if the given string is matched with a placeholder node (matchAll:true)?
     * 2. If yes, assign the value to a path variable
     * 3. where the input url ends, check and return value from
     *    that node else ERROR msg
     * 4. back-tracking rules to be defined
     **/
    private V get(TrieNode<K,V> node, int level, K[] keys) {
        V value = null;
        if (node != null) {
            // Normal matching scenario
            if (node.matchAny || node.key.equals(keys[level])) {
                if (level == (keys.length - 1)) {
                    value = node.value;
                } else if (level < (keys.length - 1)) {
                    List<TrieNode<K, V>> matchingChildren = getMatchingChildren(getAllChildren(node), keys[level + 1]);
                    value = null;
                    for (TrieNode<K, V> child : matchingChildren) {
                        value = get(child, level + 1, keys);
                        if (value != null) {
                            break;
                        }
                    }
                }
            } else if (node.parent != null && node.parent.parent == null) {
                value = get(node.rightSibling, level, keys);
            }
        }

        // Case if node == null, a null value for buildable will be returned
        return value;
    }

    private List<TrieNode<K, V>> getAllChildren(TrieNode<K, V> node) {
        List<TrieNode<K, V>> children = new ArrayList<>();
        if (node != null && node.firstChild != null) {
            children.add(node.firstChild);
            TrieNode<K, V> currentNode = node.firstChild;
            while ((currentNode = currentNode.rightSibling) != null) {
                children.add(currentNode);
            }
        }

        return children;
    }

    private List<TrieNode<K, V>> getMatchingChildren(List<TrieNode<K, V>> children, K key) {
        List<TrieNode<K, V>> wildcardChildren = new ArrayList<>();
        List<TrieNode<K, V>> matchingChildren = new ArrayList<>();

        for (TrieNode<K, V> child : children) {
            if (child.key == null) {
                wildcardChildren.add(child);
            } else if (key != null && key.equals(child.key)) {
                matchingChildren.add(child);
            }
        }
        matchingChildren.addAll(wildcardChildren);

        return matchingChildren;
    }

    public void printAllPaths(String separator) {
        traverseAndPrint(root, "", separator);
    }

    private void traverseAndPrint(TrieNode<K, V> node, String pathStr, String separator) {
        if (node == null) {
            return;
        }

        StringBuilder strBuilder = new StringBuilder(pathStr);
        String key = (String) node.key;
        if (node.parent != null) {
            if (node.parent.parent == null && key != null && !key.isEmpty() && key.charAt(0) == '_' && key.charAt(key.length() - 1) == '_') {
                strBuilder.append(key.substring(1, key.length() - 1)).append(" ");
            } else {
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
            }
        }

        traverseAndPrint(node.firstChild, strBuilder.toString(), separator);
        traverseAndPrint(node.rightSibling, pathStr, separator);
    }
}
