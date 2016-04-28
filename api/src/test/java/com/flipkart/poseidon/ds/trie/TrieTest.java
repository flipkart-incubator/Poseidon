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

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by mohan.pandian on 20/11/15.
 */
public class TrieTest {
    @Test
    public void noPlaceholderTest() {
        Trie<String, String> trie = new Trie<>();
        trie.add(new String[]{"b"}, "b");
        trie.add(new String[]{"b", "a"}, "ba");
        trie.add(new String[]{"a", "b"}, "ab");
        trie.add(new String[]{"a", "c"}, "ac");
        trie.add(new String[]{"a", "d", "a"}, "ada");

        Assert.assertEquals("b", trie.get(new String[]{"b"}));
        Assert.assertEquals("ba", trie.get(new String[]{"b", "a"}));
        Assert.assertEquals("ab", trie.get(new String[]{"a", "b"}));
        Assert.assertEquals("ac", trie.get(new String[]{"a", "c"}));
        Assert.assertEquals("ada", trie.get(new String[]{"a", "d", "a"}));

        System.out.println("Tree for noPlaceholderTest:");
        trie.printAllPaths("/");
        System.out.println();
    }

    @Test
    public void placeholderTest1() {
        Trie<String, String> trie = new Trie<>();
        trie.add(new String[]{"a", null}, "a*");
        trie.add(new String[]{null, "a", null, "a"}, "prada");
        trie.add(new String[]{null, "a", "d", "a"}, "nada");
        trie.add(new String[]{"a", "b"}, "ab");
        trie.add(new String[]{"a", "c"}, "ac");
        trie.add(new String[]{"a", "d", "a"}, "ada");

        Assert.assertEquals("ab", trie.get(new String[]{"a", "b"}));
        Assert.assertEquals("ac", trie.get(new String[]{"a", "c"}));
        Assert.assertEquals("ada", trie.get(new String[]{"a", "d", "a"}));
        Assert.assertEquals("nada", trie.get(new String[]{"n", "a", "d", "a"}));
        Assert.assertEquals("nada", trie.get(new String[]{"s", "a", "d", "a"}));
        Assert.assertEquals("nada", trie.get(new String[]{null, "a", "d", "a"}));
        Assert.assertEquals("prada", trie.get(new String[]{"j", "a", "p", "a"}));
        Assert.assertEquals("prada", trie.get(new String[]{"j", "a", null, "a"}));
        Assert.assertEquals("prada", trie.get(new String[]{null, "a", null, "a"}));
        Assert.assertEquals("a*", trie.get(new String[]{"a", "anything1"}));
        Assert.assertEquals("a*", trie.get(new String[]{"a", "anything2"}));
        Assert.assertNull(trie.get(new String[]{"a"}));
        Assert.assertNull(trie.get(new String[]{"a", "anything", "a"}));
        Assert.assertNull(trie.get(new String[]{"b", "anything", "a"}));
        Assert.assertNull(trie.get(new String[]{"a", "d", "a", "a"}));
        Assert.assertNull(trie.get(new String[]{"a", "d", "a", null}));
        Assert.assertNull(trie.get(new String[]{null, "d", "a", null}));

        System.out.println("Tree for placeholderTest1:");
        trie.printAllPaths("/");
        System.out.println();
    }

    @Test
    public void placeholderTest2() {
        Trie<String, String> trie = new Trie<>();
        trie.add(new String[]{"a", null}, "a*");
        trie.add(new String[]{"a", "b"}, "ab");
        trie.add(new String[]{"a", null, "e"}, "a*e");

        Assert.assertEquals("ab", trie.get(new String[]{"a", "b"}));
        Assert.assertEquals("a*", trie.get(new String[]{"a", "c"}));
        Assert.assertEquals("a*e", trie.get(new String[]{"a", "anything1", "e"}));
        Assert.assertEquals("a*e", trie.get(new String[]{"a", "anything2", "e"}));

        System.out.println("Tree for placeholderTest2:");
        trie.printAllPaths("/");
        System.out.println();
    }
}
