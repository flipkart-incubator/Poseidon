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

package com.flipkart.poseidon.model.trace;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

/**
 * Created by shrey.garg on 30/05/17.
 */
@JsonPropertyOrder({
        "httpMethod",
        "uri",
        "success",
        "errorIdentifier",
        "headersMap",
        "requestObject",
        "cacheCandidates",
        "serviceResponse",
        "responseHeaders"
})
public class ServiceCallDebug {
    private String uri;
    private String httpMethod;
    private Map<String, String> headersMap;
    private Object requestObject;
    private Object serviceResponse;
    private boolean success = true;
    private String errorIdentifier;
    private Map<String, String> responseHeaders;
    private int cacheCandidates;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Map<String, String> getHeadersMap() {
        return headersMap;
    }

    public void setHeadersMap(Map<String, String> headersMap) {
        this.headersMap = headersMap;
    }

    public Object getRequestObject() {
        return requestObject;
    }

    public void setRequestObject(Object requestObject) {
        this.requestObject = requestObject;
    }

    public Object getServiceResponse() {
        return serviceResponse;
    }

    public void setServiceResponse(Object serviceResponse) {
        this.serviceResponse = serviceResponse;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }

    public void setErrorIdentifier(String errorIdentifier) {
        this.errorIdentifier = errorIdentifier;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public int getCacheCandidates() {
        return cacheCandidates;
    }

    public void setCacheCandidates(int cacheCandidates) {
        this.cacheCandidates = cacheCandidates;
    }
}
