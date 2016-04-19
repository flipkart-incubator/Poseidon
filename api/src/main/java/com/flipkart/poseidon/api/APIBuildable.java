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

import com.flipkart.hydra.task.Task;
import com.flipkart.poseidon.constants.RequestConstants;
import com.flipkart.poseidon.core.PoseidonResponse;
import com.flipkart.poseidon.internal.OrchestratorDataSource;
import com.flipkart.poseidon.internal.ParamValidationFilter;
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.mappers.Mapper;
import com.flipkart.poseidon.pojos.EndpointPOJO;
import com.google.common.base.Joiner;
import flipkart.lego.api.entities.*;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import flipkart.lego.api.exceptions.InternalErrorException;
import flipkart.lego.api.exceptions.LegoException;

import java.io.IOException;
import java.util.*;

import static com.flipkart.poseidon.helpers.ObjectMapperHelper.getMapper;
import static com.google.common.net.MediaType.JSON_UTF_8;

public class APIBuildable implements Buildable {

    private static final String RESPONSE_KEY = "response_key";
    private final PoseidonLegoSet legoSet;
    private final EndpointPOJO pojo;
    private final Configuration configuration;
    private final Map<String, Task> tasks;
    private List<Object> mappedBeans;

    public APIBuildable(PoseidonLegoSet legoSet, EndpointPOJO pojo, Configuration configuration, Map<String, Task> tasks) {
        this.legoSet = legoSet;
        this.pojo = pojo;
        this.configuration = configuration;
        this.tasks = tasks;
    }

    @Override
    public long getTimeout() throws LegoException {
        return pojo.getTimeout();
    }

    @Override
    public Map<String, DataSource> getRequiredDataSources(Request request) throws InternalErrorException {
        Map<String, Object> initialParams = request.getAttribute(RequestConstants.PARAMS);
        mappedBeans = new ArrayList<>();
        OrchestratorDataSource dataSource = new OrchestratorDataSource(legoSet, initialParams,
                tasks, pojo.getResponse(), getMappers(), mappedBeans);
        Map<String, DataSource> dataSourceMap = new HashMap<>();
        dataSourceMap.put(RESPONSE_KEY, legoSet.wrapDataSource(dataSource));
        return dataSourceMap;
    }

    @Override
    public Map<String, DataSource> getOptionalDataSources(Request request) throws LegoException {
        return null;
    }

    @Override
    public void build(Request request, Response response, Map<String, Object> model) throws InternalErrorException {
        PoseidonResponse poseidonResponse = (PoseidonResponse) response;
        poseidonResponse.setContentType(JSON_UTF_8);
        poseidonResponse.addMappedBeans(mappedBeans);

        Object dsResponse = model.get(RESPONSE_KEY);
        poseidonResponse.setResponse(dsResponse);
    }

    @Override
    public LinkedHashSet<Filter> getFilters(Request request) throws InternalErrorException {
        LinkedHashSet<Filter> filters = new LinkedHashSet<>();
        filters.add(new ParamValidationFilter(pojo.getParams()));

        try {
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
        } catch (ElementNotFoundException e) {
            throw new InternalErrorException(e);
        }
        return filters;
    }

    private LinkedHashSet<Mapper> getMappers() throws InternalErrorException {
        LinkedHashSet<Mapper> mappers = new LinkedHashSet<>();
        try {
            if (pojo.getMappers() != null) {
                for (String mapperId : pojo.getMappers()) {
                    mappers.add(legoSet.getMapper(mapperId));
                }
            }
        } catch (ElementNotFoundException e) {
            throw new InternalErrorException(e);
        }
        return mappers;
    }

    @Override
    public String getId() throws UnsupportedOperationException {
        return getName() + "_" + Joiner.on(".").join(getVersion());
    }

    @Override
    public String getName() throws UnsupportedOperationException {
        return pojo.getName();
    }

    @Override
    public List<Integer> getVersion() {
        return Arrays.asList(1, 0);
    }
}
