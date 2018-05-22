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

package com.flipkart.poseidon.serviceclients;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.poseidon.handlers.http.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ServiceResponseDecoder.class, StringUtils.class, IOUtils.class, JavaType.class})
public class ServiceResponseDecoderTest {
    ObjectMapper mockMapper = spy(new ObjectMapper());
    JavaType mockJavaType = mock(JavaType.class);
    JavaType mockErrorType = mockMapper.getTypeFactory().constructType(new TypeReference<TestErrorResponse>() {
    });
    Map<String, Queue<String>> collectedHeaders;

    Class responseClass = String.class;
    Class errorClass = TestErrorResponse.class;
    Logger mockLogger;
    Map<String, Class<? extends ServiceClientException>> exceptions = new HashMap<>();

    @Rule
    ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        mockLogger = mock(Logger.class);
        collectedHeaders = new HashMap<>();
    }

    /**
     *  Success case - in case of 200
     * @throws Exception
     */
    @Test
    public void testDecodeHttpResponse() throws Exception {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        InputStream stream = mock(InputStream.class);
        mockStatic(IOUtils.class);

        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(stream);
        BDDMockito.when(IOUtils.toString(stream)).thenReturn("success");
        when(mockJavaType.getRawClass()).thenReturn(responseClass);

        ServiceResponseDecoder decoder = spy(new ServiceResponseDecoder(mockMapper, mockJavaType, mockErrorType, mockLogger , exceptions, collectedHeaders));
        ServiceResponse response = decoder.decode(mockHttpResponse);
        assertEquals("success", response.getDataList().get(0));
        Mockito.verify(mockLogger, Mockito.never());
    }

    /**
     *  service returned 200, but decode fails
     * @throws Exception
     */
    @Test
    public void testDecodeHttpResponseExceptionWhileDecode() throws Exception {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        InputStream stream = mock(InputStream.class);

        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(stream);
        Mockito.doThrow(IOException.class).when(mockMapper).readValue(stream, responseClass);

        exception.expect(IOException.class);
        exception.expectMessage(equalTo("Response object de-serialization error"));

        ServiceResponseDecoder decoder = spy(new ServiceResponseDecoder(mockMapper, mockJavaType, mockErrorType, mockLogger , exceptions, collectedHeaders));
        ServiceResponse response = decoder.decode(mockHttpResponse);
        assertNotNull(response);

    }

    /**
     *  Service returned non 200, but returned known exception
     * @throws Exception
     */
    @Test
    public void testDecodeHttpResponseReturnException() throws Exception {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        InputStream stream = mock(InputStream.class);
        String errorString = "{\"error\":\"testing error\"}";

        Map<String, Class<? extends ServiceClientException>> mockExceptions = new HashMap<>();
        mockExceptions.put("404", ServiceClientException.class);
        ServiceResponseDecoder decoder = spy(new ServiceResponseDecoder(mockMapper, mockJavaType, mockErrorType, mockLogger, mockExceptions, new HashMap<>()));

        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(stream);
        mockStatic(StringUtils.class);
        when(StringUtils.convertStreamToString(stream)).thenReturn(errorString);

        ServiceResponse response = decoder.decode(mockHttpResponse);
        assertThat(response.getException(), instanceOf(ServiceClientException.class));
        assertThat(response.getException().getErrorResponse(), instanceOf(mockErrorType.getRawClass()));
        Mockito.verify(mockLogger).debug("Non 200 response statusCode: {} response: {}", "404", errorString);
    }

    /**
     *  Service returned non 200, but returned known exception, with no errorType
     * @throws Exception
     */
    @Test
    public void testDecodeHttpResponseReturnExceptionNoErrorType() throws Exception {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        InputStream stream = mock(InputStream.class);
        String errorString = "{\"error\":\"testing error\"}";

        Map<String, Class<? extends ServiceClientException>> mockExceptions = new HashMap<>();
        mockExceptions.put("404", ServiceClientException.class);
        ServiceResponseDecoder decoder = spy(new ServiceResponseDecoder(mockMapper, mockJavaType, null, mockLogger, mockExceptions, new HashMap<>()));

        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(stream);
        mockStatic(StringUtils.class);
        when(StringUtils.convertStreamToString(stream)).thenReturn(errorString);

        ServiceResponse response = decoder.decode(mockHttpResponse);
        assertThat(response.getException(), instanceOf(ServiceClientException.class));
        assertEquals(response.getException().getErrorResponse(), null);
        Mockito.verify(mockLogger).debug("Non 200 response statusCode: {} response: {}", "404", errorString);
    }

    /**
     *  Service returned non 200, but returned known exception, with invalid errorType
     * @throws Exception
     */
    @Test
    public void testDecodeHttpResponseReturnExceptionInvalidErrorType() throws Exception {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        InputStream stream = mock(InputStream.class);
        String errorString = "{\"nonsense\":\"testing error\"}";

        Map<String, Class<? extends ServiceClientException>> mockExceptions = new HashMap<>();
        mockExceptions.put("404", ServiceClientException.class);
        ServiceResponseDecoder decoder = spy(new ServiceResponseDecoder(mockMapper, mockJavaType, mockErrorType, mockLogger, mockExceptions, new HashMap<>()));

        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(stream);
        mockStatic(StringUtils.class);
        when(StringUtils.convertStreamToString(stream)).thenReturn(errorString);

        ServiceResponse response = decoder.decode(mockHttpResponse);
        assertThat(response.getException(), instanceOf(ServiceClientException.class));
        assertEquals(response.getException().getErrorResponse(), null);
        Mockito.verify(mockLogger).debug("Non 200 response statusCode: {} response: {}", "404", errorString);
        Mockito.verify(mockLogger).warn(anyString(), anyString(), any(Object.class));
    }

    /**
     *  Service returned unknown status line(not in expected statusLines)
     * @throws Exception
     */
    @Test
    public void testDecodeHttpResponseDefaultResponse() throws Exception {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        InputStream stream = mock(InputStream.class);

        when(mockStatusLine.getStatusCode()).thenReturn(500);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(stream);
        mockStatic(StringUtils.class);
        when(StringUtils.convertStreamToString(stream)).thenReturn("error");
        exceptions.put("default", ServiceClientException.class);

        try {
            ServiceResponseDecoder decoder = spy(new ServiceResponseDecoder(mockMapper, mockJavaType, mockErrorType, mockLogger , exceptions, collectedHeaders));
            ServiceResponse response = decoder.decode(mockHttpResponse);
            fail("Decoder should throw exception for 5xx status, but didn't!");
        } catch(Exception e) {
            assertThat(e, instanceOf(ServiceClientException.class));
        }
        Mockito.verify(mockLogger).error("Non 200 response statusCode: {} response: {}","500", "error");

    }

    /**
     *  Testing ServiceResponseDecoder with multiple configured ResponseClasses (Old Constructor)
     * @throws Exception
     */
    @Test
    public void testDecoderMultipleExceptionClassesOldConstructor() throws Exception {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        InputStream stream = mock(InputStream.class);

        Map<String, Class<? extends ServiceClientException>> mockExceptions = new HashMap<>();
        mockExceptions.put("400", ServiceClientExceptionTest.class);
        mockExceptions.put("default", ServiceClientException.class);
        ServiceResponseDecoder decoder = spy(new ServiceResponseDecoder(mockMapper, mockJavaType, mockErrorType, mockLogger , mockExceptions, collectedHeaders));

        when(mockStatusLine.getStatusCode()).thenReturn(500);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(stream);
        mockStatic(StringUtils.class);
        when(StringUtils.convertStreamToString(stream)).thenReturn("error");

        try {
            decoder.decode(mockHttpResponse);
            fail("Decoder should throw exception for 5xx status, but didn't!");
        } catch(Exception e) {
            assertThat(e, instanceOf(ServiceClientException.class));
        }

        when(mockStatusLine.getStatusCode()).thenReturn(400);

        ServiceResponse response = decoder.decode(mockHttpResponse);
        assertEquals(response.getException().getClass(), ServiceClientExceptionTest.class);
        assertNull(response.getException().getErrorResponse());
        Mockito.verify(mockLogger).error("Non 200 response statusCode: {} response: {}","500", "error");

    }


    @Test
    public void testHeaders() throws Exception {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        InputStream stream = mock(InputStream.class);
        mockStatic(IOUtils.class);

        Header headerOne = new BasicHeader("one", "1");
        Header headerTwo = new BasicHeader("two", "2");
        Header[] responseHeaders = new Header[] { headerOne, headerTwo };

        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
        when(mockHttpResponse.getAllHeaders()).thenReturn(responseHeaders);
        when(mockEntity.getContent()).thenReturn(stream);
        BDDMockito.when(IOUtils.toString(stream)).thenReturn("success");
        when(mockJavaType.getRawClass()).thenReturn(responseClass);

        ServiceResponseDecoder decoder = spy(new ServiceResponseDecoder(mockMapper, mockJavaType, mockErrorType, mockLogger , exceptions, collectedHeaders));
        ServiceResponse response = decoder.decode(mockHttpResponse);
        assertEquals("success", response.getDataList().get(0));
        assertEquals(2, response.getHeaders().size());
        assertEquals("1", response.getHeaders().get("one"));
        assertEquals("2", response.getHeaders().get("two"));
        assertEquals(0, collectedHeaders.size());
        Mockito.verify(mockLogger, Mockito.never());
    }

    @Test
    public void testCollectedHeaders() throws Exception {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        InputStream stream = mock(InputStream.class);
        mockStatic(IOUtils.class);

        collectedHeaders.put("one", new ConcurrentLinkedQueue<>());

        Header headerOne = new BasicHeader("one", "1");
        Header headerOneAgain = new BasicHeader("one", "3");
        Header headerTwo = new BasicHeader("two", "2");
        Header[] responseHeaders = new Header[] { headerOne, headerTwo, headerOneAgain };

        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
        when(mockHttpResponse.getAllHeaders()).thenReturn(responseHeaders);
        when(mockEntity.getContent()).thenReturn(stream);
        BDDMockito.when(IOUtils.toString(stream)).thenReturn("success");
        when(mockJavaType.getRawClass()).thenReturn(responseClass);

        ServiceResponseDecoder decoder = spy(new ServiceResponseDecoder(mockMapper, mockJavaType, mockErrorType, mockLogger , exceptions, collectedHeaders));
        ServiceResponse response = decoder.decode(mockHttpResponse);
        assertEquals("success", response.getDataList().get(0));
        assertEquals(2, response.getHeaders().size());
        assertEquals("3", response.getHeaders().get("one"));
        assertEquals("2", response.getHeaders().get("two"));
        assertEquals(1, collectedHeaders.size());
        assertEquals(2, collectedHeaders.get("one").size());
        assertTrue(collectedHeaders.get("one").contains("1"));
        assertTrue(collectedHeaders.get("one").contains("3"));
        Mockito.verify(mockLogger, Mockito.never());

    }

    private static class TestErrorResponse {
        private String error;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

}