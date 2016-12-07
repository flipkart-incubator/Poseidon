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

package com.flipkart.poseidon.legoset;

import flipkart.lego.api.entities.DataSource;
import flipkart.lego.api.entities.DataType;
import flipkart.lego.api.entities.Request;

/*
 * Induces all request contexts (like contexts used by Hystrix, Brave's DT, our own RequestContext)
 * into DataSource threads from Jetty threads
 */
public class ContextInducedDataSource<T extends DataType> extends ContextInducedBlock implements DataSource<T> {

    private final DataSource<T> dataSource;
    private final Request request;

    public ContextInducedDataSource(DataSource<T> dataSource, Request request) {
        super(dataSource);
        this.dataSource = dataSource;
        this.request = request;
    }

    @Override
    public T call() throws Exception {
        try {
            initAllContext(request);
            T dataType = dataSource.call();
            success = true;
            return dataType;
        } finally {
            shutdownAllContext();
        }
    }
}
