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
