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

package com.flipkart.poseidon.internal;

import co.paralleluniverse.fibers.Suspendable;
import com.flipkart.hydra.dispatcher.Dispatcher;
import com.flipkart.hydra.task.Task;
import com.flipkart.poseidon.datasources.AbstractDataSource;
import com.flipkart.poseidon.mappers.Mapper;
import com.flipkart.poseidon.model.annotations.Trace;
import flipkart.lego.api.entities.DataType;
import flipkart.lego.api.entities.LegoSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Trace(false)
public class OrchestratorDataSource extends AbstractDataSource {

    private final Map<String, Object> initialParams;
    private final Map<String, Task> tasks;
    private final Object responseContext;
    private final Set<Mapper> mappers;
    private List<Object> mappedBeans;

    public OrchestratorDataSource(LegoSet legoSet, Map<String, Object> initialParams, Map<String, Task> tasks, Object responseContext, Set<Mapper> mappers, List<Object> mappedBeans) {
        super(legoSet, null);
        this.initialParams = initialParams;
        this.tasks = tasks;
        this.responseContext = responseContext;
        this.mappers = mappers;
        this.mappedBeans = mappedBeans;
    }

    @Override
    @Suspendable
    public DataType call() throws Exception {
        APIComposer composer = new APIComposer(responseContext, initialParams, mappers, mappedBeans);
        Dispatcher dispatcher = new FiberDispatcher();
        Object response;
        try {
            response = dispatcher.execute(initialParams, tasks, composer);
        } finally {
            dispatcher.shutdown();
        }

        if (response == null) {
            return null;
        }
        if (response instanceof DataType) {
            return (DataType) response;
        }
        if (response instanceof Map) {
            return new MapDataType<>((Map<String, Object>) response);
        }
        if (response instanceof List) {
            return new ListDataType<>((List) response);
        }
        if (response instanceof byte[]) {
        	return new ByteArrayDataType((byte[]) response);
        }
        throw new Exception("Unsupported response type");
    }
}
