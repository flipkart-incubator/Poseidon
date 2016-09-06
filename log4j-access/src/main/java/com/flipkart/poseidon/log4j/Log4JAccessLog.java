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

package com.flipkart.poseidon.log4j;

import com.flipkart.poseidon.log4j.message.AccessLog;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

import java.io.File;
import java.util.function.Supplier;

/**
 * Log4JAccessLog implements jetty's {@link RequestLog} to generate
 * access logs using log4j2 (similar to logback-access RequestLogImpl
 * which generates jetty access logs using logback)
 * <p/>
 * Created by mohan.pandian on 26/08/16.
 */
public class Log4JAccessLog extends AbstractLifeCycle implements RequestLog {
    private final String accessConfigFilePath;
    private final Supplier<Boolean> isEnabledSupplier;

    private LoggerContext loggerContext;
    private Logger logger;

    public Log4JAccessLog(String accessConfigFilePath) {
        this(accessConfigFilePath, () -> true);
    }

    public Log4JAccessLog(String accessConfigFilePath, Supplier<Boolean> isEnabledSupplier) {
        this.accessConfigFilePath = accessConfigFilePath;
        this.isEnabledSupplier = isEnabledSupplier;
    }

    @Override
    protected void doStart() throws Exception {
        File accessConfigFile = new File(accessConfigFilePath);
        if (!accessConfigFile.isFile()) {
            throw new Exception("Log4J access config file not found: " + accessConfigFilePath);
        }

        loggerContext = new Log4jContextFactory().getContext(Log4JAccessLog.class.getName(), null, null, true, accessConfigFile.toURI(), "PoseidonLog4JAccess");
        logger = loggerContext.getRootLogger();
    }

    @Override
    protected void doStop() throws Exception {
        loggerContext.stop();
    }

    private boolean isEnabled() {
        return isEnabledSupplier.get();
    }

    /**
     * Generates access log statement. If async logging is enabled,
     * jetty thread won't be blocked till log is written to disk.
     *
     * Access log can be dynamically disabled by returning false from supplier.
     *
     * @param request jetty's request
     * @param response jetty's response
     */
    @Override
    public final void log(Request request, Response response) {
        if (!isEnabled()) {
            return;
        }

        // Instead of generating the access log line here directly, we form AccessLog object with values
        // copied from request and response and log this object using various converters.
        // Hence access log pattern is configurable and extensible.
        logger.log(Level.INFO, new AccessLog(request, response));
    }
}
