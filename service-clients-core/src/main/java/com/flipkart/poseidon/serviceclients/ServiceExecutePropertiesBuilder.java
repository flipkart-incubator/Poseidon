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
