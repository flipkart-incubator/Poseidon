/*
 * Copyright 2017 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon.async;

import org.springframework.http.HttpMethod;

import java.beans.ConstructorProperties;
import java.util.Map;

/**
 * Created by shrey.garg on 29/09/18.
 */
public class AsyncConsumerRequest {
    // TODO: 29/09/18 Move to EndpointPOJO::name as the identifier in a major release
    private final String url;
    private final HttpMethod httpMethod;
    private final byte[] payload;
    private final Map<String, String[]> parameters;

    @ConstructorProperties({
            "url",
            "httpMethod",
            "payload",
            "parameters"
    })
    public AsyncConsumerRequest(String url, HttpMethod httpMethod, byte[] payload, Map<String, String[]> parameters) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.payload = payload;
        this.parameters = parameters;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public byte[] getPayload() {
        return payload;
    }

    public Map<String, String[]> getParameters() {
        return parameters;
    }
}
