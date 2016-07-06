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

import java.util.List;

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
        printPaths(trie);
        System.out.println();
    }

    @Test
    public void placeholderTest1() {
        Trie<String, String> trie = new Trie<>();
        trie.add(new String[]{"a", null}, "a*");
        trie.add(new String[]{"n", null}, "n*");
        trie.add(new String[]{null, "a", null, "a"}, "prada");
        trie.add(new String[]{null, "a", "d", "a"}, "nada");
        trie.add(new String[]{"a", "b"}, "ab");
        trie.add(new String[]{"a", "c"}, "ac");
        trie.add(new String[]{"a", "d", "a"}, "ada");
        trie.add(new String[]{"b", null}, "naan");
        trie.add(new String[]{"b"}, "dosa");
        trie.add(new String[]{"c", null, "a"}, "idli");
        trie.add(new String[]{null}, "tikki");

        Assert.assertEquals("tikki", trie.get(new String[]{"q"}));
        Assert.assertEquals("dosa", trie.get(new String[]{"b"}));
        Assert.assertEquals("idli", trie.get(new String[]{"c", "q", "a"}));
        Assert.assertEquals("naan", trie.get(new String[]{"b", null}));
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
        Assert.assertEquals("tikki", trie.get(new String[]{"a"}));
        Assert.assertNull(trie.get(new String[]{"a", "anything", "a"}));
        Assert.assertNull(trie.get(new String[]{"b", "anything", "a"}));
        Assert.assertNull(trie.get(new String[]{"c", "anything", "q"}));
        Assert.assertNull(trie.get(new String[]{"a", "d", "a", "a"}));
        Assert.assertNull(trie.get(new String[]{"a", "d", "a", null}));
        Assert.assertNull(trie.get(new String[]{null, "d", "a", null}));

        System.out.println("Tree for placeholderTest1:");
        printPaths(trie);
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
        printPaths(trie);
        System.out.println();
    }

    @Test
    public void placeholderTest3() {
        Trie<String, String> trie = new Trie<>();
        trie.add(new String[]{"a", "b"}, "ab");
        trie.add(new String[]{"a", "c"}, "ac");
        trie.add(new String[]{"a", null}, "a*");

        Assert.assertEquals("ac", trie.get(new String[]{"a", "c"}));
        Assert.assertEquals("ab", trie.get(new String[]{"a", "b"}));

        System.out.println("Tree for placeholderTest3:");
        printPaths(trie);
        System.out.println();
    }

    @Test
    public void placeholderTest4() {
        Trie<String, String> trie = new Trie<>();
        trie.add(new String[]{"a", "b", "d"}, "abd");
        trie.add(new String[]{"a", "c"}, "ac");

        Assert.assertEquals("ac", trie.get(new String[]{"a", "c"}));
        Assert.assertEquals(null, trie.get(new String[]{"a", "b"}));
        Assert.assertEquals(null, trie.get(new String[]{"a", "b", "c"}));

        System.out.println("Tree for placeholderTest4:");
        printPaths(trie);
        System.out.println();
    }

    @Test
    public void noNullPointerTest() {
        Trie<String, String> trie = new Trie<>();
        trie.add(new String[]{"v1", "accounts", null, "campaigns", null, "banners"}, "1");
        trie.add(new String[]{"v1", "accounts", null, "campaign", null}, "2");
        trie.add(new String[]{"v1", "login"}, "3");
        trie.add(new String[]{"v1", "accounts", null}, "4");
        trie.add(new String[]{"v1", "accounts", null, "campaigns", "all"}, "5");
        trie.add(new String[]{"v1", "accounts", null, "campaigns", null}, "6");
        trie.add(new String[]{"v1", "accounts", null, "campaigns", null, "banners", null}, "7");
        trie.add(new String[]{"v1", "uploadDocument"}, "8");
        trie.add(new String[]{"v1", "accounts"}, "9");
        trie.add(new String[]{"v1", "accounts", null, "session"}, "10");
        trie.add(new String[]{"v1", "accounts", null, "campaign"}, "11");

        Assert.assertEquals("1", trie.get(new String[]{"v1", "accounts", null, "campaigns", null, "banners"}));
        Assert.assertEquals("2", trie.get(new String[]{"v1", "accounts", null, "campaign", null}));
        Assert.assertEquals("3", trie.get(new String[]{"v1", "login"}));
        Assert.assertEquals("4", trie.get(new String[]{"v1", "accounts", null}));
        Assert.assertEquals("5", trie.get(new String[]{"v1", "accounts", null, "campaigns", "all"}));
        Assert.assertEquals("6", trie.get(new String[]{"v1", "accounts", null, "campaigns", null}));
        Assert.assertEquals("7", trie.get(new String[]{"v1", "accounts", null, "campaigns", null, "banners", null}));
        Assert.assertEquals("8", trie.get(new String[]{"v1", "uploadDocument"}));
        Assert.assertEquals("9", trie.get(new String[]{"v1", "accounts"}));
        Assert.assertEquals("10", trie.get(new String[]{"v1", "accounts", null, "session"}));
        Assert.assertEquals("11", trie.get(new String[]{"v1", "accounts", null, "campaign"}));

        System.out.println("Tree for placeholderTest1:");
        printPaths(trie);
        System.out.println();
    }

    private <K, V> void printPaths(Trie<K, V> trie) {
        trie.printAllPaths("/").forEach(list -> {
            list.forEach(System.out::print);
            System.out.println();
        });
    }
}
