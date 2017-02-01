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
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpMethod;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.flipkart.poseidon.constants.RequestConstants.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpMethod.*;

public class PoseidonServlet extends HttpServlet {

    private static final Logger logger = getLogger(PoseidonServlet.class);
    private final Application application;
    private final Configuration configuration;

    public PoseidonServlet(Application application, Configuration configuration) {
        this.application = application;
        this.configuration = configuration;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(GET, request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(POST, request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(HEAD, request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(PUT, request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(DELETE, request, response);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(OPTIONS, request, response);
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(TRACE, request, response);
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(PATCH, request, response);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String method = request.getMethod();
        if ("PATCH".equals(method)) {
            doPatch(request, response);
        } else {
            super.service(request, response);
        }
    }

    protected void doRequest(HttpMethod method, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        PoseidonRequest request = new PoseidonRequest(httpRequest);
        request.setAttribute(METHOD, method);

        if (ServletFileUpload.isMultipartContent(httpRequest)) {
            handleFileUpload(request, httpRequest);
        } else {
            StringBuffer requestBuffer = new StringBuffer();
            String line;
            try {
                BufferedReader reader = httpRequest.getReader();
                while ((line = reader.readLine()) != null)
                    requestBuffer.append(line);
            } catch (Exception e) {
                logger.debug("301: Couldn't read body" + e.getMessage());
            }
            request.setAttribute(BODY, requestBuffer.toString());
        }

        PoseidonResponse response = new PoseidonResponse();
        response.setContentType(application.getDefaultMediaType());

        try {
            buildResponse(request, response, httpResponse);
        } catch (ProcessingException exception) {
            logger.warn("301: " + exception.getMessage());
            redirect(response, httpResponse);
        } catch (BadRequestException exception) {
            badRequest(response, httpResponse, exception);
        } catch (ElementNotFoundException exception) {
            elementNotFound(response, httpResponse, exception);
        } catch (Throwable exception) {
            internalError(response, httpResponse, exception);
        }
    }

    private void handleFileUpload(PoseidonRequest request, HttpServletRequest httpRequest) throws IOException {
        // If uploaded file size is more than 10KB, will be stored in disk
        DiskFileItemFactory factory = new DiskFileItemFactory();
        File repository = new File(FILE_UPLOAD_TMP_DIR);
        if (repository.exists()) {
            factory.setRepository(repository);
        }

        // Currently we don't impose max file size at container layer. Apps can impose it by checking FileItem
        // Apps also have to delete tmp file explicitly (if at all it went to disk)
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> fileItems = null;
        try {
            fileItems = upload.parseRequest(httpRequest);
        } catch (FileUploadException e) {
            throw new IOException(e);
        }
        for (FileItem fileItem : fileItems) {
            String name = fileItem.getFieldName();
            if (fileItem.isFormField()) {
                request.setAttribute(name, new String[] { fileItem.getString() });
            } else {
                request.setAttribute(name, fileItem);
            }
        }
    }

    private void buildResponse(PoseidonRequest request, PoseidonResponse poseidonResponse, HttpServletResponse httpResponse) throws BadRequestException, ElementNotFoundException, InternalErrorException, ProcessingException, IOException {
        application.handleRequest(request, poseidonResponse);

        setHeaders(poseidonResponse, httpResponse);
        setCookies(poseidonResponse, httpResponse);

        httpResponse.setContentType(poseidonResponse.getContentType().toString());
        int statusCode = poseidonResponse.getStatusCode();
        if (statusCode < 100) {
            statusCode = SC_OK;
        }
        httpResponse.setStatus(statusCode);
        Object responseObj = poseidonResponse.getResponse();
        if (responseObj != null) {
            String responseStr = "";
            if (responseObj instanceof String) {
                responseStr = (String) responseObj;
            } else {
                responseStr = configuration.getObjectMapper().writeValueAsString(responseObj);
            }
            httpResponse.getWriter().println(responseStr);
        }
    }

    private void redirect(PoseidonResponse response, HttpServletResponse httpResponse) {
        setHeaders(response, httpResponse);
        setCookies(response, httpResponse);

        httpResponse.setStatus(SC_MOVED_PERMANENTLY);
        httpResponse.setHeader("Location", response.getAttribute(REDIRECT_URL));
    }

    private void setHeaders(PoseidonResponse response, HttpServletResponse httpResponse) {
        Map<String, String> headers = response.getHeaders();
        for (String key : headers.keySet()) {
            httpResponse.setHeader(key, headers.get(key));
        }

        Map<String, List<String>> multiValueHeaders = response.getMultiValueHeaders();
        for (String key : multiValueHeaders.keySet()) {
            Optional.ofNullable(multiValueHeaders.get(key)).ifPresent(values -> values.forEach(value -> httpResponse.addHeader(key, value)));
        }
    }

    private void setCookies(PoseidonResponse response, HttpServletResponse httpResponse) {
        Map<String, Cookie> cookies = response.getCookies();
        for (Cookie cookie : cookies.values()) {
            httpResponse.addCookie(cookie);
        }
    }

    private void badRequest(PoseidonResponse response, HttpServletResponse httpServletResponse, Exception exception) throws IOException {
        processErrorResponse(SC_BAD_REQUEST, response, httpServletResponse, exception);
    }

    private void elementNotFound(PoseidonResponse response, HttpServletResponse httpResponse, Exception exception) throws IOException {
        processErrorResponse(SC_NOT_FOUND, response, httpResponse, exception);
    }

    private void internalError(PoseidonResponse response, HttpServletResponse httpResponse, Throwable throwable) throws IOException {
        processErrorResponse(SC_INTERNAL_SERVER_ERROR, response, httpResponse, throwable);
    }

    private void processErrorResponse(int statusCode, PoseidonResponse response, HttpServletResponse httpResponse, Throwable throwable) throws IOException {
        setHeaders(response, httpResponse);
        setCookies(response, httpResponse);

        Throwable generatedException = Optional.ofNullable(ExceptionUtils.getRootCause(throwable)).orElse(throwable);
        logger.error("{}: ", statusCode, generatedException);

        if (configuration.getExceptionMapper() == null || !configuration.getExceptionMapper().map(generatedException, httpResponse)) {
            MediaType contentType = application.getDefaultMediaType();
            String errorMsg = "";

            if (generatedException != null && generatedException instanceof DataSourceException) {
                DataSourceException dsException = (DataSourceException) generatedException;
                if (dsException.getResponse() != null) {
                    errorMsg = configuration.getObjectMapper().writeValueAsString(dsException.getResponse());
                }
                if (dsException.getStatusCode() > 0) {
                    statusCode = dsException.getStatusCode();
                }
            } else {
                errorMsg = throwable.getMessage();
            }
            httpResponse.setContentType(contentType.toString());
            httpResponse.setStatus(statusCode);
            httpResponse.getWriter().println(errorMsg);
        }
    }
}
