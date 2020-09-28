/*
 * Copyright 2018 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon.async;

import com.flipkart.poseidon.api.Application;
import com.flipkart.poseidon.api.Configuration;
import com.flipkart.poseidon.api.HeaderConfiguration;
import com.flipkart.poseidon.constants.RequestConstants;
import com.flipkart.poseidon.core.PoseidonAsyncRequest;
import com.flipkart.poseidon.core.PoseidonRequest;
import com.flipkart.poseidon.core.PoseidonResponse;
import com.flipkart.poseidon.core.RequestContext;
import com.flipkart.poseidon.handlers.http.utils.StringUtils;
import com.flipkart.poseidon.helpers.MetricsHelper;
import com.flipkart.poseidon.metrics.Metrics;
import com.flipkart.poseidon.serviceclients.ServiceClientConstants;
import com.flipkart.poseidon.serviceclients.ServiceContext;
import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static com.flipkart.poseidon.constants.RequestConstants.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by shrey.garg on 29/09/18.
 */
public abstract class PoseidonConsumer {
    private static final Logger logger = getLogger(PoseidonConsumer.class);
    private final Application application;
    private final Configuration configuration;

    public PoseidonConsumer(Application application, Configuration configuration) {
        this.application = application;
        this.configuration = configuration;
    }

    public final AsyncConsumerResult consume(AsyncConsumerRequest consumerRequest) {
        Map<String, String> caseInsensitiveHeaders = Optional.ofNullable(consumerRequest.getHeaders()).orElse(Collections.emptyMap()).entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().toLowerCase(),
                Map.Entry::getValue
        ));

        PoseidonRequest request = new PoseidonAsyncRequest(consumerRequest.getUrl(), Collections.emptyMap(), caseInsensitiveHeaders, consumerRequest.getParameters());
        request.setAttribute(METHOD, consumerRequest.getHttpMethod());

        if (consumerRequest.getPayload() != null) {
            request.setAttribute(BODY_BYTES, consumerRequest.getPayload());
        }

        HystrixRequestContext hystrixRequestContext = HystrixRequestContext.initializeContext();
        initAllContext(request);

        PoseidonResponse response = null;
        try {
            response = new PoseidonResponse();
            this.application.handleRequest(request, response);
            return new AsyncConsumerResult(AsyncResultState.SUCCESS);
        } catch (Throwable throwable) {
            logger.error("Unexpected exception while consuming async event", throwable);
            return new AsyncConsumerResult(AsyncResultState.FAILURE);
        } finally {
            ingestResponseBasedMetrics(response);
            logFailedHystrixCommands(consumerRequest);
            shutdownAllContext(hystrixRequestContext);
        }
    }

    private void initAllContext(PoseidonRequest request) {
        RequestContext.initialize();
        ServiceContext.initialize(configuration.getResponseHeadersToCollect());
        setContext(request);
    }

    private void setContext(PoseidonRequest request) {
        RequestContext.set(METHOD, request.getAttribute(METHOD).toString());
        RequestContext.set(SOURCE_ADDRESS, request.getUrl());

        if (configuration.getHeadersConfiguration() != null && configuration.getHeadersConfiguration().getGlobalHeaders() != null) {
            Map<String, String> headers = new HashMap<>();
            for (HeaderConfiguration headerConfiguration : configuration.getHeadersConfiguration().getGlobalHeaders()) {
                String value = request.getHeader(headerConfiguration.getName());
                if (value == null) {
                    value = headerConfiguration.getDefaultValue();
                }
                if (value != null) {
                    headers.put(headerConfiguration.getName(), value);
                }
            }

            ImmutableMap<String, String> immutableHeaders = ImmutableMap.copyOf(headers);
            ServiceContext.set(ServiceClientConstants.HEADERS, immutableHeaders);
            ServiceContext.set(ServiceClientConstants.COMMANDS, new ConcurrentLinkedQueue<String>());
            ServiceContext.set(ServiceClientConstants.COLLECT_COMMANDS, configuration.collectServiceClientCommandNames());
            ServiceContext.set(ServiceClientConstants.THROW_ORIGINAL, configuration.throwOriginalExceptionsForNonUpstreamFailures());
            RequestContext.set(HEADERS, immutableHeaders);
            MDC.setContextMap(immutableHeaders);
        }
    }

    private void shutdownAllContext(HystrixRequestContext hystrixRequestContext) {
        RequestContext.shutDown();
        ServiceContext.shutDown();
        hystrixRequestContext.shutdown();
        MDC.clear();
    }

    private void ingestResponseBasedMetrics(PoseidonResponse response) {
        if (!StringUtils.isNullOrEmpty(RequestContext.get(ENDPOINT_NAME))) {
            String status = (response.getStatusCode() / 100) + "XX";
            Metrics.getRegistry()
                    .meter(MetricsHelper.getStatusCodeMetricsName(RequestContext.get(ENDPOINT_NAME), RequestContext.get(RequestConstants.METHOD), status))
                    .mark();
        }
    }

    private void logFailedHystrixCommands(AsyncConsumerRequest request) {
        String url = request.getUrl();
        Map<String, String> globalHeaders = RequestContext.get(HEADERS);

        HystrixRequestLog.getCurrentRequest().getAllExecutedCommands().stream().filter(
                command -> command.isResponseTimedOut() || command.isFailedExecution() || command.isResponseShortCircuited() || command.isResponseRejected()
        ).forEach(
                command -> logger.error("URL: {}. Global headers: {}. Command: {}. Events: {}. Exception: {}",
                        url, globalHeaders, command.getCommandKey().name(), command.getExecutionEvents(),
                        command.getFailedExecutionException() == null ? "" : command.getFailedExecutionException().getMessage())
        );
    }
}
