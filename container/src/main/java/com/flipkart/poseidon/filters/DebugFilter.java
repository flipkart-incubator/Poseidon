/*
 * Copyright 2017 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon.filters;

import com.flipkart.poseidon.helpers.ObjectMapperHelper;
import com.flipkart.poseidon.model.trace.DebugAPI;
import com.flipkart.poseidon.model.trace.ServiceCallDebug;
import com.flipkart.poseidon.serviceclients.ServiceClientException;
import com.flipkart.poseidon.serviceclients.ServiceContext;
import com.flipkart.poseidon.serviceclients.ServiceDebug;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by shrey.garg on 30/05/17.
 */
public class DebugFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest;
        ServletResponse response = servletResponse;

        if (servletRequest instanceof HttpServletRequest) {
            httpServletRequest = (HttpServletRequest) servletRequest;

            if (httpServletRequest.getParameter("whatIsWrongWithThis") != null) {
                ServiceContext.enableDebug();
                response = new HttpServletResponseCopier((HttpServletResponse) servletResponse);
            }

            filterChain.doFilter(httpServletRequest, response);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }

        if (ServiceContext.isDebug()) {
            httpServletRequest = (HttpServletRequest) servletRequest;

            String includeOnlyServices = httpServletRequest.getParameter("includeOnly");
            boolean disableServiceResponses = httpServletRequest.getParameterMap().containsKey("disableServiceResponses");

            Map<String, List<ServiceDebug>> callDebug = filterWantedServices(includeOnlyServices, ServiceContext.getDebugResponses());
            filterDisabledServices(httpServletRequest, callDebug);

            Map<String, List<ServiceCallDebug>> serviceCallDebugMap = new HashMap<>();
            for (Map.Entry<String, List<ServiceDebug>> entry : callDebug.entrySet()) {
                List<ServiceDebug> serviceDebugs = entry.getValue();
                if (serviceDebugs == null) {
                    continue;
                }

                List<ServiceCallDebug> serviceCallDebugs = new ArrayList<>();
                for (ServiceDebug serviceDebug : serviceDebugs) {
                    ServiceCallDebug serviceCallDebug = convertToServiceCallDebug(serviceDebug, disableServiceResponses);
                    serviceCallDebugs.add(serviceCallDebug);
                }

                serviceCallDebugMap.put(entry.getKey(), serviceCallDebugs);
            }

            generateDebugResponse((HttpServletResponseCopier) response, serviceCallDebugMap);
        }
    }

    protected List<String> disabledDebugServices(HttpServletRequest request) {
        return new ArrayList<>();
    }

    private void filterDisabledServices(HttpServletRequest request, Map<String, List<ServiceDebug>> callDebug) {
        List<String> disabledServices = disabledDebugServices(request);
        if (disabledServices.isEmpty()) {
            return;
        }

        disabledServices.forEach(callDebug::remove);
    }

    private void generateDebugResponse(HttpServletResponseCopier responseCopier, Map<String, List<ServiceCallDebug>> serviceCallDebugMap) throws IOException {
        DebugAPI debugAPI = new DebugAPI();
        debugAPI.setServiceCallInfo(serviceCallDebugMap);

        responseCopier.flushBuffer();
        try {
            debugAPI.setApiResponse(ObjectMapperHelper.getMapper().readValue(responseCopier.getCopyAsString(), Map.class));
        } catch (Exception e) {
            debugAPI.setApiResponse(responseCopier.getCopyAsString());
        }

        debugAPI.setApiStatus(responseCopier.getStatus());
        responseCopier.setStatus(200);
        responseCopier.getWriter().write(ObjectMapperHelper.getMapper().writeValueAsString(debugAPI));
        responseCopier.flushBuffer();
    }

    private ServiceCallDebug convertToServiceCallDebug(ServiceDebug serviceDebug, boolean disableServiceResponses) {
        ServiceCallDebug serviceCallDebug = new ServiceCallDebug();
        serviceCallDebug.setHeadersMap(serviceDebug.getProperties().getHeadersMap());
        serviceCallDebug.setHttpMethod(serviceDebug.getProperties().getHttpMethod());
        serviceCallDebug.setRequestObject(serviceDebug.getProperties().getRequestObject());
        serviceCallDebug.setUri(serviceDebug.getProperties().getUri());

        if (disableServiceResponses) {
            return serviceCallDebug;
        }

        Object serviceResponse;
        try {
            serviceResponse = serviceDebug.getResponsePromise().get();
            serviceCallDebug.setResponseHeaders(serviceDebug.getResponsePromise().getHeaders());
        } catch (ServiceClientException e) {
            serviceResponse = e.getErrorResponse();
            serviceCallDebug.setSuccess(false);
            serviceCallDebug.setErrorIdentifier(e.getClass().getName());
            try {
                serviceCallDebug.setResponseHeaders(serviceDebug.getResponsePromise().getHeaders());
            } catch (Exception ignored) {

            }
         } catch (Exception e) {
            serviceResponse = Optional.ofNullable(ExceptionUtils.getRootCause(e)).orElse(e);
            serviceCallDebug.setSuccess(false);
            serviceCallDebug.setErrorIdentifier(e.getClass().getName());
            try {
                serviceCallDebug.setResponseHeaders(serviceDebug.getResponsePromise().getHeaders());
            } catch (Exception ignored) {

            }
        }

        serviceCallDebug.setServiceResponse(serviceResponse);
        return serviceCallDebug;
    }

    private Map<String, List<ServiceDebug>> filterWantedServices(String includeOnlyServices, Map<String, List<ServiceDebug>> callDebug) {
        if (includeOnlyServices != null) {
            List<String> includedServices = Arrays.asList(includeOnlyServices.split(","));
            return callDebug.entrySet().stream().filter(e -> includedServices.contains(e.getKey())).collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
            ));
        }
        return callDebug;
    }

    @Override
    public void destroy() {

    }

    class HttpServletResponseCopier extends HttpServletResponseWrapper {

        private ServletOutputStream outputStream;
        private PrintWriter writer;
        private ServletOutputStreamCopier copier;

        public HttpServletResponseCopier(HttpServletResponse response) throws IOException {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (writer != null) {
                throw new IllegalStateException("getWriter() has already been called on this response.");
            }

            if (outputStream == null) {
                outputStream = getResponse().getOutputStream();
                copier = new ServletOutputStreamCopier(outputStream);
            }

            return copier;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (outputStream != null) {
                throw new IllegalStateException("getOutputStream() has already been called on this response.");
            }

            if (writer == null) {
                copier = new ServletOutputStreamCopier(new ByteArrayOutputStream());
            } else {
                copier = new ServletOutputStreamCopier(getResponse().getOutputStream());
            }

            writer = new PrintWriter(new OutputStreamWriter(copier, getResponse().getCharacterEncoding()), false);

            return writer;
        }

        @Override
        public void flushBuffer() throws IOException {
            if (writer != null) {
                writer.flush();
            } else if (outputStream != null) {
                copier.flush();
            }
        }

        public byte[] getCopy() {
            if (copier != null) {
                return copier.getCopy();
            } else {
                return new byte[0];
            }
        }

        public String getCopyAsString() throws IOException {
            return new String(getCopy(), getCharacterEncoding());
        }
    }

    class ServletOutputStreamCopier extends ServletOutputStream {

        private OutputStream outputStream;
        private ByteArrayOutputStream copy;

        public ServletOutputStreamCopier(OutputStream outputStream) {
            this.outputStream = outputStream;
            this.copy = new ByteArrayOutputStream(1024);
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
            copy.write(b);
        }

        public byte[] getCopy() {
            return copy.toByteArray();
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }
    }
}
