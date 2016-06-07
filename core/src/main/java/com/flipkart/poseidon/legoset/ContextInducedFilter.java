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
import flipkart.lego.api.entities.Filter;
import flipkart.lego.api.entities.Request;
import flipkart.lego.api.entities.Response;
import flipkart.lego.api.exceptions.BadRequestException;
import flipkart.lego.api.exceptions.InternalErrorException;
import flipkart.lego.api.exceptions.ProcessingException;

import java.util.List;
import java.util.Map;

import static com.flipkart.poseidon.tracing.TraceHelper.endTrace;
import static com.flipkart.poseidon.tracing.TraceHelper.startTrace;

/*
 * Induces all request contexts (like contexts used by Hystrix, Brave's DT, our own RequestContext)
 * into DataSource threads from Jetty threads
 */
public class ContextInducedFilter implements Filter {

    private final Filter filter;
    private final Map<String, Object> parentContext;
    private final Map<String, Object> parentServiceContext;
    private final HystrixRequestContext parentThreadState;
    private final ServerSpan serverSpan;

    public ContextInducedFilter(Filter filter) {
        this.filter = filter;
        parentContext = RequestContext.getContextMap();
        parentServiceContext = ServiceContext.getContextMap();
        parentThreadState = HystrixRequestContext.getContextForCurrentThread();
        serverSpan = Brave.getServerSpanThreadBinder().getCurrentServerSpan();
    }

    @Override
    public void filterRequest(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {
        HystrixRequestContext existingState = HystrixRequestContext.getContextForCurrentThread();
        boolean success = false;
        try {
            RequestContext.initialize(parentContext);
            ServiceContext.initialize(parentServiceContext);
            HystrixRequestContext.setContextOnCurrentThread(parentThreadState);
            // Parent thread span info is passed onto filter thread using Brave's ThreadLocal implementation
            if (serverSpan != null && serverSpan.getSpan() != null) {
                Brave.getServerSpanThreadBinder().setCurrentSpan(serverSpan);
            }
            // We don't want to trace requests in filter traces
            startTrace(filter);
            filter.filterRequest(request, response);
            success = true;
        } finally {
            endTrace(filter, success);
            RequestContext.shutDown();
            ServiceContext.shutDown();
            HystrixRequestContext.setContextOnCurrentThread(existingState);
            Brave.getServerSpanThreadBinder().setCurrentSpan(null);
        }
    }

    @Override
    public void filterResponse(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {
        HystrixRequestContext existingState = HystrixRequestContext.getContextForCurrentThread();
        boolean success = false;
        try {
            RequestContext.initialize(parentContext);
            ServiceContext.initialize(parentServiceContext);
            HystrixRequestContext.setContextOnCurrentThread(parentThreadState);
            // Parent thread span info is passed onto filter thread using Brave's ThreadLocal implementation
            if (serverSpan != null && serverSpan.getSpan() != null) {
                Brave.getServerSpanThreadBinder().setCurrentSpan(serverSpan);
            }
            // We don't want to trace responses in filter traces
            startTrace(filter);
            filter.filterResponse(request, response);
            success = true;
        } finally {
            endTrace(filter, success);
            RequestContext.shutDown();
            ServiceContext.shutDown();
            HystrixRequestContext.setContextOnCurrentThread(existingState);
            Brave.getServerSpanThreadBinder().setCurrentSpan(null);
        }
    }

    @Override
    public String getId() throws UnsupportedOperationException {
        return filter.getId();
    }

    @Override
    public String getName() throws UnsupportedOperationException {
        return filter.getName();
    }

    @Override
    public List<Integer> getVersion() throws UnsupportedOperationException {
        return filter.getVersion();
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof ContextInducedFilter && o.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return filter.hashCode();
    }

    @Override
    public String getShortDescription() {
        return filter.getShortDescription();
    }

    @Override
    public String getDescription() {
        return filter.getDescription();
    }
}
