package com.flipkart.poseidon.serviceclients;

import com.fasterxml.jackson.databind.JavaType;

import java.util.Map;

/**
 * Created by shrey.garg on 16/03/16.
 */
public class ServiceExecuteProperties {
    private JavaType javaType;
    private JavaType errorType;
    private String uri;
    private String httpMethod;
    private Map<String, String> headersMap;
    private Object requestObject;
    private String commandName;
    private boolean requestCachingEnabled;

    public JavaType getJavaType() {
        return javaType;
    }

    public void setJavaType(JavaType javaType) {
        this.javaType = javaType;
    }

    public JavaType getErrorType() {
        return errorType;
    }

    public void setErrorType(JavaType errorType) {
        this.errorType = errorType;
    }

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

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public boolean isRequestCachingEnabled() {
        return requestCachingEnabled;
    }

    public void setRequestCachingEnabled(boolean requestCachingEnabled) {
        this.requestCachingEnabled = requestCachingEnabled;
    }
}
