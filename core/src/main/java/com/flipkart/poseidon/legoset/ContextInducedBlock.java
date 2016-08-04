/*
 * Copyright 2016 Flipkart Internet, pvt ltd.
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
import flipkart.lego.api.entities.Request;
import flipkart.lego.api.helpers.Identifiable;
import org.slf4j.MDC;

import java.util.Map;

import static com.flipkart.poseidon.tracing.TraceHelper.endTrace;
import static com.flipkart.poseidon.tracing.TraceHelper.startTrace;

/**
 *
 * Induces all request contexts (like contexts used by Hystrix, Brave's DT, our own RequestContext etc)
 * into DataSource/Filter threads from parent threads
 *
 * Created by mohan.pandian on 04/08/16.
 */
public abstract class ContextInducedBlock {
    /* Either data source or filter */
    private final Identifiable identifiable;

    /* Poseidon's request context from parent thread */
    private final Map<String, Object> parentContext;

    /* Poseidon's service context from parent thread */
    private final Map<String, Object> parentServiceContext;

    /* Hystrix request context from parent thread */
    private final HystrixRequestContext parentThreadState;

    /* MDC request context from parent thread */
    private final Map<String, String> mdcContext;

    /* Brave's zipkin request context from parent thread */
    private final ServerSpan serverSpan;

    /* Hystrix request context of current thread */
    private HystrixRequestContext existingState;

    /* Whether the filter or datasource was executed without exceptions */
    protected boolean success = false;

    /* Stores parent's contexts */
    protected ContextInducedBlock(Identifiable identifiable) {
        this.identifiable = identifiable;
        parentContext = RequestContext.getContextMap();
        parentServiceContext = ServiceContext.getContextMap();
        parentThreadState = HystrixRequestContext.getContextForCurrentThread();
        mdcContext = MDC.getCopyOfContextMap();
        serverSpan = Brave.getServerSpanThreadBinder().getCurrentServerSpan();
    }

    /* Passes parent's contexts to DS/filter contexts */
    protected void initAllContext(Request request) {
        existingState = HystrixRequestContext.getContextForCurrentThread();
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
        startTrace(identifiable, request);
    }

    /* Clears DS/filter contexts */
    protected void shutdownAllContext() {
        endTrace(identifiable, success);
        RequestContext.shutDown();
        ServiceContext.shutDown();
        HystrixRequestContext.setContextOnCurrentThread(existingState);
        MDC.clear();
        Brave.getServerSpanThreadBinder().setCurrentSpan(null);
    }
}
