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

package com.flipkart.poseidon.core;

import com.flipkart.poseidon.api.Application;
import com.flipkart.poseidon.api.Configuration;
import com.flipkart.poseidon.exception.DataSourceException;
import com.google.common.net.MediaType;
import flipkart.lego.api.exceptions.BadRequestException;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import flipkart.lego.api.exceptions.InternalErrorException;
import flipkart.lego.api.exceptions.ProcessingException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.flipkart.poseidon.constants.RequestConstants.METHOD;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

/**
 * Class to handle REST based requests consumer from queue.
 */
public class PoseidonQueueConsumer {

    public static final List SUPPORTED_HTTP_METHODS = Arrays.asList(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE);
    private static final Logger logger = getLogger(PoseidonQueueConsumer.class);
    private final Application application;
    private final Configuration configuration;

    public void consumptionProcessing(AsyncPoseidonRequest request, AsyncPoseidonResponse response) throws Exception {
        if(SUPPORTED_HTTP_METHODS.contains(request.getHttpMethod())) throw new Exception("Unsupported request HttpMethod received. Expected: " + SUPPORTED_HTTP_METHODS.toString() + ", Found: " + request.getHttpMethod());
        switch (request.getHttpMethod()){
            case PUT:
                doPut(request, response);
            case POST:
                doPost(request, response);
            case DELETE:
                doDelete(request, response);
        }
    }

    public PoseidonQueueConsumer(Application application, Configuration configuration) {
        this.application = application;
        this.configuration = configuration;
    }

    protected void doPost(PoseidonRequest request, PoseidonResponse response) throws IOException {
        request.setAttribute(METHOD, POST);
        doRequest(request, response);
    }

    protected void doPut(PoseidonRequest request, PoseidonResponse response) throws IOException {
        request.setAttribute(METHOD, PUT);
        doRequest(request, response);
    }

    protected void doDelete(PoseidonRequest request, PoseidonResponse response) throws IOException {
        request.setAttribute(METHOD, DELETE);
        doRequest(request, response);
    }

    protected void doRequest(PoseidonRequest request, PoseidonResponse response) throws IOException {
        response.setContentType(application.getDefaultMediaType());
        try {
            buildResponse(request, response);
        } catch (BadRequestException exception) {
            badRequest(response, exception);
        } catch (ElementNotFoundException exception) {
            elementNotFound(response, exception);
        } catch (Throwable exception) {
            internalError(response, exception);
        }
    }

    private void buildResponse(PoseidonRequest request, PoseidonResponse poseidonResponse) throws BadRequestException, ElementNotFoundException, InternalErrorException, ProcessingException, IOException {
        application.handleRequest(request, poseidonResponse);
    }

    private void badRequest(PoseidonResponse response, Exception exception) throws IOException {
        processErrorResponse(SC_BAD_REQUEST, response, exception);
    }

    private void elementNotFound(PoseidonResponse response, Exception exception) throws IOException {
        processErrorResponse(SC_NOT_FOUND, response, exception);
    }

    private void internalError(PoseidonResponse response, Throwable throwable) throws IOException {
        processErrorResponse(SC_INTERNAL_SERVER_ERROR, response, throwable);
    }

    private void processErrorResponse(int statusCode, PoseidonResponse response, Throwable throwable) throws IOException {
        Throwable exception = Optional.ofNullable(ExceptionUtils.getRootCause(throwable)).orElse(throwable);
        logger.error("{}: ", statusCode, exception);
        MediaType contentType = application.getDefaultMediaType();
        String errorMsg = "";
        if (exception != null && exception instanceof DataSourceException) {
            if (((DataSourceException) exception).getResponse() != null) {
                errorMsg = configuration.getObjectMapper().writeValueAsString(
                    ((DataSourceException) exception).getResponse());
            }
            if (((DataSourceException) exception).getStatusCode() > 0) {
                statusCode = ((DataSourceException) exception).getStatusCode();
            }
        } else {
            errorMsg = throwable.getMessage();
        }
        response.setContentType(contentType);
        response.setStatusCode(statusCode);
        response.setResponse(errorMsg);
    }
}
