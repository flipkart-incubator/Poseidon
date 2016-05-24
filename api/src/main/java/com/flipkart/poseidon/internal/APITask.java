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

import com.flipkart.hydra.composer.DefaultComposer;
import com.flipkart.hydra.composer.exception.ComposerEvaluationException;
import com.flipkart.hydra.composer.exception.ComposerInstantiationException;
import com.flipkart.hydra.task.DefaultMultiTask;
import com.flipkart.hydra.task.exception.BadCallableException;
import com.flipkart.poseidon.datasources.DataSourceRequest;
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import flipkart.lego.api.entities.DataSource;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import flipkart.lego.api.exceptions.LegoSetException;

import java.util.Map;
import java.util.concurrent.Callable;

public class APITask extends DefaultMultiTask {

    private final PoseidonLegoSet legoSet;
    private final String name;

    public APITask(PoseidonLegoSet legoSet, String name, String loopOverContext, Map<String, Object> context) throws ComposerInstantiationException {
        super(legoSet.getDataSourceExecutor(),
                null,
                new DefaultComposer(context),
                loopOverContext == null ? null : new DefaultComposer(loopOverContext));

        this.legoSet = legoSet;
        this.name = name;
    }

    @Override
    public Callable getCallable(Map<String, Object> values) throws BadCallableException {
        if (loopComposer == null) {
            return getDataSource(values);
        } else {
            try {
                return new DataSourceCallable(legoSet, executor, name, loopComposer, composer, values);
            } catch (NoSuchMethodException | ComposerEvaluationException e) {
                throw new BadCallableException("Unable to execute callable", e);
            }
        }
    }

    private DataSource getDataSource(Map<String, Object> values) throws BadCallableException {
        try {
            Map<String, Object> composedValues = (Map<String, Object>) composer.compose(values);
            DataSourceRequest dataSourceRequest = getDataSourceRequest(composedValues);
            return legoSet.getDataSource(name, dataSourceRequest);
        } catch (ElementNotFoundException | LegoSetException | ComposerEvaluationException e) {
            throw new BadCallableException("Unable to find datasource - " + name, e);
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
