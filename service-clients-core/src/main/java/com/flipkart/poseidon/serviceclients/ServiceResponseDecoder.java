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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.poseidon.handlers.http.HttpResponseDecoder;
import com.flipkart.poseidon.handlers.http.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by mohan.pandian on 18/03/15.
 * <p/>
 * Decodes the response returned by back-end HTTP services to Type T
 */
public class ServiceResponseDecoder<T> implements HttpResponseDecoder<ServiceResponse<T>> {
    private final ObjectMapper objectMapper;
    private final JavaType javaType;
    private final JavaType errorType;
    private final Logger logger;
    private final Map<String, Class<? extends ServiceClientException>> exceptions;
    private final Map<String, Queue<String>> collectedHeaders;
    private final Map<String, List<String>> localCollectedHeaders = new HashMap<>();

    public ServiceResponseDecoder(ObjectMapper objectMapper, JavaType javaType, JavaType errorType, Logger logger, Map<String, Class<? extends ServiceClientException>> exceptions, Map<String, Queue<String>> collectedHeaders) {
        this.objectMapper = objectMapper;
        this.javaType = javaType;
        this.errorType = errorType;
        this.logger = logger;
        this.exceptions = exceptions;
        this.collectedHeaders = collectedHeaders;
    }

    private Map<String, String> getHeaders(HttpResponse httpResponse) {
        Map<String, String> headers = new HashMap<>();
        HeaderIterator iterator = httpResponse.headerIterator();
        if (iterator == null) {
            return headers;
        }
        while (iterator.hasNext()) {
            Header header = iterator.nextHeader();
            headers.put(header.getName(), header.getValue());
            if (collectedHeaders.isEmpty()) {
                continue;
            }

            String lowerCaseHeader = header.getName().toLowerCase();
            if (collectedHeaders.containsKey(lowerCaseHeader)) {
                localCollectedHeaders.computeIfAbsent(lowerCaseHeader, s -> new ArrayList<>()).add(header.getValue());
            }
        }
        return headers;
    }

    @Override
    public ServiceResponse<T> decode(HttpResponse httpResponse) throws Exception {
        Map<String, String> headers = getHeaders(httpResponse);
        collectedHeaders.forEach((k, v) -> Optional.ofNullable(localCollectedHeaders.get(k)).ifPresent(v::addAll));
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        String statusCodeString = String.valueOf(statusCode);
        if (statusCode >= 200 && statusCode <= 299) {
            if (statusCode == 204) {
                return new ServiceResponse<T>((T) null, headers);
            } else {
                try {
                    // Don't deserialize a plain string response using jackson
                    if (String.class.isAssignableFrom(javaType.getRawClass())) {
                        return new ServiceResponse<T>((T) IOUtils.toString(httpResponse.getEntity().getContent()), headers);
                    }
                    return new ServiceResponse<T>(objectMapper.<T>readValue(httpResponse.getEntity().getContent(), javaType), headers);
                } catch (JsonMappingException e) {
                    if (e.getMessage().contains("No content to map due to end-of-input")) {
                        return new ServiceResponse<T>((T) null, headers);
                    } else {
                        logger.error("Error de-serializing response object exception: {}", e.getMessage());
                        throw new IOException("Response object de-serialization error", e);
                    }
                } catch (Exception e) {
                    logger.error("Error de-serializing response object exception: {}", e.getMessage());
                    throw new IOException("Response object de-serialization error", e);
                }
            }
        } else {
            String serviceResponse = StringUtils.convertStreamToString(httpResponse.getEntity().getContent());
            Object errorResponse = null;
            if (errorType != null) {
                try {
                    errorResponse = objectMapper.readValue(serviceResponse, errorType);
                } catch (Exception e) {
                    logger.warn("Error while de-serializing non 200 response to given errorType statusCode:{} exception: {}", statusCodeString, e.getMessage());
                }
            }
            Class<? extends ServiceClientException> exceptionClass;
            if (exceptions.containsKey(statusCodeString)) {
                exceptionClass = exceptions.get(statusCodeString);
            } else {
                exceptionClass = exceptions.get("default");
            }

            String exceptionMessage = statusCodeString + " " + serviceResponse;
            ServiceClientException serviceClientException = exceptionClass.getConstructor(String.class, Object.class).newInstance(exceptionMessage, errorResponse);
            if (statusCode >= 500 && statusCode <= 599) {
                // 5xx errors have to be treated as hystrix command failures. Hence throw service client exception.
                logger.error("Non 200 response statusCode: {} response: {}", statusCodeString, serviceResponse);
                throw serviceClientException;
            } else {
                // Rest of non 2xx don't have to be treated as hystrix command failures (ex: validation failure resulting in 400)
                logger.debug("Non 200 response statusCode: {} response: {}", statusCodeString, serviceResponse);
                return new ServiceResponse<T>(serviceClientException, headers);
            }
        }
    }

    @Override
    public ServiceResponse<T> decode(String stringResponse) throws Exception {
        try {
            return new ServiceResponse<T>(objectMapper.<T>readValue(stringResponse, javaType), null);
        } catch (IOException e) {
            logger.error("Error de-serializing response object", e);
            throw new Exception("Response object de-serialization error", e);
        }
    }

    @Override
    public ServiceResponse<T> decode(byte[] byteResponse) throws Exception {
        try {
            return new ServiceResponse<T>(objectMapper.<T>readValue(byteResponse, javaType), null);
        } catch (IOException e) {
            logger.error("Error de-serializing response object", e);
            throw new IOException("Response object de-serialization error", e);
        }
    }

    @Override
    public ServiceResponse<T> decode(InputStream is) throws Exception {
        return new ServiceResponse<T>(objectMapper.<T>readValue(is, javaType), null);
    }
}
