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

package com.flipkart.poseidon.log4j.pattern;

import com.flipkart.poseidon.log4j.message.IAccessLog;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.PatternConverter;

/**
 * Log4J plugin to convert keys in patterns. Converts requestHeader key in pattern.
 * If a header name is passed within curly braces, only that header value will be
 * logged. Else all the headers will be logged. Header names are case insensitive.
 * <p/>
 * Ex: %requestHeader{User-Agent} and %requestHeader{user-agent} will log User-Agent header.
 * %requestHeader will log all request headers in map format {h1: v1, h2: v2 etc}
 * <p/>
 * Created by mohan.pandian on 31/08/16.
 */
@Plugin(name = "RequestHeaderPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"requestHeader"})
public class RequestHeaderPatternConverter extends AccessLogPatternConverter {
    private String headerName;

    public RequestHeaderPatternConverter(String[] options) {
        if (options != null && options.length > 0 && options[0] != null) {
            headerName = options[0];
        }
    }

    public static RequestHeaderPatternConverter newInstance(final String[] options) {
        return new RequestHeaderPatternConverter(options);
    }

    @Override
    protected void format(IAccessLog accessLog, StringBuilder stringBuilder) {
        if (headerName != null && !headerName.isEmpty()) {
            stringBuilder.append(accessLog.getRequestHeader(headerName));
        } else {
            stringBuilder.append(accessLog.getRequestHeaders());
        }
    }
}
