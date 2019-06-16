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

package com.flipkart.poseidon;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jetty9.InstrumentedHandler;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codahale.metrics.servlets.AdminServlet;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.fasterxml.jackson.databind.JavaType;
import com.flipkart.phantom.runtime.impl.jetty.filter.ServletTraceFilter;
import com.flipkart.poseidon.api.Application;
import com.flipkart.poseidon.api.Configuration;
import com.flipkart.poseidon.api.JettyConfiguration;
import com.flipkart.poseidon.api.JettyFilterConfiguration;
import com.flipkart.poseidon.core.PoseidonServlet;
import com.flipkart.poseidon.core.RewriteRule;
import com.flipkart.poseidon.filters.HystrixContextFilter;
import com.flipkart.poseidon.filters.RequestGzipFilter;
import com.flipkart.poseidon.healthchecks.Rotation;
import com.flipkart.poseidon.log4j.Log4JAccessLog;
import com.flipkart.poseidon.metrics.Metrics;
import com.flipkart.poseidon.rotation.BackInRotationServlet;
import com.flipkart.poseidon.rotation.OutOfRotationServlet;
import com.flipkart.poseidon.rotation.RotationCheckServlet;
import com.flipkart.poseidon.tracing.ServletTraceFilterBuilder;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.trpr.platform.runtime.impl.bootstrap.spring.Bootstrap;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.flipkart.poseidon.helpers.ObjectMapperHelper.getMapper;
import static javax.servlet.DispatcherType.REQUEST;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class Poseidon implements ApplicationContextAware {

    public static final Logger STARTUP_LOGGER = getLogger("PoseidonStartupLogger");

    private final Configuration configuration;
    private final Application application;
    private final RotationCheckServlet rotationCheckServlet;
    private final BackInRotationServlet backInRotationServlet;
    private final OutOfRotationServlet outOfRotationServlet;
    private final ExecutorService dataSourceES;
    private final ExecutorService filterES;
    private ApplicationContext context;
    private Server server;

    @Autowired
    public Poseidon(Configuration configuration, Application application, RotationCheckServlet rotationCheckServlet,
                    BackInRotationServlet backInRotationServlet, OutOfRotationServlet outOfRotationServlet) {
        this.configuration = configuration;
        this.application = application;
        this.rotationCheckServlet = rotationCheckServlet;
        this.backInRotationServlet = backInRotationServlet;
        this.outOfRotationServlet = outOfRotationServlet;

        dataSourceES = Executors.newCachedThreadPool();
        filterES = Executors.newCachedThreadPool();
    }

    public static void main(String[] args) {
        if(args.length < 1) {
            throw new RuntimeException("No Bootstrap File Specified");
        }

        (new Bootstrap()).init(args[0]);
    }

    public void stop() {
        STARTUP_LOGGER.info("*** Poseidon - stopping application... ***");
        application.stop();

        STARTUP_LOGGER.info("*** Poseidon - stopping executor services... ***");
        dataSourceES.shutdown();
        filterES.shutdown();

        if (server != null) {
            STARTUP_LOGGER.info("*** Poseidon - stopping server... ***");
            try {
                server.stop();
            } catch(Exception e) {
                STARTUP_LOGGER.error("Exception stopping server", e);
            }
        }

        STARTUP_LOGGER.info("*** Poseidon stopped ***");
    }

    public void start() {
        try {
            JettyConfiguration jettyConfiguration = configuration.getJettyConfiguration();
            if (jettyConfiguration != null) {
                // Use a bounded queue over jetty's default unbounded queue
                // https://wiki.eclipse.org/Jetty/Howto/High_Load#Thread_Pool
                BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(jettyConfiguration.getTaskQueueSize());
                QueuedThreadPool threadPool = new QueuedThreadPool(jettyConfiguration.getMaxThreads(),
                        jettyConfiguration.getMinThreads(), jettyConfiguration.getThreadIdleTimeout(), queue);
                server = new Server(threadPool);
                ServerConnector connector = new ServerConnector(
                        server,
                        jettyConfiguration.getAcceptors(),
                        jettyConfiguration.getSelectors(),
                        Optional.ofNullable(jettyConfiguration.getHttpConnectionFactory()).orElseGet(HttpConnectionFactory::new));
                connector.setPort(configuration.getPort());
                connector.setAcceptQueueSize(jettyConfiguration.getAcceptQueueSize());
                server.setConnectors(new Connector[] { connector });
            } else {
                server = new Server(configuration.getPort());
            }
            if (!configuration.sendServerVersion()) {
                disableSendingServerVersion();
            }

            server.setHandler(getHandlers());
            application.init(dataSourceES, filterES, context);
            initializeMetricReporters();

            server.start();
            STARTUP_LOGGER.info("*** Poseidon started ***");
        } catch (Exception e) {
            STARTUP_LOGGER.error("Unable to start Poseidon.", e);
            throw new RuntimeException("Unable to start Poseidon", e);
        }
    }

    private HandlerCollection getHandlers() {
        ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
        contextHandlerCollection.setHandlers(new Handler[]{
                getRequestLogHandler(getGzipHandler(getParentHandler())),
                getMetricsHandler()
        });
        return contextHandlerCollection;
    }

    private Handler getGzipHandler(Handler handler) {
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.addIncludedMethods("GET", "POST", "PUT", "DELETE", "PATCH");
        gzipHandler.addIncludedPaths("/*");
        gzipHandler.setHandler(handler);
        return gzipHandler;
    }

    private Handler getRequestLogHandler(Handler handler) {
        RequestLog requestLog = new Log4JAccessLog(configuration.getAccessLogConfigFilePath(),
                () -> configuration.isAccessLogEnabled());
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(requestLog);
        requestLogHandler.setHandler(handler);

        return requestLogHandler;
    }

    private Handler getParentHandler() {
        Handler poseidonHandler = getPoseidonHandler();
        return getRewriteHandler(poseidonHandler).orElse(poseidonHandler);
    }

    private Optional<Handler> getRewriteHandler(Handler handler) {
        try {
            String rewriteFilePath = configuration.getRewriteFilePath();
            if (rewriteFilePath != null && !(rewriteFilePath = rewriteFilePath.trim()).isEmpty()) {
                RewriteHandler rewriteHandler = new RewriteHandler();
                rewriteHandler.setRewritePathInfo(false);
                rewriteHandler.setRewriteRequestURI(true);
                rewriteHandler.setOriginalPathAttribute("__path");
                rewriteHandler.setHandler(handler);

                JavaType listRuleType = getMapper().getTypeFactory().constructParametricType(List.class, RewriteRule.class);
                List<RewriteRule> rules = getMapper().readValue(new FileInputStream(rewriteFilePath), listRuleType);
                boolean isAnyRuleActive = false;
                for (RewriteRule rule : rules) {
                    if (rule.isActive()) {
                        RewriteRegexRule regexRule = new RewriteRegexRule();
                        regexRule.setRegex(rule.getFrom());
                        regexRule.setReplacement(rule.getTo());
                        rewriteHandler.addRule(regexRule);
                        isAnyRuleActive = true;
                    }
                }
                return isAnyRuleActive ? Optional.of(rewriteHandler) : Optional.empty();
            }
        } catch (IOException e) {
            STARTUP_LOGGER.error("Unable to read Rewrite Rules", e);
        }
        return Optional.empty();
    }

    private Handler getPoseidonHandler() {
        ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath("/");
        servletContextHandler.addServlet(new ServletHolder(getPoseidonServlet()), "/*");
        servletContextHandler.addServlet(new ServletHolder(rotationCheckServlet), "/_poseidon/health");
        servletContextHandler.addServlet(new ServletHolder(backInRotationServlet), "/_poseidon/bir");
        servletContextHandler.addServlet(new ServletHolder(outOfRotationServlet), "/_poseidon/oor");

        addFilters(servletContextHandler);

        InstrumentedHandler instrumentedHandler = new InstrumentedHandler(Metrics.getRegistry());
        instrumentedHandler.setHandler(servletContextHandler);
        return instrumentedHandler;
    }

    private ServletContextHandler getMetricsHandler() {
        MetricRegistry registry = Metrics.getRegistry();
        HealthCheckRegistry healthCheckRegistry = Metrics.getHealthCheckRegistry();
        healthCheckRegistry.register("rotation", new Rotation(configuration.getRotationStatusFilePath()));

        registry.registerAll(new GarbageCollectorMetricSet());
        registry.registerAll(new MemoryUsageGaugeSet());
        registry.registerAll(new ThreadStatesGaugeSet());
        registry.registerAll(new JvmAttributeGaugeSet());

        ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath("/__metrics");
        servletContextHandler.setAttribute(MetricsServlet.class.getCanonicalName() + ".registry", registry);
        servletContextHandler.setAttribute(HealthCheckServlet.class.getCanonicalName() + ".registry", healthCheckRegistry);
        servletContextHandler.addServlet(new ServletHolder(new AdminServlet()), "/*");

        return servletContextHandler;
    }

    private static void initializeMetricReporters() {
        final JmxReporter jmxReporter = JmxReporter.forRegistry(Metrics.getRegistry()).build();
        jmxReporter.start();
    }

    private void addFilters(ServletContextHandler servletContextHandler) {
        // RequestContext is required in other filters, hence set it up first
        servletContextHandler.addFilter(new FilterHolder(new HystrixContextFilter(configuration)), "/*", EnumSet.of(REQUEST));
        // Set up distributed tracing filter
        ServletTraceFilter servletTraceFilter = ServletTraceFilterBuilder.build(configuration);
        if (servletTraceFilter != null) {
            servletContextHandler.addFilter(new FilterHolder(servletTraceFilter), "/*", EnumSet.of(REQUEST));
        }
        servletContextHandler.addFilter(new FilterHolder(new RequestGzipFilter()), "/*", EnumSet.of(REQUEST));

        List<JettyFilterConfiguration> jettyFilterConfigurations = Optional.ofNullable(configuration.getJettyConfiguration()).map(JettyConfiguration::getJettyFilterConfigurations).orElse(new ArrayList<>());
        for (JettyFilterConfiguration filterConfig : jettyFilterConfigurations) {
            FilterHolder filterHolder = new FilterHolder(filterConfig.getFilter());
            filterHolder.setInitParameters(filterConfig.getInitParameters());
            for (String mapping : filterConfig.getMappings()) {
                servletContextHandler.addFilter(filterHolder, mapping, filterConfig.getDispatcherTypes());
            }
        }
    }

    private PoseidonServlet getPoseidonServlet() {
        return new PoseidonServlet(application, configuration);
    }

    private void disableSendingServerVersion() {
        for (Connector connector : server.getConnectors()) {
            for (ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
                if (connectionFactory instanceof HttpConnectionFactory) {
                    ((HttpConnectionFactory) connectionFactory).getHttpConfiguration().setSendServerVersion(false);
                }
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
