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
        Boolean insertAsrightSibling = false;

        while (!EXIT_STATUS && currPosition <= noOfParts) {
            if (currNode == null) {          // this means insert all parts of url as firstChild
                for (int pos = currPosition; pos <= noOfParts; pos++) {
                    if (!insertAsrightSibling) {
                        currNode = prevNode.firstChild = new TrieNode<>();
                        currNode.parent = prevNode;
                    } else {
                        currNode = prevNode.rightSibling = new TrieNode<>();
                        currNode.parent = prevNode;
                        insertAsrightSibling = false;
                    }
                    currNode.key = keys[pos];

                    // Mark this node as a placeholder if key is null
                    if (currNode.key == null) {
                        currNode.matchAll = true;
                    }

                    if (pos == noOfParts) {
                        currNode.value = value;
                    }

                    // Check if new node is on right of placeHolder Node, if not move it to right
                    if (currNode.parent.matchAll && currNode.parent.rightSibling == currNode) {
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
                    insertAsrightSibling = false;
                }
            } else {
                prevNode = currNode;
                currNode = currNode.rightSibling;
                insertAsrightSibling = true;
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
            if (node.matchAll || node.key.equals(keys[level])) {
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

                    if (currentNode.matchAll) {
                        if (level == (keys.length - 1)) {
                            value = node.value;
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
        if (node.matchAll) {
            strBuilder.append("*");
        }
        if (node.value != null) {
            System.out.println(strBuilder);
        }

        traverseAndPrint(node.firstChild, strBuilder.toString(), separator);
        traverseAndPrint(node.rightSibling, pathStr, separator);
    }
}
