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
import com.flipkart.poseidon.core.RequestContext;
import com.flipkart.poseidon.ds.trie.KeyWrapper;
import com.flipkart.poseidon.ds.trie.Trie;
import com.flipkart.poseidon.helpers.MetricsHelper;
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.metrics.Metrics;
import com.flipkart.poseidon.pojos.EndpointPOJO;
import com.flipkart.poseidon.serviceclients.ServiceContext;
import com.flipkart.poseidon.utils.ApiHelper;
import flipkart.lego.api.entities.Buildable;
import flipkart.lego.api.entities.Request;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import flipkart.lego.api.exceptions.LegoSetException;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.flipkart.poseidon.Poseidon.STARTUP_LOGGER;
import static com.flipkart.poseidon.constants.RequestConstants.*;
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
            trie.add(getKeysForTrie(url), entry.getValue());
        }
        printPaths();
    }

    private void printPaths() {
        List<List<String>> paths = trie.printAllPaths("/");
        StringBuilder builder = new StringBuilder();
        builder.append("Registered URLs: \n");
        builder.append("==========================================================================================\n");
        builder.append("\n");
        paths.forEach(list -> {
            equalizeAndPrintHttpMethod(list.get(0), builder);
            list.subList(1, list.size()).forEach(builder::append);
            builder.append("\n");
        });
        builder.append("\n");
        builder.append("==========================================================================================\n");
        STARTUP_LOGGER.info(builder.toString());
    }

    private void equalizeAndPrintHttpMethod(String method, StringBuilder builder) {
        int diff = MAX_METHOD_LENGTH - method.length();
        for (int i = 0; i < diff; i++) {
            method = method + " ";
        }
        builder.append(method + "\t\t\t\t");
    }

    @Override
    public Buildable getBuildable(Request request) throws LegoSetException, ElementNotFoundException {
        PoseidonRequest poseidonRequest = (PoseidonRequest) request;
        String httpMethod = poseidonRequest.getAttribute(RequestConstants.METHOD).toString();
        String completeUrl = ApiHelper.getUrlWithHttpMethod(poseidonRequest.getUrl(), httpMethod);
        Buildable buildable = trie.get(getKeysArrayForTrie(completeUrl));
        if (buildable == null) {
            throw new ElementNotFoundException("Buildable not found for given url: " + poseidonRequest.getUrl());
        }

        if (buildable instanceof APIBuildable) {
            EndpointPOJO pojo = ((APIBuildable) buildable).getPojo();
            RequestContext.set(URI, pojo.getUrl());
            RequestContext.set(ENDPOINT_NAME, pojo.getName());
            RequestContext.set(API_ANNOTATIONS, pojo.getProperties());
            RequestContext.set(ENDPOINT_METHOD, pojo.getHttpMethod());
            ServiceContext.set(ENDPOINT_NAME, pojo.getName());

            String name = pojo.getName();
            if (name != null && !name.isEmpty()) {
                poseidonRequest.setAttribute(TIMER_CONTEXT, Metrics.getRegistry().timer(MetricsHelper.getApiTimerMetricsName(name, httpMethod)).time());
            }
        }

        return buildable;
    }

    public static List<KeyWrapper<String>> getKeysForTrie(String url) {
        List<KeyWrapper<String>> wrappers = Arrays.stream(getKeysArrayForTrie(url)).map(KeyWrapper::new).collect(Collectors.toList());

        for (KeyWrapper<String> keyWrapper : wrappers) {
            if (keyWrapper.key.startsWith("{") && keyWrapper.key.endsWith("}")) {
                keyWrapper.key = null;
                keyWrapper.wildCard = true;
            } else if (keyWrapper.key.startsWith("*") && keyWrapper.key.endsWith("*")) {
                keyWrapper.key = null;
                keyWrapper.greedyWildCard = true;
            }
        }

        return wrappers;
    }

    private static String[] getKeysArrayForTrie(String url) {
        if (url.startsWith("/")) {
            url = url.replaceFirst("\\/", "");
        }
        return url.split("/");
    }
}
