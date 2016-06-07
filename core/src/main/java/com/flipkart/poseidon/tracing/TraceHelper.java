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

package com.flipkart.poseidon.tracing;

import com.flipkart.phantom.runtime.impl.spring.ServiceProxyComponentContainer;
import com.flipkart.phantom.task.impl.collector.EventDispatchingSpanCollector;
import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.poseidon.core.RequestContext;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ClientTracer;
import com.github.kristofa.brave.TraceFilter;
import flipkart.lego.api.entities.Request;
import flipkart.lego.api.helpers.Identifiable;

import java.util.Arrays;
import java.util.List;

import static com.flipkart.poseidon.constants.RequestConstants.ENABLE_TRACING;
import static com.flipkart.poseidon.helper.BlocksHelper.getName;
import static com.flipkart.poseidon.helper.BlocksHelper.trace;

/**
 * Contains helper methods to enable Distributed Tracing on
 * lego blocks like datasources, filters etc.
 *
 * Plugs phantom, brave and lego blocks together to achieve this.
 *
 * Created by mohan.pandian on 31/05/16.
 */
public class TraceHelper {
    private static final String FAILURE_ANNOTATION = "failure";
    private static final String REQUEST_ANNOTATION = "request";
    private static final List<TraceFilter> TRACING_ON_FILTERS = Arrays.asList(AbstractHandler.TRACING_ON);
    private static final EventDispatchingSpanCollector SPAN_COLLECTOR;

    static {
        SPAN_COLLECTOR = getBean("eventDispatchingSpanCollector", EventDispatchingSpanCollector.class);
    }

    public static <T> T getBean(String beanId, Class<T> tClass) {
        return ServiceProxyComponentContainer.getCommonProxyHandlerBeansContext().getBean(beanId, tClass);
    }

    public static EventDispatchingSpanCollector getSpanCollector() {
        return SPAN_COLLECTOR;
    }

    public static void startTrace(Identifiable block) {
        startTrace(block, null);
    }

    public static boolean isTracingEnabledRequest() {
        Boolean enableTracing = RequestContext.get(ENABLE_TRACING);
        return (enableTracing != null && enableTracing);
    }

    public static void startTrace(Identifiable block, Request request) {
        if (!isTracingEnabledRequest() || !trace(block)) {
            return;
        }

        String name = getName(block);
        ClientTracer clientTracer = Brave.getClientTracer(SPAN_COLLECTOR, TRACING_ON_FILTERS);
        clientTracer.startNewSpan(name);
        clientTracer.setCurrentClientServiceName(name);
        if (request != null) {
            clientTracer.submitBinaryAnnotation(REQUEST_ANNOTATION, request.toString());
        }
        clientTracer.setClientSent();
    }

    public static void endTrace(Identifiable block, boolean success) {
        if (!isTracingEnabledRequest() || !trace(block)) {
            return;
        }

        ClientTracer clientTracer = Brave.getClientTracer(SPAN_COLLECTOR, TRACING_ON_FILTERS);
        if (!success) {
            clientTracer.submitAnnotation(FAILURE_ANNOTATION);
        }
        clientTracer.setClientReceived();
    }
}
