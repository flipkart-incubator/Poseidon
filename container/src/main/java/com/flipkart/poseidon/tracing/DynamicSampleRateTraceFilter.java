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

import com.flipkart.poseidon.api.TracingConfiguration;
import com.flipkart.poseidon.core.RequestContext;
import com.github.kristofa.brave.FixedSampleRateTraceFilter;
import com.github.kristofa.brave.TraceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.flipkart.poseidon.constants.RequestConstants.ENABLE_TRACING;

/**
 * <code>DynamicSampleRateTraceFilter</code> is an implementation of {@link TraceFilter}
 * that determines whether to trace requests using {@link TracingConfiguration}.
 *
 * <p>
 * Globally tracing can be turned on/off by using config flag <code>TracingConfiguration.enableTracing()</code>.
 * Once tracing is turned on globally, tracing for individual URLs can be controlled using map config
 * <code>TracingConfiguration.getSampleRateMap()</code>. It is read only when global tracing is toggled from off to on.
 * Once the config is read, it won't be reloaded until global tracing is switched off and on again.
 *
 * Current sampling counter per URL is kept in an instance of {@link FixedSampleRateTraceFilter} which shouldn't be
 * reconstructed for every request. Hence we avoid reading this config once read into memory.
 * </p>
 * <p>
 * Ex: To turn on tracing for all requests, use
 * <p>
 * <code>TracingConfiguration.enableTracing() should return true</code><br>
 * <code>TracingConfiguration.getSampleRateMap() should return {"*": 1}</code>
 * </p>
 *
 * To turn on tracing for only some URLs, say 1 out of 100 "/some_api" and 1 out of 50 "/some_other_api" requests should be traced
 * and to turn off for all other URLs, use
 * <p>
 * <code>TracingConfiguration.enableTracing() should return true</code><br>
 * <code>TracingConfiguration.getSampleRateMap() should return {"/some_api": 100, "/some_other_api": 50}</code>
 * </p>
 *
 * @author Mohan Kumar Pandian
 * @version 1.0, 15th Jul, 2015
 */
public class DynamicSampleRateTraceFilter implements TraceFilter {
    private final TracingConfiguration tracingConfiguration;
    private final static Logger LOGGER = LoggerFactory.getLogger(DynamicSampleRateTraceFilter.class);

    private Map<String, TraceFilter> currentTraceFilterMap;

    public DynamicSampleRateTraceFilter(TracingConfiguration tracingConfiguration) {
        this.tracingConfiguration = tracingConfiguration;
    }

    @Override
    public boolean trace(final String requestName) {
        if (!isTracingEnabled() || requestName == null || requestName.isEmpty()) {
            RequestContext.set(ENABLE_TRACING, false);
            return false;
        }

        Map<String, TraceFilter> traceFilterMap = getTraceFilterMap();
        for(Map.Entry<String, TraceFilter> entry: traceFilterMap.entrySet()) {
            String requestPattern = entry.getKey();
            TraceFilter traceFilter = entry.getValue();

            if ("*".equals(requestPattern)) {
                boolean enableTracing = traceFilter.trace(requestName);
                if (enableTracing) {
                    LOGGER.debug("Wildcard match. Tracing enabled for {}", requestName);
                } else {
                    LOGGER.debug("Wildcard match. Tracing not enabled for {}", requestName);
                }
                RequestContext.set(ENABLE_TRACING, enableTracing);
                return enableTracing;
            }

            if (requestName.indexOf(requestPattern) > -1) {
                boolean enableTracing = traceFilter.trace(requestName);
                if (enableTracing) {
                    LOGGER.debug("Pattern match {}. Tracing enabled for {}", requestPattern, requestName);
                } else {
                    LOGGER.debug("Pattern match {}. Tracing not enabled for {}", requestPattern, requestName);
                }
                RequestContext.set(ENABLE_TRACING, enableTracing);
                return enableTracing;
            }
        }
        LOGGER.debug("No pattern matched. Tracing not enabled for {}", requestName);
        RequestContext.set(ENABLE_TRACING, false);
        return false;
    }

    @Override
    public void close() {
        // NO-OP
    }

    private boolean isTracingEnabled() {
        boolean isTracingEnabled = tracingConfiguration.enableTracing();
        if (!isTracingEnabled && currentTraceFilterMap != null) {
            currentTraceFilterMap = null;
        }
        return isTracingEnabled;
    }

    private Map<String, TraceFilter> getTraceFilterMap() {
        if (currentTraceFilterMap == null) {
            synchronized (this) {
                if (currentTraceFilterMap == null && isTracingEnabled()) {
                    Map<String, TraceFilter> traceFilterMap = new HashMap<>();
                    Map<String, Integer> sampleRateMap = tracingConfiguration.getSampleRateMap();
                    if (sampleRateMap != null) {
                        for (Map.Entry<String, Integer> entry : sampleRateMap.entrySet()) {
                            String requestPattern = entry.getKey();
                            Integer sampleRate = entry.getValue();
                            if (sampleRate == null) {
                                continue;
                            }

                            traceFilterMap.put(requestPattern, new FixedSampleRateTraceFilter(sampleRate));
                        }
                    }
                    currentTraceFilterMap = traceFilterMap;
                    LOGGER.debug("Reloaded URL sampling rate config with {} entries", currentTraceFilterMap.size());
                }
            }
        }
        return currentTraceFilterMap;
    }
}
