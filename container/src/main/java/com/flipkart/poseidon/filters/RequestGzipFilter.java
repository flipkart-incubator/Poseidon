/*
 * Copyright 2016 Flipkart Internet, pvt ltd.
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

import com.google.common.io.ByteStreams;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by shrey.garg on 27/05/16.
 */
public class RequestGzipFilter implements Filter {

    private static final String _PATCH = "PATCH";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(final ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        boolean isGzipped = servletRequest.getHeader(HttpHeaders.CONTENT_ENCODING) != null
                && servletRequest.getHeader(HttpHeaders.CONTENT_ENCODING).contains("gzip");
        boolean requestTypeSupported = HttpMethod.POST.toString().equals(servletRequest.getMethod()) || HttpMethod.PUT.toString().equals(servletRequest.getMethod()) || HttpMethod.PATCH.toString().equals(servletRequest.getMethod());
        if (isGzipped && !requestTypeSupported) {
            throw new IllegalStateException(new StringBuilder()
                    .append(servletRequest.getMethod())
                    .append(" is not supports gzipped body of parameters.")
                    .append(" Only POST requests are currently supported.")
                    .toString()
            );
        }
        if (isGzipped) {
            servletRequest = new GzippedInputStreamWrapper(servletRequest);
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }

    final class GzippedInputStreamWrapper extends HttpServletRequestWrapper {

        private byte[] bytes;

        public GzippedInputStreamWrapper(final HttpServletRequest request) throws IOException {
            super(request);
            try {
                final InputStream in = new GZIPInputStream(request.getInputStream());
                bytes = ByteStreams.toByteArray(in);
            } catch (EOFException e) {
                bytes = new byte[0];
            }
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            final ByteArrayInputStream sourceStream = new ByteArrayInputStream(bytes);
            return new ServletInputStream() {

                public int read() throws IOException {
                    return sourceStream.read();
                }

                public void close() throws IOException {
                    super.close();
                    sourceStream.close();
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
        }
    }
}
