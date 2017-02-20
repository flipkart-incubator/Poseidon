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

package com.flipkart.poseidon.filters;

import com.flipkart.poseidon.api.Configuration;
import com.flipkart.poseidon.api.HeaderConfiguration;
import com.flipkart.poseidon.core.RequestContext;
import com.flipkart.poseidon.serviceclients.ServiceClientConstants;
import com.flipkart.poseidon.serviceclients.ServiceContext;
import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.slf4j.Logger;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.flipkart.poseidon.constants.RequestConstants.*;
import static org.slf4j.LoggerFactory.getLogger;

public class HystrixContextFilter implements Filter {
    private static final Logger logger = getLogger(HystrixContextFilter.class);
    private final Configuration configuration;

    public HystrixContextFilter(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HystrixRequestContext hystrixRequestContext = HystrixRequestContext.initializeContext();
        initAllContext(request);
        try {
            chain.doFilter(request, response);
        } finally {
            // Log all the failed Hystrix commands before shutting down context
            logFailedHystrixCommands(request);
            shutdownAllContext(hystrixRequestContext);
        }
    }

    private void initAllContext(ServletRequest request) {
        RequestContext.initialize();
        ServiceContext.initialize();
        if (request instanceof HttpServletRequest) {
            setContext((HttpServletRequest) request);
        }
    }

    private void setContext(HttpServletRequest httpServletRequest) {
        RequestContext.set(METHOD, httpServletRequest.getMethod());
        RequestContext.set(SOURCE_ADDRESS, httpServletRequest.getRemoteAddr());

        if (configuration.getHeadersConfiguration() != null && configuration.getHeadersConfiguration().getGlobalHeaders() != null) {
            Map<String, String> headers = new HashMap<>();
            for (HeaderConfiguration headerConfiguration : configuration.getHeadersConfiguration().getGlobalHeaders()) {
                String value = httpServletRequest.getHeader(headerConfiguration.getName());
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

    @Override
    public void destroy() {
    }

    /**
     * Logs details like full exception stack trace for failed Hystrix commands.
     * A command might not have been executed (say threadpool/semaphore rejected,
     * short circuited). Command might have been executed but failed (say timed out,
     * command execution failed).
     *
     * This is required as Phantom's RequestLogger logs failures of sync command
     * executions alone (and not async command executions) and doesn't provide request
     * level view of all commands.
     *
     * We log global headers here as it typically contains request id
     */
    private void logFailedHystrixCommands(ServletRequest request) {
        String url = ((HttpServletRequest) request).getPathInfo();
        Map<String, String> globalHeaders = RequestContext.get(HEADERS);

        HystrixRequestLog.getCurrentRequest().getAllExecutedCommands().stream().filter(
                command -> command.isResponseTimedOut() || command.isFailedExecution() || command.isResponseShortCircuited() || command.isResponseRejected()
        ).forEach(
                command -> logger.error("URL: {}. Global headers: {}. Command: {}. Events: {}. Exception: ",
                        url, globalHeaders, command.getCommandKey().name(), command.getExecutionEvents(), command.getFailedExecutionException())
        );
    }
}
