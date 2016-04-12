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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.phantom.task.impl.TaskContextFactory;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.TaskResult;
import com.google.common.base.Joiner;
import flipkart.lego.api.entities.ServiceClient;
import flipkart.lego.api.exceptions.LegoServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static com.flipkart.poseidon.serviceclients.ServiceClientConstants.HEADERS;

/**
 * Created by mohan.pandian on 24/02/15.
 *
 * Generated service client implementations will extend this abstract class
 */
public abstract class AbstractServiceClient implements ServiceClient {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected Map<String, Class<? extends ServiceClientException>> exceptions = new HashMap<>();

    protected abstract String getCommandName();

    protected final <T> FutureTaskResultToDomainObjectPromiseWrapper<T> execute(JavaType javaType, String uri, String httpMethod, Map<String, String> headersMap, Object requestObject) throws IOException {
        return execute(new ServiceExecutePropertiesBuilder().setJavaType(javaType).setUri(uri).setHttpMethod(httpMethod).setHeadersMap(headersMap).setRequestObject(requestObject).setRequestCachingEnabled(false).build());
    }

    protected final <T> FutureTaskResultToDomainObjectPromiseWrapper<T> execute(JavaType javaType, String uri, String httpMethod,
                                  Map<String, String> headersMap, Object requestObject, String commandName) throws IOException {
        return execute(new ServiceExecutePropertiesBuilder().setJavaType(javaType).setUri(uri).setHttpMethod(httpMethod).setHeadersMap(headersMap).setRequestObject(requestObject).setCommandName(commandName).setRequestCachingEnabled(false).build());
    }

    protected final <T> FutureTaskResultToDomainObjectPromiseWrapper<T> execute(JavaType javaType, String uri, String httpMethod, Map<String, String> headersMap, Object requestObject, String commandName, boolean requestCachingEnabled) throws IOException {
        return execute(new ServiceExecutePropertiesBuilder().setJavaType(javaType).setUri(uri).setHttpMethod(httpMethod).setHeadersMap(headersMap).setRequestObject(requestObject).setCommandName(commandName).setRequestCachingEnabled(requestCachingEnabled).build());
    }

    protected final <T> FutureTaskResultToDomainObjectPromiseWrapper<T> execute(ServiceExecuteProperties properties) throws IOException {
        Logger logger = LoggerFactory.getLogger(getClass());

        String commandName = properties.getCommandName();
        String uri = properties.getUri();
        String httpMethod = properties.getHttpMethod();
        boolean requestCachingEnabled = properties.isRequestCachingEnabled();
        Map<String, String> headersMap = properties.getHeadersMap();
        Object requestObject = properties.getRequestObject();
        JavaType javaType = properties.getJavaType();
        JavaType errorType = properties.getErrorType();

        if(commandName == null || commandName.isEmpty()) {
            commandName = getCommandName();
        }
        logger.info("Executing {} with {} {}", commandName, httpMethod, uri);

        Map<String, String> params = new HashMap<>();
        params.put("uri", uri);
        params.put("method", httpMethod);
        if (requestCachingEnabled) {
            params.put("X-Cache-Request", "true");
        }

        Map<String, String> injectedHeadersMap = injectHeaders(headersMap);
        if (!injectedHeadersMap.isEmpty()) {
            try {
                params.put("headers", objectMapper.writeValueAsString(injectedHeadersMap));
            } catch (Exception e) {
                logger.error("Error serializing headers", e);
                throw new IOException("Headers serialization error", e);
            }
        }

        byte[] payload = null;
        if (requestObject != null) {
            try {
                if (requestObject instanceof String)
                    payload = ((String) requestObject).getBytes();
                else
                    payload = objectMapper.writeValueAsBytes(requestObject);
            } catch (Exception e) {
                logger.error("Error serializing request object", e);
                throw new IOException("Request object serialization error", e);
            }
        }

        TaskContext taskContext = TaskContextFactory.getTaskContext();
        ServiceResponseDecoder<T> serviceResponseDecoder = new ServiceResponseDecoder<>(objectMapper, javaType, errorType, logger, exceptions);
        Future<TaskResult> future = taskContext.executeAsyncCommand(commandName, payload,
                params, serviceResponseDecoder);
        return new FutureTaskResultToDomainObjectPromiseWrapper<>(future);
    }

    /**
     * Injects request id, perf test headers from RequestContext
     *
     * @param headersMap Original headers map sent from service client
     * @return Injected headers map
     */
    protected Map<String, String> injectHeaders(Map<String, String> headersMap) {
        Map<String, String> injectedHeadersMap = new HashMap<>();
        if (headersMap != null && !headersMap.isEmpty()) {
            injectedHeadersMap.putAll(headersMap);
        }

        // Inject Configured Headers
        if (ServiceContext.get(HEADERS) instanceof Map) {
            Map<String, String> configuredHeaders = (Map<String, String>) ServiceContext.get(HEADERS);
            injectedHeadersMap.putAll(configuredHeaders);
        }

        return injectedHeadersMap;
    }

    protected String encodeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        try {
            // Reverting back to ~ is available in existing mobile-api cp-service-client URLHelper.java. Keeping it...
            return URLEncoder.encode(url, "UTF-8").replaceAll("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            LoggerFactory.getLogger(getClass()).error("Exception while encoding URL: " + url, e);
            return url;
        }
    }

    protected String getOptURI(String paramName, Object paramValue) {
        if (paramValue == null || paramValue instanceof String && paramValue.toString().isEmpty()) {
            return "";
        } else {
            return paramName + "=" + paramValue;
        }
    }

    protected String getQueryURI(List<String> params) {
        StringBuilder queryURI = new StringBuilder();
        Boolean first = true;
        for(String param: params) {
            if(param == null || param.isEmpty()) continue;
            if(first) {
                queryURI.append("?");
                first = false;
            } else {
                queryURI.append("&");
            }
            queryURI.append(param);
        }
        return queryURI.toString();
    }

    public JavaType getJavaType(TypeReference typeReference) {
        return objectMapper.getTypeFactory().constructType(typeReference);
    }

    public JavaType getErrorType(TypeReference typeReference) {
        return objectMapper.getTypeFactory().constructType(typeReference);
    }

    public <T> JavaType getJavaType(Class<T> clazz) {
        return objectMapper.getTypeFactory().constructType(clazz);
    }

    public <T> JavaType getErrorType(Class<T> clazz) {
        return objectMapper.getTypeFactory().constructType(clazz);
    }

    @Override
    public void init() throws LegoServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutDown() throws LegoServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() throws UnsupportedOperationException {
       return getName() + "_" + Joiner.on(".").join(getVersion());
    }
}
