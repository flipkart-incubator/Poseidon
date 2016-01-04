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
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.mappers.Mapper;
import com.google.common.base.Joiner;
import flipkart.lego.api.entities.*;
import flipkart.lego.api.exceptions.InternalErrorException;
import flipkart.lego.api.exceptions.LegoException;

import java.io.IOException;
import java.util.*;

import static com.flipkart.poseidon.helpers.ObjectMapperHelper.getMapper;
import static com.google.common.net.MediaType.JSON_UTF_8;

public class APIBuildable implements Buildable {

    private static final String RESPONSE_KEY = "response_key";
    private final PoseidonLegoSet legoSet;
    private final String url;
    private final long timeout;
    private final LinkedHashSet<Filter> filters;
    private final Set<Mapper> mappers;
    private final Map<String, Task> tasks;
    private final Object response;
    private List<Object> mappedBeans;

        public APIBuildable(PoseidonLegoSet legoSet, String url, long timeout, LinkedHashSet<Filter> filters, Set<Mapper> mappers, Map<String, Task> tasks, Object response) {
        this.legoSet = legoSet;
        this.url = url;
        this.timeout = timeout;
        this.filters = filters;
        this.mappers = mappers;
        this.tasks = tasks;
        this.response = response;
    }

    public String getUrl() {
        return this.url;
    }

    public Map<String, Buildable> getRoutes() {
        Map<String, Buildable> routes = new HashMap<>();
        routes.put(this.url, this);
        return routes;
    }

    @Override
    public long getTimeout() throws LegoException {
        return timeout;
    }

    @Override
    public Map<String, DataSource> getRequiredDataSources(Request request) throws InternalErrorException {
        Map<String, Object> initialParams = (Map<String, Object>) request.getAttribute(RequestConstants.PARAMS);
        mappedBeans = new ArrayList<>();
        OrchestratorDataSource dataSource = new OrchestratorDataSource(legoSet, initialParams, tasks, response, mappers, mappedBeans);
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

        try {
            Object dsResponse = model.get(RESPONSE_KEY);
            String responseStr = dsResponse == null ? "" : getMapper().writeValueAsString(dsResponse);
            poseidonResponse.setResponse(responseStr);
        } catch (IOException e) {
            throw new InternalErrorException(e);
        }
    }

    @Override
    public LinkedHashSet<Filter> getFilters(Request request) throws InternalErrorException {
        return filters;
    }

    @Override
    public String getId() throws UnsupportedOperationException {
        return getName() + "_" + Joiner.on(".").join(getVersion());
    }

    @Override
    public String getName() throws UnsupportedOperationException {
        return "APIBuildable";
    }

    @Override
    public List<Integer> getVersion() {
        return Arrays.asList(1, 0);
    }
}
