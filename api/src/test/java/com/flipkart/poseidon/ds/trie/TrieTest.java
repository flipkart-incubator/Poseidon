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

import com.flipkart.poseidon.api.APILegoSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.flipkart.poseidon.api.APILegoSet.getKeysForTrie;

/**
 * Created by mohan.pandian on 20/11/15.
 */
public class TrieTest {
    @Test
    public void noPlaceholderTest() {
        Trie<String, String> trie = new Trie<>();
        trie.add(getKeysForTrie("b"), "b");
        trie.add(getKeysForTrie("b/a"), "ba");
        trie.add(getKeysForTrie("a/b"), "ab");
        trie.add(getKeysForTrie("a/c"), "ac");
        trie.add(getKeysForTrie("a/d/a"), "ada");

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
        trie.add(getKeysForTrie("a/{-}"), "a*");
        trie.add(getKeysForTrie("n/{-}"), "n*");
        trie.add(getKeysForTrie("{-}/a/{-}/a"), "prada");
        trie.add(getKeysForTrie("{-}/a/d/a"), "nada");
        trie.add(getKeysForTrie("a/b"), "ab");
        trie.add(getKeysForTrie("**"), "chole");
        trie.add(getKeysForTrie("a/c"), "ac");
        trie.add(getKeysForTrie("a/d/a"), "ada");
        trie.add(getKeysForTrie("b/{-}"), "naan");
        trie.add(getKeysForTrie("b"), "dosa");
        trie.add(getKeysForTrie("c/{-}/a"), "idli");
        trie.add(getKeysForTrie("{-}"), "tikki");

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
        Assert.assertEquals("chole", trie.get(new String[]{"a", "anything", "a"}));
        Assert.assertEquals("chole", trie.get(new String[]{"b", "anything", "a"}));
        Assert.assertEquals("chole", trie.get(new String[]{"c", "anything", "q"}));
        Assert.assertEquals("chole", trie.get(new String[]{"a", "d", "a", "a", "z"}));
        Assert.assertEquals("chole", trie.get(new String[]{"a", "d", "a", null}));
        Assert.assertEquals("chole", trie.get(new String[]{null, "d", "a", null}));
        Assert.assertEquals("tikki", trie.get(new String[]{"v"}));

        System.out.println("Tree for placeholderTest1:");
        printPaths(trie);
        System.out.println();
    }

    @Test
    public void placeholderTest2() {
        Trie<String, String> trie = new Trie<>();
        trie.add(getKeysForTrie("a/{}"), "a*");
        trie.add(getKeysForTrie("a/b"), "ab");
        trie.add(getKeysForTrie("a/{}/e"), "a*e");

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
        trie.add(getKeysForTrie("a/b"), "ab");
        trie.add(getKeysForTrie("a/c"), "ac");
        trie.add(getKeysForTrie("a/{}"), "a*");

        Assert.assertEquals("ac", trie.get(new String[]{"a", "c"}));
        Assert.assertEquals("ab", trie.get(new String[]{"a", "b"}));

        System.out.println("Tree for placeholderTest3:");
        printPaths(trie);
        System.out.println();
    }

    @Test
    public void placeholderTest4() {
        Trie<String, String> trie = new Trie<>();
        trie.add(getKeysForTrie("a/b/d"), "abd");
        trie.add(getKeysForTrie("a/c"), "ac");

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
        trie.add(getKeysForTrie("v1/accounts/{}/campaigns/{}/banners"), "1");
        trie.add(getKeysForTrie("v1/accounts/{}/campaign/{}"), "2");
        trie.add(getKeysForTrie("v1/login"), "3");
        trie.add(getKeysForTrie("v1/accounts/{}"), "4");
        trie.add(getKeysForTrie("v1/accounts/{}/campaigns/all"), "5");
        trie.add(getKeysForTrie("v1/accounts/{}/campaigns/{}"), "6");
        trie.add(getKeysForTrie("v1/accounts/{}/campaigns/{}/banners/{}"), "7");
        trie.add(getKeysForTrie("v1/uploadDocument"), "8");
        trie.add(getKeysForTrie("v1/accounts"), "9");
        trie.add(getKeysForTrie("v1/accounts/{}/session"), "10");
        trie.add(getKeysForTrie("v1/accounts/{}/campaign"), "11");

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

    @Test
    public void greedyWildcardTest() {
        Trie<String, String> trie = new Trie<>();
        trie.add(getKeysForTrie("{}/**/r/**"), "{}*r*");
        trie.add(getKeysForTrie("g/**/r/**"), "g*r*");
        trie.add(getKeysForTrie("g/**/r/**/p"), "g*r*p");
        trie.add(getKeysForTrie("a/**/q"), "a*");
        trie.add(getKeysForTrie("a/{}/q"), "a{}");
        trie.add(getKeysForTrie("a/b"), "ab");
        trie.add(getKeysForTrie("a/**/e"), "a*e");
        trie.add(getKeysForTrie("g/**"), "g*");
        trie.add(getKeysForTrie("g/**/r"), "g*r");

        Assert.assertEquals("ab", trie.get(new String[]{"a", "b"}));
        Assert.assertEquals("a{}", trie.get(new String[]{"a", "c", "q"}));
        Assert.assertEquals("g*", trie.get(new String[]{"g", "c", "q"}));
        Assert.assertEquals("g*r", trie.get(new String[]{"g", "c", "r"}));
        Assert.assertEquals("g*r", trie.get(new String[]{"g", "c", "q", "i", "r"}));
        Assert.assertEquals("g*r*p", trie.get(new String[]{"g", "c", "r", "x", "p"}));
        Assert.assertEquals("g*r*", trie.get(new String[]{"g", "c", "r", "x", "a"}));
        Assert.assertEquals("{}*r*", trie.get(new String[]{"x", "c", "r", "x", "a"}));
        Assert.assertEquals("g*r*", trie.get(new String[]{"g", "c", "r", "x", "a", "q", "z"}));
        Assert.assertEquals("a*", trie.get(new String[]{"a", "c", "d", "q"}));
        Assert.assertEquals("a*e", trie.get(new String[]{"a", "anything1", "e"}));
        Assert.assertEquals("a*e", trie.get(new String[]{"a", "anything2", "e"}));

        System.out.println("Tree for placeholderTest2:");
        printPaths(trie);
        System.out.println();
    }

    @Test
    public void greedyWildcardTest2() {
        Trie<String, String> trie = new Trie<>();
        trie.add(getKeysForTrie("3/**/imei/{-}"), "wildImei");
        trie.add(getKeysForTrie("3/imei/{}"), "imei");

        Assert.assertEquals("imei", trie.get(new String[]{"3", "imei", "358967061697585"}));
        Assert.assertEquals("wildImei", trie.get(new String[]{"3", "uisa", "dadssaf", "asdsa", "imei", "358967061697585"}));

        System.out.println("Tree for placeholderTest2:");
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
