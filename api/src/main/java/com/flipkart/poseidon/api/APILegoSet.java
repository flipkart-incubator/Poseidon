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

package com.flipkart.poseidon.api;

import com.flipkart.poseidon.constants.RequestConstants;
import com.flipkart.poseidon.core.PoseidonRequest;
import com.flipkart.poseidon.ds.trie.Trie;
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.metrics.Metrics;
import com.flipkart.poseidon.utils.ApiHelper;
import flipkart.lego.api.entities.Buildable;
import flipkart.lego.api.entities.Request;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import flipkart.lego.api.exceptions.LegoSetException;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import static com.flipkart.poseidon.constants.RequestConstants.TIMER_CONTEXT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by mohan.pandian on 05/11/15.
 */
public abstract class APILegoSet extends PoseidonLegoSet {
    private static final Logger logger = getLogger(APILegoSet.class);
    private final int MAX_METHOD_LENGTH = HttpMethod.OPTIONS.asString().length();

    private Trie<String, Buildable> trie = new Trie<>();

    @Override
    public void updateBuildables(Map<String, Buildable> buildableMap) {
        for (Map.Entry<String, Buildable> entry : buildableMap.entrySet()) {
            String url = entry.getKey();
            String[] keys = getKeysForTrie(url);
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].startsWith("{") && keys[i].endsWith("}")) {
                    keys[i] = null;
                }
            }
            trie.add(keys, entry.getValue());
        }

        printPaths();
    }

    private void printPaths() {
        logger.info("Registered URLs: ");
        List<List<String>> paths = trie.printAllPaths("/");
        System.out.println("==========================================================================================");
        System.out.println();
        paths.forEach(list -> {
            equalizeAndPrintHttpMethod(list.get(0));
            list.subList(1, list.size()).forEach(System.out::print);
            System.out.println();
        });
        System.out.println();
        System.out.println("==========================================================================================");
        System.out.println();
    }

    private void equalizeAndPrintHttpMethod(String method) {
        int diff = MAX_METHOD_LENGTH - method.length();
        for (int i = 0; i < diff; i++) {
            method = method + " ";
        }
        System.out.print(method + "\t\t\t\t");
    }

    @Override
    public Buildable getBuildable(Request request) throws LegoSetException, ElementNotFoundException {
        PoseidonRequest poseidonRequest = (PoseidonRequest) request;
        String httpMethod = poseidonRequest.getAttribute(RequestConstants.METHOD).toString();
        String completeUrl = ApiHelper.getUrlWithHttpMethod(poseidonRequest.getUrl(), httpMethod);
        Buildable buildable = trie.get(getKeysForTrie(completeUrl));
        if (buildable == null) {
            throw new ElementNotFoundException("Buildable not found for given url: " + poseidonRequest.getUrl());
        }
        String name = buildable.getName();
        if (name != null && !name.isEmpty()) {
            poseidonRequest.setAttribute(TIMER_CONTEXT, Metrics.getRegistry().timer("poseidon.api." + name + "." + httpMethod).time());
        }
        return buildable;
    }

    private String[] getKeysForTrie(String url) {
        if (url.startsWith("/")) {
            url = url.replaceFirst("\\/", "");
        }
        return url.split("/");
    }
}
