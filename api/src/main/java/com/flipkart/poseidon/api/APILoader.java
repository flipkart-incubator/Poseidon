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
import com.flipkart.poseidon.internal.ParamValidationFilter;
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.mappers.Mapper;
import com.flipkart.poseidon.pojos.EndpointPOJO;
import com.flipkart.poseidon.pojos.TaskPOJO;
import flipkart.lego.api.entities.Buildable;
import flipkart.lego.api.entities.Filter;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import org.slf4j.Logger;

import java.util.*;

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
                // check if already a api-builable map exists for a given api/uri
                if ( buildables.containsKey( pojo.getUrl()) ){
                    logger.error("******* More than one Buildable defined for api: \"" + pojo.getUrl()+"\", all except first occurrences will be ignored. *******");
                } else {
                    APIBuildable apiBuildable = new APIBuildable(legoSet, pojo.getUrl(), pojo.getTimeout(), getFilters(pojo), getMappers(pojo), getCalls(pojo.getTasks()), pojo.getResponse());
                    buildables.putAll(apiBuildable.getRoutes());
                }
            } catch (Throwable error) {
                logger.error("Buildable loading failed for atleast one api: " + pojo.getUrl(), error);
            }
        }

        return buildables;
    }

    private LinkedHashSet<Filter> getFilters(EndpointPOJO pojo) throws ElementNotFoundException {
        LinkedHashSet<Filter> filters = new LinkedHashSet<>();
        filters.add(new ParamValidationFilter(pojo.getParams()));

        // Add global filters
        if (configuration.getFilterIds() != null) {
            for (String filterId : configuration.getFilterIds()) {
                filters.add(legoSet.getFilter(filterId));
            }
        }

        // Add endpoint specific filters
        if (pojo.getFilters() != null) {
            for (String filterId : pojo.getFilters()) {
                filters.add(legoSet.getFilter(filterId));
            }
        }
        return filters;
    }

    private Set<Mapper> getMappers(EndpointPOJO pojo) throws ElementNotFoundException {
        Set<Mapper> mappers = new LinkedHashSet<>();
        if (pojo.getMappers() != null) {
            for (String mapperId : pojo.getMappers()) {
                mappers.add(legoSet.getMapper(mapperId));
            }
        }
        return mappers;
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
