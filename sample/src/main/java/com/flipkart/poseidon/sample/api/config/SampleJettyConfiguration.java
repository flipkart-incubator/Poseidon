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

package com.flipkart.poseidon.sample.api.config;

import com.flipkart.poseidon.api.JettyConfiguration;
import com.flipkart.poseidon.api.JettyFilterConfiguration;
import com.flipkart.poseidon.filters.DebugFilter;

import java.util.Arrays;
import java.util.List;

public class SampleJettyConfiguration implements JettyConfiguration {
    private int acceptors;
    private int selectors;
    private int acceptQueueSize;
    private int taskQueueSize;
    private int minThreads;
    private int maxThreads;
    private int threadIdleTimeout;

    @Override
    public int getAcceptors() {
        return acceptors;
    }

    public void setAcceptors(int acceptors) {
        this.acceptors = acceptors;
    }

    @Override
    public int getSelectors() {
        return selectors;
    }

    public void setSelectors(int selectors) {
        this.selectors = selectors;
    }

    @Override
    public int getAcceptQueueSize() {
        return acceptQueueSize;
    }

    public void setAcceptQueueSize(int acceptQueueSize) {
        this.acceptQueueSize = acceptQueueSize;
    }

    @Override
    public int getTaskQueueSize() {
        return taskQueueSize;
    }

    public void setTaskQueueSize(int taskQueueSize) {
        this.taskQueueSize = taskQueueSize;
    }

    @Override
    public int getMinThreads() {
        return minThreads;
    }

    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
    }

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    @Override
    public int getThreadIdleTimeout() {
        return threadIdleTimeout;
    }

    public void setThreadIdleTimeout(int threadIdleTimeout) {
        this.threadIdleTimeout = threadIdleTimeout;
    }

    public List<JettyFilterConfiguration> getJettyFilterConfigurations() {
        JettyFilterConfiguration debugFilterConfig = new JettyFilterConfiguration(new DebugFilter());
        return Arrays.asList(debugFilterConfig);
    }
}
