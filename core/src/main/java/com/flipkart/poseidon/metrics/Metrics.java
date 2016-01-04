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

package com.flipkart.poseidon.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;

/**
 * A container that holds a single instance of a codahale {@link MetricRegistry}
 * and a single instance of codahale {@link HealthCheckRegistry}
 */
public final class Metrics {

    private static final MetricRegistry registry = new MetricRegistry();
    private static final HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

    private Metrics() {
    }

    /**
     * Returns an instance of codahale {@link MetricRegistry}
     *
     * @return instance of codahale {@link MetricRegistry}
     */
    public static MetricRegistry getRegistry() {
        return registry;
    }

    /**
     * Returns an instance of codahale {@link HealthCheckRegistry}
     *
     * @return instance of codahale {@link HealthCheckRegistry}
     */
    public static HealthCheckRegistry getHealthCheckRegistry() {
        return healthCheckRegistry;
    }
}
