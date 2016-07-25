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

package com.flipkart.poseidon.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.flipkart.poseidon.utils.ApiHelper;
import org.springframework.http.HttpMethod;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EndpointPOJO {

    private String url;
    private HttpMethod httpMethod;
    private long timeout;
    /*
     * API Name used in Timer to expose endpoint metrics (latency, rate of requests).
     * Metrics are named like "poseidon.api.<name>.<httpMethod>"
     */
    private String name;
    private boolean deprecated;
    private ParamsPOJO params;
    private Map<String, TaskPOJO> tasks;
    private String[] filters;
    private String[] mappers;
    private Object response;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = ApiHelper.getFormattedUrl(url);
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getName() {
        return name;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public ParamsPOJO getParams() {
        return params;
    }

    public Map<String, TaskPOJO> getTasks() {
        return tasks;
    }

    public String[] getFilters() {
        return filters;
    }

    public String[] getMappers() {
        return mappers;
    }

    public Object getResponse() {
        return response;
    }
}
