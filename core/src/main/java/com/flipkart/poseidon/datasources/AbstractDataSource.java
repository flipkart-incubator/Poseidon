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

import com.flipkart.poseidon.helper.AnnotationHelper;
import com.flipkart.poseidon.helper.CallableNameHelper;
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import flipkart.lego.api.entities.DataSource;
import flipkart.lego.api.entities.DataType;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;

import java.util.Map;
import java.util.concurrent.Future;

public abstract class AbstractDataSource<T extends DataType> implements DataSource<T> {

    protected final PoseidonLegoSet legoset;
    protected final Request request;

    public AbstractDataSource(LegoSet legoset, Request request) {
        this.legoset = (PoseidonLegoSet) legoset;
        this.request = request;
    }

    public <Q extends DataType> Future<Q> execute(String dsId, Map<String, Object> requestMap) throws Exception {
        DataSourceRequest dataSourceRequest = new DataSourceRequest();
        dataSourceRequest.setAttributes(requestMap);
        return execute(dsId, dataSourceRequest);
    }

    public <Q extends DataType> Future<Q> execute(Class<? extends AbstractDataSource<Q>> clazz, Map<String, Object> requestMap) throws Exception {
        return execute(getDataSourceId(clazz), requestMap);
    }

    public <Q extends DataType> Future<Q> execute(AbstractDataSource<Q> dataSource) throws Exception {
        return this.legoset.getDataSourceExecutor().submit(this.legoset.wrapDataSource(dataSource, dataSource.getRequest()));
    }

    public <Q extends DataType> Future<Q> execute(String dsId, Request request) throws Exception {
        DataSource<Q> dataSource = this.legoset.getDataSource(dsId, request);
        return this.legoset.getDataSourceExecutor().submit(dataSource);
    }

    public <Q extends DataType> Future<Q> execute(Class<? extends AbstractDataSource<Q>> clazz, Request request) throws Exception {
        return this.execute(getDataSourceId(clazz), request);
    }

    public <Q extends DataType> Q executeSync(AbstractDataSource<Q> dataSource) throws Exception {
        return dataSource.call();
    }

    public <Q extends DataType> Q executeSync(String dsId, Map<String, Object> requestMap) throws Exception {
        DataSourceRequest dataSourceRequest = new DataSourceRequest();
        dataSourceRequest.setAttributes(requestMap);
        return executeSync(dsId, dataSourceRequest);
    }

    public <Q extends DataType> Q executeSync(Class<? extends AbstractDataSource<Q>> clazz, Map<String, Object> requestMap) throws Exception {
        return executeSync(getDataSourceId(clazz), requestMap);
    }

    public <Q extends DataType> Q executeSync(String dsId, Request request) throws Exception {
        DataSource<Q> dataSource = this.legoset.getBasicDataSource(dsId, request);
        return dataSource.call();
    }

    public <Q extends DataType> Q executeSync(Class<? extends AbstractDataSource<Q>> clazz, Request request) throws Exception {
        return executeSync(getDataSourceId(clazz), request);
    }

    private <Q extends AbstractDataSource> String getDataSourceId(Class<Q> clazz) {
        String name = clazz.getAnnotation(Name.class).value();
        String version = AnnotationHelper.constructVersion(clazz.getAnnotation(Version.class));
        return CallableNameHelper.versionedName(name, version);
    }

    @Override
    public abstract T call() throws Exception;

    protected Request getRequest() {
        return request;
    }
}
