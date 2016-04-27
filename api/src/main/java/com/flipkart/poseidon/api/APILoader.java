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

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.hydra.composer.exception.ComposerInstantiationException;
import com.flipkart.hydra.task.Task;
import com.flipkart.poseidon.internal.APITask;
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.pojos.EndpointPOJO;
import com.flipkart.poseidon.pojos.TaskPOJO;
import com.flipkart.poseidon.utils.ApiHelper;
import flipkart.lego.api.entities.Buildable;
import org.slf4j.Logger;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.flipkart.poseidon.helpers.ObjectMapperHelper.getMapper;
import static org.slf4j.LoggerFactory.getLogger;

public class APILoader {

    private static final Logger logger = getLogger(APILoader.class);
    private final PoseidonLegoSet legoSet;
    private List<EndpointPOJO> pojos = new ArrayList<>();
    private final Configuration configuration;

    public APILoader(PoseidonLegoSet legoSet, String config, Configuration configuration) {
        this.legoSet = legoSet;
        this.configuration = configuration;
        try {
            loadBuildables(config);
        } catch (Exception exception) {
            logger.error("Buildables loading failed: {}", exception);
        }
    }

    public Map<String, Buildable> getBuildableMap() {
        Map<String, Buildable> buildables = new HashMap<>();
        for (EndpointPOJO pojo : pojos) {
            try {
                // check if already a buildable  exists for a given api/uri
                if (buildables.containsKey(pojo.getUrl())) {
                    logger.error("******* More than one Buildable defined for api: \"" + pojo.getUrl()+"\", all except first occurrences will be ignored. *******");
                } else {
                    APIBuildable apiBuildable = new APIBuildable(legoSet, pojo, configuration, getCalls(pojo.getTasks()));

                    String completeUrl;
                    if (pojo.getHttpMethod() != null) {
                        completeUrl = ApiHelper.getUrlWithHttpMethod(pojo.getUrl(), pojo.getHttpMethod().toString());
                    } else {
                        // to remove extra slashes in url
                        completeUrl = ApiHelper.getUrlWithHttpMethod(pojo.getUrl(), HttpMethod.GET.toString());
                    }
                    buildables.put(completeUrl, apiBuildable);
                }
            } catch (Throwable error) {
                logger.error("Buildable loading failed for atleast one api: " + pojo.getUrl(), error);
            }
        }

        return buildables;
    }

    private void loadBuildables(String config) throws Exception {
        JsonNode nodes = getMapper().readTree(config);

        if (!nodes.isArray()) {
            throw new Exception("Config is not an array of buildables");
        }

        for (JsonNode node : nodes) {
            try {
                pojos.add(getMapper().readValue(node.toString(), EndpointPOJO.class));
            } catch (Exception exception) {
                logger.error("Error in de-serializing a config: " + node.toString(), exception);
            }
        }
    }

    private Map<String, Task> getCalls(Map<String, TaskPOJO> sources) throws ComposerInstantiationException {
        Map<String, Task> tasks = new HashMap<>();
        for (String key : sources.keySet()) {
            TaskPOJO dataSourcePOJO = sources.get(key);
            String name = dataSourcePOJO.getName();

            Map<String, Object> context = dataSourcePOJO.getContext();
            String loopOverContext = dataSourcePOJO.getLoopOver();
            tasks.put(key, new APITask(legoSet, name, loopOverContext, context));
        }

        return tasks;
    }
}
