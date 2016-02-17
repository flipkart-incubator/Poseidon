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
import com.flipkart.poseidon.helpers.ObjectMapperHelper;
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
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static com.flipkart.poseidon.constants.RequestConstants.*;
import static com.flipkart.poseidon.helpers.ObjectMapperHelper.getMapper;
import static javax.servlet.http.HttpServletResponse.*;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpMethod.*;

public class PoseidonServlet extends HttpServlet {

    private static final Logger logger = getLogger(PoseidonServlet.class);
    private final Application application;
    private final ExecutorService datasourceTPE;
    private final ExecutorService filterTPE;
    private final Configuration configuration;

    public PoseidonServlet(Application application, Configuration configuration, ExecutorService datasourceTPE, ExecutorService filterTPE) {
        this.application = application;
        this.configuration = configuration;
        this.datasourceTPE = datasourceTPE;
        this.filterTPE = filterTPE;

        application.init(datasourceTPE, filterTPE);
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

    protected void doRequest(HttpMethod method, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        setRequestContext(httpRequest);

        PoseidonRequest request = new PoseidonRequest(httpRequest);
        request.setAttribute(METHOD, method);

        if (ServletFileUpload.isMultipartContent(httpRequest)) {
            handleFileUpload(request, httpRequest);
        } else {
            StringBuffer requestBuffer = new StringBuffer();
            String line = null;
            try {
                BufferedReader reader = httpRequest.getReader();
                while ((line = reader.readLine()) != null)
                    requestBuffer.append(line);
            } catch (Exception e) {
                logger.warn("301: Couldn't read body" + e.getMessage());
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
            logger.error("400: {}", exception);
            badRequest(httpResponse, exception);
        } catch (ElementNotFoundException exception) {
            logger.error("404: {}", exception);
            elementNotFound(httpResponse, exception);
        } catch (Throwable exception) {
            logger.error("500: {}", exception);
            internalError(httpResponse, exception);
        }
    }

    private void setRequestContext(HttpServletRequest httpServletRequest) {
        RequestContext.set(METHOD, httpServletRequest.getMethod());
        RequestContext.set(REQUEST_ID, getRequestId(httpServletRequest));
        RequestContext.set(IS_PERF_TEST, isPerfTest(httpServletRequest));
    }

    private String getRequestId(HttpServletRequest httpServletRequest) {
        if (httpServletRequest.getHeader(REQUEST_ID_HEADER) != null) {
            return httpServletRequest.getHeader(REQUEST_ID_HEADER);
        }
        return UUID.randomUUID().toString();
    }

    private boolean isPerfTest(HttpServletRequest httpServletRequest) {
        return Boolean.parseBoolean(httpServletRequest.getHeader(PERF_TEST_HEADER));
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
        String responseStr = "";
        if (responseObj != null) {
            if (responseObj instanceof String) {
                responseStr = (String) responseObj;
            } else {
                responseStr = getMapper().writeValueAsString(responseObj);
            }
        }
        httpResponse.getWriter().println(responseStr);
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
    }

    private void setCookies(PoseidonResponse response, HttpServletResponse httpResponse) {
        Map<String, Cookie> cookies = response.getCookies();
        for (Cookie cookie : cookies.values()) {
            httpResponse.addCookie(cookie);
        }
    }

    private void badRequest(HttpServletResponse httpServletResponse, Exception exception) throws IOException {
        processErrorResponse(SC_BAD_REQUEST, httpServletResponse, exception);
    }

    private void elementNotFound(HttpServletResponse httpResponse, Exception exception) throws IOException {
        processErrorResponse(SC_NOT_FOUND, httpResponse, exception);
    }

    private void internalError(HttpServletResponse httpResponse, Throwable throwable) throws IOException {
        processErrorResponse(SC_INTERNAL_SERVER_ERROR, httpResponse, throwable);
    }

    private void processErrorResponse(int statusCode, HttpServletResponse httpResponse, Throwable throwable) throws IOException {
        Throwable generatedException = Optional.ofNullable(ExceptionUtils.getRootCause(throwable)).orElse(throwable);
        if (configuration.getExceptionMapper() == null || !configuration.getExceptionMapper().map(generatedException, httpResponse)) {
            MediaType contentType = application.getDefaultMediaType();
            String errorMsg = "";

            if (generatedException != null && generatedException instanceof DataSourceException) {
                DataSourceException dsException = (DataSourceException) generatedException;
                if (dsException.getResponse() != null) {
                    errorMsg = ObjectMapperHelper.getMapper().writeValueAsString(dsException.getResponse());
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
