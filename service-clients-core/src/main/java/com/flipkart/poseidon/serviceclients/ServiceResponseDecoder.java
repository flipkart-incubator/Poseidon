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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mohan.pandian on 18/03/15.
 * <p/>
 * Decodes the response returned by back-end HTTP services to Type T
 */
public class ServiceResponseDecoder<T> implements HttpResponseDecoder<ServiceResponse<T>> {
    private final ObjectMapper objectMapper;
    private final JavaType javaType;
    private final Logger logger;
    private final Map<String, Class<? extends ServiceClientException>> exceptions;

    public ServiceResponseDecoder(ObjectMapper objectMapper, JavaType javaType, Logger logger, Map<String, Class<? extends ServiceClientException>> exceptions) {
        this.objectMapper = objectMapper;
        this.javaType = javaType;
        this.logger = logger;
        this.exceptions = exceptions;
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
        }
        return headers;
    }

    @Override
    public ServiceResponse<T> decode(HttpResponse httpResponse) throws Exception {
        Map<String, String> headers = getHeaders(httpResponse);
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
                        logger.error("Error de-serializing response object", e);
                        throw new IOException("Response object de-serialization error", e);
                    }
                } catch (Exception e) {
                    logger.error("Error de-serializing response object", e);
                    throw new IOException("Response object de-serialization error", e);
                }
            }
        } else {
            try {
                String serviceResponse = StringUtils.convertStreamToString(httpResponse.getEntity().getContent());
                logger.warn("Non 200 response {}", serviceResponse);
                Class<? extends ServiceClientException> exceptionClass;
                if (exceptions.containsKey(statusCodeString))
                    exceptionClass = exceptions.get(statusCodeString);
                else
                    exceptionClass = exceptions.get("default");

                return new ServiceResponse<T>(exceptionClass.getConstructor(String.class).newInstance(serviceResponse), headers);

            } catch (Exception e) {
                logger.error("Error de-serializing non 200 response", e);
                throw new IOException("Non 200 response de-serialization error", e);
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
    public ServiceResponse<T> decode(InputStream is) {
        throw new UnsupportedOperationException();
    }
}
