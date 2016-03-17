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

package com.flipkart.poseidon.serviceclients;

import com.fasterxml.jackson.databind.JavaType;

import java.util.Map;

/**
 * Created by shrey.garg on 16/03/16.
 */
public class ServiceExecutePropertiesBuilder {
    private ServiceExecuteProperties instance;

    public ServiceExecutePropertiesBuilder() {
        this.instance = new ServiceExecuteProperties();
    }

    public ServiceExecutePropertiesBuilder setJavaType(JavaType javaType) {
        instance.setJavaType(javaType);
        return this;
    }

    public ServiceExecutePropertiesBuilder setErrorType(JavaType errorType) {
        instance.setErrorType(errorType);
        return this;
    }

    public ServiceExecutePropertiesBuilder setUri(String uri) {
        instance.setUri(uri);
        return this;
    }

    public ServiceExecutePropertiesBuilder setHttpMethod(String httpMethod) {
        instance.setHttpMethod(httpMethod);
        return this;
    }

    public ServiceExecutePropertiesBuilder setHeadersMap(Map<String, String> headersMap) {
        instance.setHeadersMap(headersMap);
        return this;
    }

    public ServiceExecutePropertiesBuilder setRequestObject(Object requestObject) {
        instance.setRequestObject(requestObject);
        return this;
    }

    public ServiceExecutePropertiesBuilder setCommandName(String commandName) {
        instance.setCommandName(commandName);
        return this;
    }

    public ServiceExecutePropertiesBuilder setRequestCachingEnabled(boolean requestCachingEnabled) {
        instance.setRequestCachingEnabled(requestCachingEnabled);
        return this;
    }

    public ServiceExecuteProperties build() {
        return instance;
    }
}
