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
import com.flipkart.poseidon.serviceclients.ServiceContext;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ServerSpan;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import flipkart.lego.api.entities.DataSource;
import flipkart.lego.api.entities.DataType;
import flipkart.lego.api.entities.Request;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

import static com.flipkart.poseidon.tracing.TraceHelper.endTrace;
import static com.flipkart.poseidon.tracing.TraceHelper.startTrace;

/*
 * Induces all request contexts (like contexts used by Hystrix, Brave's DT, our own RequestContext)
 * into DataSource threads from Jetty threads
 */
public class ContextInducedDataSource implements DataSource {

    private final DataSource dataSource;
    private final Request request;
    private final Map<String, Object> parentContext;
    private final Map<String, Object> parentServiceContext;
    private final HystrixRequestContext parentThreadState;
    private final Map<String, String> mdcContext;
    private final ServerSpan serverSpan;

    public ContextInducedDataSource(DataSource dataSource, Request request) {
        this.dataSource = dataSource;
        this.request = request;
        parentContext = RequestContext.getContextMap();
        parentServiceContext = ServiceContext.getContextMap();
        parentThreadState = HystrixRequestContext.getContextForCurrentThread();
        mdcContext = MDC.getCopyOfContextMap();
        serverSpan = Brave.getServerSpanThreadBinder().getCurrentServerSpan();
    }

    @Override
    public DataType call() throws Exception {
        HystrixRequestContext existingState = HystrixRequestContext.getContextForCurrentThread();
        boolean success = false;
        try {
            initAllContext();
            DataType dataType = dataSource.call();
            success = true;
            return dataType;
        } finally {
            shutdownAllContext(existingState, success);
        }
    }

    private void initAllContext() {
        RequestContext.initialize(parentContext);
        ServiceContext.initialize(parentServiceContext);
        HystrixRequestContext.setContextOnCurrentThread(parentThreadState);
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
        // Parent thread span info is passed onto filter thread using Brave's ThreadLocal implementation
        if (serverSpan != null && serverSpan.getSpan() != null) {
            Brave.getServerSpanThreadBinder().setCurrentSpan(serverSpan);
        }
        startTrace(dataSource, request);
    }

    private void shutdownAllContext(HystrixRequestContext existingState, boolean success) {
        endTrace(dataSource, success);
        RequestContext.shutDown();
        ServiceContext.shutDown();
        HystrixRequestContext.setContextOnCurrentThread(existingState);
        MDC.clear();
        Brave.getServerSpanThreadBinder().setCurrentSpan(null);
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
