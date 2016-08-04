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

import java.util.List;

/*
 * Induces all request contexts (like contexts used by Hystrix, Brave's DT, our own RequestContext)
 * into DataSource threads from Jetty threads
 */
public class ContextInducedDataSource extends ContextInducedBlock implements DataSource {

    private final DataSource dataSource;
    private final Request request;

    public ContextInducedDataSource(DataSource dataSource, Request request) {
        super(dataSource);
        this.dataSource = dataSource;
        this.request = request;
    }

    @Override
    public DataType call() throws Exception {
        try {
            initAllContext(request);
            DataType dataType = dataSource.call();
            success = true;
            return dataType;
        } finally {
            shutdownAllContext();
        }
    }

    @Override
    public String getId() throws UnsupportedOperationException {
        return dataSource.getId();
    }

    @Override
    public String getName() throws UnsupportedOperationException {
        return dataSource.getName();
    }

    @Override
    public List<Integer> getVersion() throws UnsupportedOperationException {
        return dataSource.getVersion();
    }

    @Override
    public String getShortDescription() {
        return dataSource.getShortDescription();
    }

    @Override
    public String getDescription() {
        return dataSource.getDescription();
    }
}
