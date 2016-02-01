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

package com.flipkart.poseidon.datasources;

import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import flipkart.lego.api.entities.DataSource;
import flipkart.lego.api.entities.DataType;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static com.flipkart.poseidon.datasources.util.CallableNameHelper.canonicalName;

public abstract class AbstractDataSource<T extends DataType> implements DataSource {

    protected final PoseidonLegoSet legoset;
    protected final Request request;

    public AbstractDataSource(LegoSet legoset, Request request) {
        this.legoset = (PoseidonLegoSet) legoset;
        this.request = request;
    }

    protected Future<DataType> execute(String dsId, Map<String, Object> requestMap) throws Exception {
        DataSourceRequest dataSourceRequest = new DataSourceRequest();
        dataSourceRequest.setAttributes(requestMap);
        return execute(dsId, dataSourceRequest);
    }

    protected Future<DataType> execute(String dsId, Request request) throws Exception {
        if (getId().equals(dsId)) {
            throw new IllegalArgumentException("Recursive calling of datasource is not allowed");
        }

        DataSource dataSource = this.legoset.getDataSource(dsId, request);
        return this.legoset.getDataSourceExecutor().submit(dataSource);
    }

    @Override
    public abstract T call() throws Exception;

    @Override
    public String getId() throws UnsupportedOperationException {
        return getName() + "_" + Joiner.on(".").join(getVersion());
    }

    @Override
    public String getName() throws UnsupportedOperationException {
        return canonicalName(getClass().getSimpleName(), "DataSource", "DS");
    }

    @Override
    public List<Integer> getVersion() throws UnsupportedOperationException {
        return Lists.newArrayList(1, 0, 0);
    }

    @Override
    public String getShortDescription() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getDescription() {
        return this.getClass().getName();
    }
}
