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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.poseidon.handlers.http.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Assert;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ServiceResponseDecoder.class, StringUtils.class, IOUtils.class, JavaType.class})
public class ServiceResponseDecoderTest {

    ServiceResponseDecoder decoder;
    ObjectMapper mockMapper = mock(ObjectMapper.class);
    JavaType mockJavaType = mock(JavaType.class);

    Class responseClass = String.class;
    Logger mockLogger;
    Map<String, Class<? extends ServiceClientException>> exceptions = new HashMap<>();

    @Rule
    ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        mockLogger = mock(Logger.class);
        decoder = spy(new ServiceResponseDecoder(mockMapper, mockJavaType, mockLogger , exceptions));
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

        ServiceResponse response = decoder.decode(mockHttpResponse);
        Assert.assertEquals("success", response.getDataList().get(0));
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

        Map mockExceptions = mock(Map.class);
        decoder = spy(new ServiceResponseDecoder(mockMapper, mockJavaType, mockLogger, mockExceptions));

        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(stream);
        mockStatic(StringUtils.class);
        when(StringUtils.convertStreamToString(stream)).thenReturn("error");
        when(mockExceptions.containsKey("404")).thenReturn(true);
        when(mockExceptions.get("404")).thenReturn(ServiceClientException.class);

        ServiceResponse response = decoder.decode(mockHttpResponse);
        assertThat(response.getException(), instanceOf(ServiceClientException.class));
        Mockito.verify(mockLogger).warn("Non 200 response {}", "error");
    }

    /**
     *  Service returned un known status line(not in expected statusLines)
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

        ServiceResponse response = decoder.decode(mockHttpResponse);
        assertThat(response.getException(), instanceOf(ServiceClientException.class));
        Mockito.verify(mockLogger).warn("Non 200 response {}", "error");
    }

    /**
     *  Service returned un known status line(not in expected statusLines) and decode fails
     * @throws Exception
     */
    @Test
    public void testDecodeHttpResponseDefaultResponseDecodeFail() throws Exception {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        InputStream stream = mock(InputStream.class);

        when(mockStatusLine.getStatusCode()).thenReturn(500);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(stream);
        mockStatic(StringUtils.class);
        when(StringUtils.convertStreamToString(stream)).thenThrow(IOException.class);
        exceptions.put("default", ServiceClientException.class);

        exception.expect(IOException.class);
        exception.expectMessage(equalTo("Non 200 response de-serialization error"));
        ServiceResponse response = decoder.decode(mockHttpResponse);
        Mockito.verify(mockLogger).error("Error de-serializing non 200 response");

    }

}