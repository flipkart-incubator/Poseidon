package com.flipkart.poseidon.ds.trie;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shrey.garg on 18/07/16.
 */
public class KeyWrapper<K> {
    public K key;
    public boolean wildCard = false;
    public boolean wildPath = false;

    public KeyWrapper(K key) {
        this.key = key;
    }
}
