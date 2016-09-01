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

import java.util.Map;

/**
 * Interface to get details required to generate http access log
 * like request URL, status code, time elapsed etc.
 *
 * Created by mohan.pandian on 01/09/16.
 */
public interface IAccessLog {
    String NA = "-";

    /**
     * @param headerName http request header name
     * @return http request header value
     */
    String getRequestHeader(String headerName);

    /**
     * @return all http request headers
     */
    Map<String, String> getRequestHeaders();

    /**
     * @return http remote host
     */
    String getRemoteHost();

    /**
     * @return HTTP Request line - the first line of request
     * Ex: GET /url HTTP/1.1
     */
    String getRequestLine();

    /**
     * @return HTTP status code
     */
    int getStatusCode();

    /**
     * @return final content length - gzipped content length
     * if compression is enabled
     */
    long getContentLength();

    /**
     * @return total time taken by request
     */
    long getElapsedTime();
}
