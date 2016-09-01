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

package com.flipkart.poseidon.log4j.message;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import java.util.Enumeration;
import java.util.HashMap;

/**
 * AccessLog extends {@link AbstractAccessLog} and populates the required details
 * like request URL, status code, time elapsed etc from Jetty's request & response
 * <p/>
 * Created by mohan.pandian on 01/09/16.
 */
public class AccessLog extends AbstractAccessLog {
    /**
     * Populates all required details to generate access log
     * like request URL, status code, time elapsed etc from
     * jetty's request and response. This should be done on
     * caller thread (irrespective of sync/asyn logging).
     * If we don't copy  the values from request and response here
     * and do it in converters, async logging won't fetch correct values
     *
     * @param request Jetty's request
     * @param response Jetty's response
     */
    public AccessLog(Request request, Response response) {
        populateRequestHeaders(request);
        remoteHost = ensureValue(request.getRemoteHost());

        populateRequestLine(request);
        statusCode = response.getStatus();
        contentLength = response.getContentCount();
        elapsedTime = timeStamp - request.getTimeStamp();
    }

    /**
     * Populates headers in a case insensitive way.
     *
     * @param request Jetty's Request
     */
    private void populateRequestHeaders(Request request) {
        requestHeaders = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            requestHeaders.put(key.toLowerCase(), ensureValue(request.getHeader(key)));
        }
    }

    /**
     * Populates HTTP Request line (method uri protocol)
     *
     * @param request Jetty's http request
     */
    private void populateRequestLine(Request request) {
        StringBuilder builder = new StringBuilder();
        builder.append(ensureValue(request.getMethod()))
                .append(' ')
                .append(ensureValue(request.getRequestURI()))
                .append(' ')
                .append(ensureValue(request.getProtocol()));
        requestLine = builder.toString();
    }
}
