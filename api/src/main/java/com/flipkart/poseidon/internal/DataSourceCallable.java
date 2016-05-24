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

import com.flipkart.hydra.composer.Composer;
import com.flipkart.hydra.composer.exception.ComposerEvaluationException;
import com.flipkart.hydra.task.entities.WrapperCallable;
import com.flipkart.hydra.task.exception.BadCallableException;
import com.flipkart.poseidon.datasources.DataSourceRequest;
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.google.common.util.concurrent.ListenableFuture;
import flipkart.lego.api.entities.DataSource;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import flipkart.lego.api.exceptions.LegoSetException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;

public class DataSourceCallable extends WrapperCallable {

    private final PoseidonLegoSet legoSet;
    private final String dataSourceName;

    public DataSourceCallable(PoseidonLegoSet legoSet, ExecutorService executor, String dataSourceName, Composer loopComposer, Composer composer, Map<String, Object> values) throws NoSuchMethodException, ComposerEvaluationException {
        super(listeningDecorator(executor), null, loopComposer.compose(values), composer, values);
        this.legoSet = legoSet;
        this.dataSourceName = dataSourceName;
    }

    @Override
    protected ListenableFuture<Object> getFuture(Object key, Object value) throws Exception {
        Map<String, Object> request = new HashMap<>(values);
        request.put("__key", key);
        request.put("__value", value);
        return executorService.submit((Callable) getDataSource(request));
    }

    private DataSource getDataSource(Map<String, Object> values) throws BadCallableException {
        try {
            Map<String, Object> composedValues = (Map<String, Object>) composer.compose(values);
            DataSourceRequest dataSourceRequest = getDataSourceRequest(composedValues);
            return legoSet.getDataSource(dataSourceName, dataSourceRequest);
        } catch (ElementNotFoundException | LegoSetException | ComposerEvaluationException e) {
            throw new BadCallableException("Unable to find datasource - " + dataSourceName, e);
        }
    }

    private DataSourceRequest getDataSourceRequest(Map<String, Object> values) {
        DataSourceRequest dataSourceRequest = new DataSourceRequest();
        if (values != null) {
            for (String key : values.keySet()) {
                dataSourceRequest.setAttribute(key, values.get(key));
            }
        }

        return dataSourceRequest;
    }
}
