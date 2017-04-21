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

package com.flipkart.poseidon.helpers;

/**
 * Created by shubham.srivastava on 19/04/17.
 */
public class MetricsHelper {

    private static String BASE_METRICS_NAME = "poseidon.api.";
    private static String DEFAULT_DELIMITER = ".";

    public static String getStatusCodeMetricsName(String endpoint, String method, String status) {
        return new StringBuilder(BASE_METRICS_NAME)
                .append(endpoint)
                .append(method)
                .append(DEFAULT_DELIMITER)
                .append(status)
                .toString();
    }

    public static String getApiTimerMetricsName(String endpoint, String method) {
        return new StringBuilder(BASE_METRICS_NAME)
                .append(endpoint)
                .append(DEFAULT_DELIMITER)
                .append(method)
                .toString();
    }

}
