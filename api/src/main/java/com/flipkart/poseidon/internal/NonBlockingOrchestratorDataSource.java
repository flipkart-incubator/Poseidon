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

package com.flipkart.poseidon.internal;

import com.flipkart.hydra.task.Task;
import com.flipkart.poseidon.datasources.AbstractNonBlockingDataSource;
import com.flipkart.poseidon.mappers.Mapper;
import com.flipkart.poseidon.model.annotations.Trace;
import flipkart.lego.api.entities.DataType;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.concurrency.api.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Trace(false)
public class NonBlockingOrchestratorDataSource extends AbstractNonBlockingDataSource<Object, DataType>  {
    private final Map<String, Object> initialParams;
    private final Map<String, Task> tasks;
    private final Object responseContext;
    private final Set<Mapper> mappers;
    private List<Object> mappedBeans;

    private APIComposer composer;
    private NonBlockingDispatcher dispatcher;

    private static final Logger logger = LoggerFactory.getLogger(NonBlockingOrchestratorDataSource.class);

    public NonBlockingOrchestratorDataSource(LegoSet legoSet, Map<String, Object> initialParams, Map<String, Task> tasks, Object responseContext, Set<Mapper> mappers, List<Object> mappedBeans) {
        super(legoSet, null);
        this.initialParams = initialParams;
        this.tasks = tasks;
        this.responseContext = responseContext;
        this.mappers = mappers;
        this.mappedBeans = mappedBeans;
        this.dispatcher = new NonBlockingDispatcher();
    }

    @Override
    public Promise<Object> callAsync() throws Exception {
        logger.info("Thread executing callAsync - {}", Thread.currentThread().getName());

        composer = new APIComposer(responseContext, initialParams, mappers, mappedBeans);
        return dispatcher.execute(initialParams, tasks, composer);
    }

    @Override
    public DataType map(Object response) throws Exception {
        logger.info("Thread executing map - {}", Thread.currentThread().getName());
        response = dispatcher.map(composer);
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
