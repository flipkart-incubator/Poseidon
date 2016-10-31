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
import flipkart.lego.api.entities.DataSource;
import flipkart.lego.api.entities.DataType;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;

import java.util.Map;
import java.util.concurrent.Future;

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

    protected Future<DataType> execute(AbstractDataSource dataSource) throws Exception {
        return this.legoset.getDataSourceExecutor().submit(this.legoset.wrapDataSource(dataSource, dataSource.getRequest()));
    }

    protected Future<DataType> execute(String dsId, Request request) throws Exception {
        DataSource dataSource = this.legoset.getDataSource(dsId, request);
        return this.legoset.getDataSourceExecutor().submit(dataSource);
    }

    @Override
    public abstract T call() throws Exception;

    protected Request getRequest() {
        return request;
    }
}
