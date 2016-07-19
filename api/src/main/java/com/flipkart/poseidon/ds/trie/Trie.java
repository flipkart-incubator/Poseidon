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
    public void add(List<KeyWrapper<K>> keys, V value) {
        if (root.firstChild == null) {
            addChainAsFirstChild(root, keys, value);
            return;
        }

        TrieNode currentNode = root.firstChild;
        TrieNode currentParent = root;
        for (int i = 0; i < keys.size(); i++) {
            TrieNode matchingNode = findMatchingNode(currentNode, keys.get(i));
            if (matchingNode != null) {
                currentParent = matchingNode;
                currentNode = matchingNode.firstChild;
                matchingNode.value = i == keys.size() - 1 ? value : matchingNode.value;
                continue;
            }

            TrieNode newNode = new TrieNode();
            newNode.key = keys.get(i).key;
            newNode.matchAny = keys.get(i).wildCard;
            newNode.greedyMatchAny = keys.get(i).greedyWildCard;
            newNode.value = i == keys.size() - 1 ? value : null;

            if (currentNode == null) {
                currentParent.firstChild = newNode;
            } else if (!newNode.matchAny && !newNode.greedyMatchAny) {
                newNode.rightSibling = currentParent.firstChild;
                currentParent.firstChild = newNode;
            } else if (newNode.matchAny) {
                if (currentNode.greedyMatchAny) {
                    newNode.rightSibling = currentNode;
                    currentParent.firstChild = newNode;
                } else {
                    while (currentNode.rightSibling != null) {
                        if (currentNode.rightSibling.greedyMatchAny) {
                            break;
                        }
                        currentNode = currentNode.rightSibling;
                    }

                    if (currentNode.rightSibling != null && currentNode.rightSibling.greedyMatchAny) {
                        newNode.rightSibling = currentNode.rightSibling;
                        currentNode.rightSibling = newNode;
                    } else {
                        currentNode.rightSibling = newNode;
                    }

                    currentParent.wildChild = newNode;
                }
            } else if (newNode.greedyMatchAny) {
                if (currentNode.matchAny) {
                    currentParent.rightSibling = newNode;
                } else {
                    while (currentNode.rightSibling != null) {
                        currentNode = currentNode.rightSibling;
                    }

                    currentNode.rightSibling = newNode;
                    currentParent.greedyWildChild = newNode;
                }
            }

            addChainAsFirstChild(newNode, keys.subList(i + 1, keys.size()), value);
            return;
        }
    }

    private TrieNode findMatchingNode(TrieNode node, KeyWrapper<K> wrapper) {
        if (node == null) {
            return null;
        }

        TrieNode correctNode = null;
        if ((node.matchAny && wrapper.wildCard) ||
                (node.greedyMatchAny && wrapper.greedyWildCard) ||
                (!node.matchAny && !node.greedyMatchAny && node.key.equals(wrapper.key))) {
            correctNode = node;
        } else {
            TrieNode current = node.rightSibling;
            while (current != null) {
                if ((current.matchAny && wrapper.wildCard) ||
                        (current.greedyMatchAny && wrapper.greedyWildCard) ||
                        (!current.matchAny && !current.greedyMatchAny && current.key.equals(wrapper.key))) {
                    correctNode = current;
                    break;
                }
                current = current.rightSibling;
            }
        }
        return correctNode;
    }

    private void addChainAsFirstChild(TrieNode node, List<KeyWrapper<K>> keys, V value) {
        TrieNode currentNode = node;
        for (int i = 0; i < keys.size(); i++) {
            TrieNode newNode = new TrieNode();
            newNode.key = keys.get(i).key;
            newNode.matchAny = keys.get(i).wildCard;
            newNode.greedyMatchAny = keys.get(i).greedyWildCard;
            newNode.value = i == keys.size() - 1 ? value : null;

            currentNode.firstChild = newNode;
            if (newNode.matchAny) {
                currentNode.wildChild = newNode;
            } else if (newNode.greedyMatchAny) {
                currentNode.greedyWildChild = newNode;
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
            TrieNode<K, V> wildPathChild = node.greedyWildChild;

            while (matchingChild != null) {
                if (matchingChild.matchAny || matchingChild.greedyMatchAny || matchingChild.key.equals(keys[level])) {
                    break;
                }
                matchingChild = matchingChild.rightSibling;
            }

            if (matchingChild == null) {
                return null;
            }

            if (level == keys.length - 1) {
                value = matchingChild.value;
            } else {
                value = get(matchingChild, level + 1, keys);
            }

            if (value == null && !matchingChild.matchAny && !matchingChild.greedyMatchAny && wildChild != null) {
                if (level == keys.length - 1) {
                    value = wildChild.value;
                } else {
                    value = get(wildChild, level + 1, keys);
                }
            }

            if (value == null && wildPathChild != null) {
                if (wildPathChild.firstChild == null) {
                    value = wildPathChild.value;
                } else {
                    for (int i = level + 1; i < keys.length; i++) {
                        TrieNode<K, V> currentLookUp = findMatch(wildPathChild.firstChild, keys[i]);
                        if (currentLookUp != null) {
                            if (i == keys.length - 1) {
                                value = currentLookUp.value;
                            } else {
                                value = get(currentLookUp, i, keys);
                            }
                            break;
                        }
                    }

                    if (value == null) {
                        value = wildPathChild.value;
                    }
                }
            }
        }

        // Case if node == null, a null value for buildable will be returned
        return value;
    }

    public TrieNode<K, V> findMatch(TrieNode<K, V> node, K key) {
        if (node.matchAny || node.greedyMatchAny || node.key.equals(key)) {
            return node;
        } else {
            TrieNode<K, V> current = node.rightSibling;
            while (current != null) {
                if (current.matchAny || current.greedyMatchAny || current.key.equals(key)) {
                    return current;
                }
                current = current.rightSibling;
            }
            return null;
        }
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

        traverseAndPrint(node.firstChild, pathParts, separator, paths);
        traverseAndPrint(node.rightSibling, pathStr, separator, paths);
    }
}
