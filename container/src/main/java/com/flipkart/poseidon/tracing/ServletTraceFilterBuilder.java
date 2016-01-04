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

package com.flipkart.poseidon.tracing;

import com.flipkart.phantom.event.consumer.PushToZipkinEventConsumer;
import com.flipkart.phantom.event.consumer.RequestLogger;
import com.flipkart.phantom.runtime.impl.jetty.filter.ServletTraceFilter;
import com.flipkart.phantom.runtime.impl.spring.ServiceProxyComponentContainer;
import com.flipkart.phantom.task.impl.collector.DelegatingZipkinSpanCollector;
import com.flipkart.phantom.task.impl.collector.EventDispatchingSpanCollector;
import com.flipkart.poseidon.api.Configuration;
import com.flipkart.poseidon.api.TracingConfiguration;
import com.github.kristofa.brave.TraceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ServletTraceFilterBuilder</code> builds a {@link ServletTraceFilter} which is a jetty filter
 * provided by phantom runtime, to kick start distributed tracing for every request. Spans are emitted
 * to the zipkin collector based on config which is controlled by {@link DynamicSampleRateTraceFilter}.
 * <p>
 * Distributed Tracing by default on prod adds almost NILL overhead. The plan is to enable it per host
 * on need basis dynamically without restarting Posidon application which will add a minor overhead of config lookups,
 * sampling counter lock, logs at Poseidon threads and minor overhead at phantom threads (which is not yet measured). Actual
 * spans and traces are pushed to Zipkin Collector asynchronously (memory requirements are also not measured yet).
 *
 * Expectation is to enable tracing for a brief period for a set of required URLs on one host and turn it off asap.
 *
 * @author Mohan Kumar Pandian
 * @version 1.0, 15th Jul, 2015
 */
public class ServletTraceFilterBuilder {
    private final static Logger LOGGER = LoggerFactory.getLogger(ServletTraceFilterBuilder.class);

    public static ServletTraceFilter build(Configuration configuration) {
        TracingConfiguration tracingConfiguration = configuration.getTracingConfiguration();
        if (tracingConfiguration == null) {
            return null;
        }

        EventDispatchingSpanCollector eventDispatchingSpanCollector = getBean("eventDispatchingSpanCollector", EventDispatchingSpanCollector.class);
        TraceFilter traceFilter = new DynamicSampleRateTraceFilter(tracingConfiguration);

        ServletTraceFilter servletTraceFilter = new ServletTraceFilter();
        servletTraceFilter.setEventDispatchingSpanCollector(eventDispatchingSpanCollector);
        servletTraceFilter.setTraceFilter(traceFilter);
        servletTraceFilter.setAppContextPath(configuration.getAppName());
        try {
            servletTraceFilter.afterPropertiesSet();
        } catch (Exception e) {
            LOGGER.error("Exception in afterPropertiesSet of ServletTraceFilter", e);
            return null;
        }

        String collectorHost = tracingConfiguration.getCollectorHost();
        int collectorPort = tracingConfiguration.getCollectorPort();
        DelegatingZipkinSpanCollector spanCollector = new DelegatingZipkinSpanCollector();
        spanCollector.setZipkinCollectorHost(collectorHost);
        spanCollector.setZipkinCollectorPort(collectorPort);

        RequestLogger requestLogger = getBean("commonRequestLogger", RequestLogger.class);
        PushToZipkinEventConsumer pushToZipkinEventConsumer = new PushToZipkinEventConsumer();
        pushToZipkinEventConsumer.setSpanCollector(spanCollector);
        pushToZipkinEventConsumer.setRequestLogger(requestLogger);
        pushToZipkinEventConsumer.setSubscriptions(new String[]{"evt://com.flipkart.phantom.events.TRACING_COLLECTOR"});
        try {
            pushToZipkinEventConsumer.afterPropertiesSet();
        } catch (Exception e) {
            LOGGER.error("Exception in afterPropertiesSet of PushToZipkinEventConsumer", e);
            return null;
        }
        return servletTraceFilter;
    }

    private static <T> T getBean(String beanId, Class<T> tClass) {
        return ServiceProxyComponentContainer.getCommonProxyHandlerBeansContext().getBean(beanId, tClass);
    }
}
