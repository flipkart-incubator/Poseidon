/*
 * Copyright 2016 Flipkart Internet, pvt ltd.
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

import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.utils.ApiHelper;
import flipkart.lego.api.entities.Buildable;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Created by shrey.garg on 24/07/16.
 */
public class APILoaderTest {

    private static PoseidonLegoSet legoSet;
    private static String configs;
    private static Configuration configuration;

    @BeforeClass
    public static void setUp() throws Exception {
        legoSet = mock(PoseidonLegoSet.class);
        configuration = mock(Configuration.class);
        File testApiFile = new File("src/test/resources/api/test.json");
        File testApiSlashFile = new File("src/test/resources/api/testSlash.json");
        String config1 = FileUtils.readFileToString(testApiFile);
        String config2 = FileUtils.readFileToString(testApiSlashFile);

        configs = String.format("[ %s, %s ]", config1, config2);
    }

    @Test
    public void testGetBuildableMap() throws Exception {
        APILoader loader = new APILoader(legoSet, configs, configuration);
        Map<String, Buildable> buildableMap = loader.getBuildableMap();
        assertEquals(2, buildableMap.size());
    }

    @Test
    public void testGetBuildableMapCompeteUrl() throws Exception {
        APILoader loader = new APILoader(legoSet, configs, configuration);
        Map<String, Buildable> buildableMap = loader.getBuildableMap();

        String url = "/home/test";
        String method = "POST";
        String completeUrl = ApiHelper.getUrlWithHttpMethod(url, method.toString());

        assertEquals(2, buildableMap.size());
        assertNotNull(buildableMap.get(completeUrl));
        assertTrue(buildableMap.get(completeUrl) instanceof APIBuildable);
        APIBuildable apiBuildable = (APIBuildable) buildableMap.get(completeUrl);
        assertEquals(url, apiBuildable.getPojo().getUrl());
    }

    @Test
    public void testGetBuildableMapCompeteUrlWithMissingSlash() throws Exception {
        APILoader loader = new APILoader(legoSet, configs, configuration);
        Map<String, Buildable> buildableMap = loader.getBuildableMap();

        String url = "/shop/test";
        String method = "POST";
        String completeUrl = ApiHelper.getUrlWithHttpMethod(url, method.toString());

        assertEquals(2, buildableMap.size());
        assertNotNull(buildableMap.get(completeUrl));
        assertTrue(buildableMap.get(completeUrl) instanceof APIBuildable);
        APIBuildable apiBuildable = (APIBuildable) buildableMap.get(completeUrl);
        assertEquals(url, apiBuildable.getPojo().getUrl());
    }
}