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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mohan.pandian on 20/11/15.
 */
public class TrieNode<K,V> {
    K key;
    V value;
    boolean matchAny;
    boolean greedyMatchAny;

    TrieNode<K,V> wildChild;
    TrieNode<K,V> greedyWildChild;

    final Map<K, TrieNode<K, V>> children = new HashMap<>();

    @Override
    public String toString() {
        return "TrieNode{" +
                "key=" + key +
                ", value=" + value +
                ", matchAny=" + matchAny +
                ", greedyMatchAny=" + greedyMatchAny +
                ", wildChild=" + wildChild +
                ", greedyWildChild=" + greedyWildChild +
                ", children=" + children +
                '}';
    }
}
