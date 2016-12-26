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

import com.flipkart.poseidon.api.TracingConfiguration;

import java.util.Map;

public class SampleTracingConfiguration implements TracingConfiguration {
    private String collectorHost;
    private int port;
    private boolean enableTracing;
    private Map<String, Integer> sampleRateMap;

    @Override
    public String getCollectorHost() {
        return collectorHost;
    }

    public void setCollectorHost(String collectorHost) {
        this.collectorHost = collectorHost;
    }

    @Override
    public int getCollectorPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean enableTracing() {
        return enableTracing;
    }

    public void setEnableTracing(boolean enableTracing) {
        this.enableTracing = enableTracing;
    }

    @Override
    public Map<String, Integer> getSampleRateMap() {
        return sampleRateMap;
    }

    public void setSampleRateMap(Map<String, Integer> sampleRateMap) {
        this.sampleRateMap = sampleRateMap;
    }
}
