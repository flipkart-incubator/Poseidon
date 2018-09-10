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

package com.flipkart.poseidon.serviceclients.idl.pojo;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Created by mohan.pandian on 17/02/15.
 */
public class EndPoint {
    private String httpMethod;
    private String uri;
    private String commandName;
    private boolean dynamicCommandName;
    private Map<String, String> headers;
    private String[] parameters;
    private boolean requestCachingEnabled;
    private String requestObject;
    private String requestSplitterClass;
    private String requestParamWithLimit;
    private String responseObject;
    private String responseMergerClass;
    private String errorResponseObject;
    private String[] description;

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    public boolean isRequestCachingEnabled() {
        return requestCachingEnabled;
    }

    public void setRequestCachingEnabled(boolean requestCachingEnabled) {
        this.requestCachingEnabled = requestCachingEnabled;
    }

    public String getRequestObject() {
        return requestObject;
    }

    public void setRequestObject(String requestObject) {
        this.requestObject = requestObject;
    }

    public String getResponseObject() {
        return responseObject;
    }

    public void setResponseObject(String responseObject) {
        this.responseObject = responseObject;
    }

    public String getErrorResponseObject() {
        return errorResponseObject;
    }

    public void setErrorResponseObject(String errorResponseObject) {
        this.errorResponseObject = errorResponseObject;
    }

    public String[] getDescription() {
        return description;
    }

    public void setDescription(String[] description) {
        this.description = description;
    }

    public String getRequestSplitterClass() { return requestSplitterClass; }

    public void setRequestSplitterClass(String requestSplitterClass) { this.requestSplitterClass = requestSplitterClass; }

    public String getRequestParamWithLimit() {
        return requestParamWithLimit;
    }

    public void setRequestParamWithLimit(String requestParamWithLimit) {
        this.requestParamWithLimit = requestParamWithLimit;
    }

    public String getResponseMergerClass() {
        return responseMergerClass;
    }

    public void setResponseMergerClass(String responseMergerClass) {
        this.responseMergerClass = responseMergerClass;
    }

    public boolean isDynamicCommandName() {
        return dynamicCommandName;
    }

    public void setDynamicCommandName(boolean dynamicCommandName) {
        this.dynamicCommandName = dynamicCommandName;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof EndPoint)) {
            return false;
        }
        if (this == object) {
            return true;
        }

        EndPoint endPoint = (EndPoint) object;
        if (!Objects.equals(httpMethod, endPoint.getHttpMethod())) {
            return false;
        }
        if (!Objects.equals(uri, endPoint.getUri())) {
            return false;
        }
        if (!Objects.equals(commandName, endPoint.getCommandName())) {
            return false;
        }
        if (!Objects.equals(dynamicCommandName, endPoint.isDynamicCommandName())) {
            return false;
        }
        if (!Objects.equals(headers, endPoint.getHeaders())) {
            return false;
        }
        if (!Arrays.equals(parameters, endPoint.getParameters())) {
            return false;
        }
        if (!Objects.equals(requestObject, endPoint.getRequestObject())) {
            return false;
        }
        if (!Objects.equals(responseObject, endPoint.getResponseObject())) {
            return false;
        }
        if (!Arrays.equals(description, endPoint.getDescription())) {
            return false;
        }
        return true;
    }
}
