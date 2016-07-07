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

import com.flipkart.poseidon.core.RequestContext;
import com.flipkart.poseidon.serviceclients.ServiceContext;
import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.slf4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

import static com.flipkart.poseidon.constants.RequestConstants.HEADERS;
import static org.slf4j.LoggerFactory.getLogger;

public class HystrixContextFilter implements Filter {
    private static final Logger logger = getLogger(HystrixContextFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HystrixRequestContext hystrixRequestContext = HystrixRequestContext.initializeContext();
        RequestContext.initialize();
        ServiceContext.initialize();
        try {
            chain.doFilter(request, response);
        } finally {
            // Log all the failed Hystrix commands before shutting down context
            logFailedHystrixCommands(request);

            RequestContext.shutDown();
            ServiceContext.shutDown();
            hystrixRequestContext.shutdown();
        }
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
