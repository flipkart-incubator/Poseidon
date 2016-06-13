package com.flipkart.poseidon.filters;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by shrey.garg on 13/06/16.
 */
public class RequestReplayFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest;
        if (servletRequest instanceof HttpServletRequest) {
            httpServletRequest = (HttpServletRequest) servletRequest;
            httpServletRequest = new ReplayRequestWrapper(httpServletRequest);

            filterChain.doFilter(httpServletRequest, servletResponse);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {

    }

    private final class ReplayRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] bytes;

        public ReplayRequestWrapper(final HttpServletRequest request) throws IOException {
            super(request);
            bytes = IOUtils.toByteArray(request.getInputStream());
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
