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

package com.flipkart.poseidon.handlers.http;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class HttpResponseData
{
    private final int statusCode;

    private final byte[] responseBody;

    private final Map<String,String> responseHeaders;

    public HttpResponseData(@NotNull int responseCode, byte[] responseBody,  Map<String,String> responseHeaders) {
        this.statusCode = responseCode;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders;
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public byte[] getResponseBody()
    {
        return responseBody;
    }

    public Map<String, String> getResponseHeaders()
    {
        return responseHeaders;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(statusCode).append("\n");
        if(responseHeaders != null) {
            for(String key : responseHeaders.keySet()) {
                sb.append(key).append(":").append(responseHeaders.get(key));
            }
        }

        sb.append("\n\n").append(responseBody);

        return sb.toString();
    }
}
