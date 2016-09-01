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

import org.apache.logging.log4j.message.Message;

import java.util.Map;

/**
 * AbstractAccessLog implements {@link IAccessLog} and {@link Message}
 * and hence can be passed to Logger.log().
 *
 * Implementing classes should populate the required details
 * like request URL, status code, time elapsed etc. Converters
 * will read these details and form the access log line.
 * <p/>
 * Created by mohan.pandian on 01/09/16.
 */
public abstract class AbstractAccessLog implements IAccessLog, Message {
    protected Map<String, String> requestHeaders;
    protected String remoteHost;
    protected String requestLine;
    protected int statusCode;
    protected long contentLength;
    protected long elapsedTime;

    protected final long timeStamp;

    public AbstractAccessLog() {
        // Response time is marked here.
        timeStamp = System.currentTimeMillis();
    }

    protected final String ensureValue(String value) {
        return (value != null && !value.isEmpty()) ? value : NA;
    }

    @Override
    public final String getRequestHeader(String headerName) {
        String headerValue = requestHeaders.get(headerName.toLowerCase());
        return ensureValue(headerValue);
    }

    @Override
    public final Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    @Override
    public final String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public final String getRequestLine() {
        return requestLine;
    }

    @Override
    public final int getStatusCode() {
        return statusCode;
    }

    @Override
    public final long getContentLength() {
        return contentLength;
    }

    @Override
    public final long getElapsedTime() {
        return elapsedTime;
    }

    @Override
    public final String getFormattedMessage() {
        return null;
    }

    @Override
    public final String getFormat() {
        return null;
    }

    @Override
    public final Object[] getParameters() {
        return null;
    }

    @Override
    public final Throwable getThrowable() {
        return null;
    }
}
