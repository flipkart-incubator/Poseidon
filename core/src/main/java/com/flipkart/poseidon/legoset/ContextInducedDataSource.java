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

import com.flipkart.poseidon.core.RequestContext;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ServerSpan;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import flipkart.lego.api.entities.DataSource;
import flipkart.lego.api.entities.DataType;

import java.util.List;
import java.util.Map;

/*
 * Induces all request contexts (like contexts used by Hystrix, Brave's DT, our own RequestContext)
 * into DataSource threads from Jetty threads
 */
public class ContextInducedDataSource implements DataSource {

    private final DataSource dataSource;
    private final Map<String, Object> parentContext;
    private final HystrixRequestContext parentThreadState;
    private final ServerSpan serverSpan;

    public ContextInducedDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        parentContext = RequestContext.getContextMap();
        parentThreadState = HystrixRequestContext.getContextForCurrentThread();
        serverSpan = Brave.getServerSpanThreadBinder().getCurrentServerSpan();
    }

    @Override
    public DataType call() throws Exception {
        HystrixRequestContext existingState = HystrixRequestContext.getContextForCurrentThread();
        try {
            RequestContext.initialize(parentContext);
            HystrixRequestContext.setContextOnCurrentThread(parentThreadState);
            // Parent thread span info is passed onto Datasource thread using Brave's ThreadLocal implementation
            if (serverSpan != null && serverSpan.getSpan() != null) {
                Brave.getServerSpanThreadBinder().setCurrentSpan(serverSpan);
            }
            return dataSource.call();
        } finally {
            RequestContext.shutDown();
            HystrixRequestContext.setContextOnCurrentThread(existingState);
            Brave.getServerSpanThreadBinder().setCurrentSpan(null);
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
