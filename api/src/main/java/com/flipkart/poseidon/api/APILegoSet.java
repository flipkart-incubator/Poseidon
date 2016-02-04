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

import com.flipkart.poseidon.core.PoseidonRequest;
import com.flipkart.poseidon.ds.trie.Trie;
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.utils.ApiHelper;
import flipkart.lego.api.entities.Buildable;
import flipkart.lego.api.entities.Request;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import flipkart.lego.api.exceptions.LegoSetException;
import org.slf4j.Logger;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by mohan.pandian on 05/11/15.
 */
public abstract class APILegoSet extends PoseidonLegoSet {
    private static final Logger logger = getLogger(APILegoSet.class);

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

        logger.info("Registered URLs: ");
        trie.printAllPaths("/");
    }

    @Override
    public Buildable getBuildable(Request request) throws LegoSetException, ElementNotFoundException {
        PoseidonRequest poseidonRequest = (PoseidonRequest) request;
        String completeUrl = ApiHelper.getUrlWithHttpMethod(poseidonRequest.getUrl(), poseidonRequest.getHttpMethod());
        Buildable buildable = trie.get(getKeysForTrie(completeUrl));
        buildable = (buildable == null) ? trie.get(getKeysForTrie(poseidonRequest.getUrl())) : buildable;
        if (buildable == null) {
            throw new ElementNotFoundException("Buildable not found for given url: " + poseidonRequest.getUrl());
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
