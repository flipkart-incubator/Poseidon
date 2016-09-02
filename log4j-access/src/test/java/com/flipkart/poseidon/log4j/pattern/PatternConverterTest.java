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

import com.flipkart.poseidon.log4j.message.AbstractAccessLog;
import com.flipkart.poseidon.log4j.message.AccessLog;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.message.SimpleMessage;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.flipkart.poseidon.log4j.message.IAccessLog.NA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Unit tests for all PatternConverters, IAccessLog, Message etc
 *
 * Created by mohan.pandian on 01/09/16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ System.class, AbstractAccessLog.class })
public class PatternConverterTest {
    // Constants
    private static final String METHOD = "GET";
    private static final String URI = "/a/b/c";
    private static final String PROTOCOL = "HTTP/1.1";
    private static final String REMOTE_HOST = "12.34.56.78";
    private static final int STATUS = 200;
    private static final long CONTENT_LENGTH = 1000;
    private static final long REQUEST_TIME = 1472797200;
    private static final long ELAPSED_TIME = 700;
    private static final long RESPONSE_TIME = REQUEST_TIME + ELAPSED_TIME;
    private static final String HEADER_KEY1 = "header1";
    private static final String HEADER_KEY2 = "header2";
    private static final String HEADER_VALUE1 = "value1";
    private static final String HEADER_VALUE2 = "value2";
    private static final String NON_EXISTING_HEADER = "NON_EXISTING_HEADER";

    private static final Map<String, String> HEADERS = new HashMap<String, String>() {{
        put(HEADER_KEY1, HEADER_VALUE1);
        put(HEADER_KEY2, HEADER_VALUE2);
    }};

    // Objects to be mocked
    private Request mockRequest;
    private Response mockResponse;

    // Actual objects to test
    private ContentLengthPatternConverter contentLengthConverter;
    private ElapsedTimePatternConverter elapsedTimeConverter;
    private RemoteHostPatternConverter remoteHostConverter;
    private RequestHeaderPatternConverter requestHeaderConverter;
    private RequestHeaderPatternConverter requestHeaderCaseConverter;
    private RequestHeaderPatternConverter requestHeaderNAConverter;
    private RequestLinePatternConverter requestLineConverter;
    private StatusCodePatternConverter statusCodePatternConverter;
    private Log4jLogEvent logEvent;
    private Log4jLogEvent noLogEvent;

    @Before
    public void setup() {
        // Create mock objects
        mockRequest = mock(Request.class);
        mockResponse = mock(Response.class);
        when(mockRequest.getMethod()).thenReturn(METHOD);
        when(mockRequest.getRequestURI()).thenReturn(URI);
        when(mockRequest.getProtocol()).thenReturn(PROTOCOL);
        when(mockRequest.getRemoteHost()).thenReturn(REMOTE_HOST);
        when(mockResponse.getStatus()).thenReturn(STATUS);
        when(mockResponse.getContentCount()).thenReturn(CONTENT_LENGTH);
        when(mockRequest.getTimeStamp()).thenReturn(REQUEST_TIME);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration((HEADERS.keySet())));
        when(mockRequest.getHeader(anyString())).thenAnswer(invocation -> HEADERS.get(invocation.getArguments()[0]));

        // Static mocks
        mockStatic(System.class);
        when(System.currentTimeMillis()).thenReturn(RESPONSE_TIME);

        // Create actual objects
        contentLengthConverter = ContentLengthPatternConverter.newInstance(null);
        elapsedTimeConverter = ElapsedTimePatternConverter.newInstance(null);
        remoteHostConverter = RemoteHostPatternConverter.newInstance(null);
        requestHeaderConverter = RequestHeaderPatternConverter.newInstance(null);
        requestHeaderCaseConverter = RequestHeaderPatternConverter.newInstance(new String[] {HEADER_KEY1.toUpperCase() });
        requestHeaderNAConverter = RequestHeaderPatternConverter.newInstance(new String[] { NON_EXISTING_HEADER });
        requestLineConverter = RequestLinePatternConverter.newInstance(null);
        statusCodePatternConverter = StatusCodePatternConverter.newInstance(null);
        logEvent = new Log4jLogEvent.Builder().setMessage(new AccessLog(mockRequest, mockResponse)).build();
        noLogEvent = new Log4jLogEvent.Builder().setMessage(new SimpleMessage()).build();
    }

    /**
     * Tests all pattern converters and message/accesslog
     * 1. Basic converter functionality correctness. Ex: ContentLengthPatternConverter should append contentLength from Request
     * 2. Special converter functionality correctness. Ex: Case insensitive request header
     * 3. If a log event doesn't have AccessLog message, no conversion should happen
     */
    @Test
    public void testPatterns() {
        // Basic converter functionality
        testPattern(contentLengthConverter, CONTENT_LENGTH);
        testPattern(elapsedTimeConverter, ELAPSED_TIME);
        testPattern(remoteHostConverter, REMOTE_HOST);
        testPattern(requestHeaderNAConverter, NA);
        testPattern(requestLineConverter, METHOD + ' ' + URI + ' ' + PROTOCOL);
        testPattern(statusCodePatternConverter, STATUS);

        // Special converter functionality
        testPattern(requestHeaderConverter, HEADERS);
        testPattern(requestHeaderCaseConverter, HEADER_VALUE1);

        // No conversion
        testPattern(contentLengthConverter, "", noLogEvent);
        testPattern(requestHeaderConverter, "", noLogEvent);
    }

    /**
     * Passes logEvent to converter and matches the formatting done by converter against the expected result
     *
     * @param converter PatternConverter to test
     * @param expected  Expected output
     */
    private void testPattern(AccessLogPatternConverter converter, Object expected) {
        testPattern(converter, expected, logEvent);
    }

    /**
     * Passes logEvent to converter and matches the formatting done by converter against the expected result
     *
     * @param converter PatternConverter to test
     * @param expected Expected output
     * @param logEvent LogEvent to be given to converter
     */
    private void testPattern(AccessLogPatternConverter converter, Object expected, Log4jLogEvent logEvent) {
        StringBuilder stringBuilder = new StringBuilder();
        converter.format(logEvent, stringBuilder);
        String actual = stringBuilder.toString();
        assertEquals(expected.toString(), actual);
    }

    /**
     * If any of these test fails, highly possible that its a backward incompatible change.
     * Basically existing log4j-access.xml might get broken as they use a particular token
     * which is being changed.
     */
    @Test
    public void testPatternAnnotations() {
        testPatternAnnotation(ContentLengthPatternConverter.class, "contentLength");
        testPatternAnnotation(ElapsedTimePatternConverter.class, "elapsedTime");
        testPatternAnnotation(RemoteHostPatternConverter.class, "remoteHost");
        testPatternAnnotation(RequestHeaderPatternConverter.class, "requestHeader");
        testPatternAnnotation(RequestLinePatternConverter.class, "requestLine");
        testPatternAnnotation(StatusCodePatternConverter.class, "statusCode");
    }

    /**
     * Tests the presence of certain annotations and converter key in a given Converter class
     *
     * @param clazz PatterConverter class to test
     * @param key Converter key to match
     */
    private void testPatternAnnotation(Class<? extends AccessLogPatternConverter> clazz, String key) {
        assertTrue(clazz.isAnnotationPresent(Plugin.class));
        assertTrue(clazz.isAnnotationPresent(ConverterKeys.class));
        assertEquals(key, clazz.getAnnotation(ConverterKeys.class).value()[0]);
    }
}
