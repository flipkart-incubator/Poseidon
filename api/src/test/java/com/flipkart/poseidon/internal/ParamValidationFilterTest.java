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

package com.flipkart.poseidon.internal;

import com.flipkart.poseidon.TestConfiguration;
import com.flipkart.poseidon.TestEnum;
import com.flipkart.poseidon.TestPOJO;
import com.flipkart.poseidon.Wrapper;
import com.flipkart.poseidon.api.APIBuildable;
import com.flipkart.poseidon.api.Configuration;
import com.flipkart.poseidon.constants.RequestConstants;
import com.flipkart.poseidon.core.PoseidonRequest;
import com.flipkart.poseidon.core.PoseidonResponse;
import com.flipkart.poseidon.core.RequestContext;
import com.flipkart.poseidon.model.VariableModel;
import com.flipkart.poseidon.pojos.EndpointPOJO;
import com.flipkart.poseidon.pojos.ParamPOJO;
import com.flipkart.poseidon.pojos.ParamsPOJO;
import flipkart.lego.api.exceptions.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by shrey.garg on 14/07/16.
 */
public class ParamValidationFilterTest {
    private Configuration configuration = new TestConfiguration();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testPathParam() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 4);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz/{test}");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());
        assertEquals("qwe", parsedParams.get("test"));
    }

    @Test(expected = BadRequestException.class)
    public void testPathParamNotFound() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 5);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz/qwe");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());
    }

    @Test
    public void testPathParamMiddle() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 3);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);

        RequestContext.set(RequestConstants.URI, "/3/abc/{test}/qwe");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());
        assertEquals("xyz", parsedParams.get("test"));
    }

    @Test
    public void testPathParamInfiniteTail() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 4);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz/**");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe/yui/ase");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());
        assertEquals("qwe/yui/ase", parsedParams.get("test"));
    }

    @Test
    public void testPathParamInfiniteTailWithoutGreedyMarker() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 4);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz/{test}");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe/yui/ase");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());
        assertEquals("qwe", parsedParams.get("test"));
    }

    @Test
    public void testPathParamInfiniteTailMiddle() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 4);
        ParamPOJO paramPOJO2 = mockPathParam("test2", 3);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO, paramPOJO2 };
        when(params.getRequired()).thenReturn(paramPOJOs);

        RequestContext.set(RequestConstants.URI, "/3/abc/{test2}/**");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe/yui/ase");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(2, parsedParams.size());
        assertEquals("qwe/yui/ase", parsedParams.get("test"));
        assertEquals("xyz", parsedParams.get("test2"));
    }

    @Test
    public void testPathParamInfiniteTailMiddleWithoutGreedyMarker() throws Exception {
        RequestContext.set(RequestConstants.URI, "/123/{MIDDLE}/543/{TAIL}");
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 4);
        ParamPOJO paramPOJO2 = mockPathParam("test2", 3);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO, paramPOJO2 };
        when(params.getRequired()).thenReturn(paramPOJOs);

        RequestContext.set(RequestConstants.URI, "/3/abc/{test2}/{test}");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe/yui/ase");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(2, parsedParams.size());
        assertEquals("qwe", parsedParams.get("test"));
        assertEquals("xyz", parsedParams.get("test2"));
    }

    @Test
    public void testPathParamGreedyMiddle() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO2 = mockPathParam("test2", 3);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO2 };
        when(params.getRequired()).thenReturn(paramPOJOs);

        RequestContext.set(RequestConstants.URI, "/3/abc/**/def");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe/yui/ase/def");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());
        assertEquals("xyz/qwe/yui/ase", parsedParams.get("test2"));
    }

    @Test
    public void testPathParamGreedyMiddleTail() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 5);
        ParamPOJO paramPOJO2 = mockPathParam("test2", 3);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO, paramPOJO2 };
        when(params.getRequired()).thenReturn(paramPOJOs);

        RequestContext.set(RequestConstants.URI, "/3/abc/**/def/**");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe/def/ase/qjw");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(2, parsedParams.size());
        assertEquals("xyz/qwe", parsedParams.get("test2"));
        assertEquals("ase/qjw", parsedParams.get("test"));
    }

    @Test
    public void testPathParamGreedyMiddleTailMultiple() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 5);
        ParamPOJO paramPOJO2 = mockPathParam("test2", 3);
        ParamPOJO paramPOJO3 = mockPathParam("test3", 8);
        ParamPOJO paramPOJO4 = mockPathParam("test4", 10);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO, paramPOJO2, paramPOJO3, paramPOJO4 };
        when(params.getRequired()).thenReturn(paramPOJOs);

        RequestContext.set(RequestConstants.URI, "/3/abc/**/def/**/gbh/ytr/{test3}/qwe/**");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe/def/ase/qjw/gbh/ytr/thy/qwe/123/567/212");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(4, parsedParams.size());
        assertEquals("xyz/qwe", parsedParams.get("test2"));
        assertEquals("ase/qjw", parsedParams.get("test"));
        assertEquals("thy", parsedParams.get("test3"));
        assertEquals("123/567/212", parsedParams.get("test4"));
    }

    @Test
    public void testQueryParam() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockQueryParam("test", ParamPOJO.DataType.STRING);
        ParamPOJO paramPOJO2 = mockQueryParam("test2", ParamPOJO.DataType.BOOLEAN);
        ParamPOJO paramPOJO3 = mockQueryParam("test3", ParamPOJO.DataType.INTEGER);
        ParamPOJO paramPOJO4 = mockQueryParam("test4", ParamPOJO.DataType.NUMBER);
        ParamPOJO paramPOJO5 = mockQueryParam("test5", ParamPOJO.DataType.LONG);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO, paramPOJO2, paramPOJO3, paramPOJO4, paramPOJO5 };
        when(params.getRequired()).thenReturn(paramPOJOs);

        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("test", new String[] { "works" });
        parameterMap.put("test2", new String[] { "true" });
        parameterMap.put("test3", new String[] { "2132" });
        parameterMap.put("test4", new String[] { "32.532" });
        parameterMap.put("test5", new String[] { "2147483648" });

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz", parameterMap);

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(5, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof String);
        assertEquals("works", test);

        Object test2 = parsedParams.get("test2");
        assertTrue(test2 instanceof Boolean);
        assertEquals(true, test2);

        Object test3 = parsedParams.get("test3");
        assertTrue(test3 instanceof Integer);
        assertEquals(2132, test3);

        Object test4 = parsedParams.get("test4");
        assertTrue(test4 instanceof Double);
        assertEquals(32.532, test4);

        Object test5 = parsedParams.get("test5");
        assertTrue(test5 instanceof Long);
        assertEquals(2147483648L, test5);
    }

    @Test
    public void testHeaderParam() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockHeaderParam("test", ParamPOJO.DataType.STRING);
        ParamPOJO paramPOJO2 = mockHeaderParam("test2", ParamPOJO.DataType.BOOLEAN);
        ParamPOJO paramPOJO3 = mockHeaderParam("test3", ParamPOJO.DataType.INTEGER);
        ParamPOJO paramPOJO4 = mockHeaderParam("test4", ParamPOJO.DataType.NUMBER);
        ParamPOJO paramPOJO5 = mockHeaderParam("test5", ParamPOJO.DataType.LONG);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO, paramPOJO2, paramPOJO3, paramPOJO4, paramPOJO5 };
        when(params.getRequired()).thenReturn(paramPOJOs);

        Map<String, String> headers = new HashMap<>();
        headers.put("test", "works");
        headers.put("test2", "true");
        headers.put("test3", "2132");
        headers.put("test4", "32.532");
        headers.put("test5", "2147483648");

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz", new HashMap<>(), headers);

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(5, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof String);
        assertEquals("works", test);

        Object test2 = parsedParams.get("test2");
        assertTrue(test2 instanceof Boolean);
        assertEquals(true, test2);

        Object test3 = parsedParams.get("test3");
        assertTrue(test3 instanceof Integer);
        assertEquals(2132, test3);

        Object test4 = parsedParams.get("test4");
        assertTrue(test4 instanceof Double);
        assertEquals(32.532, test4);

        Object test5 = parsedParams.get("test5");
        assertTrue(test5 instanceof Long);
        assertEquals(2147483648L, test5);
    }

    @Test
    public void testPathParamTypes() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 3, ParamPOJO.DataType.STRING);
        ParamPOJO paramPOJO2 = mockPathParam("test2", 4, ParamPOJO.DataType.BOOLEAN);
        ParamPOJO paramPOJO3 = mockPathParam("test3", 5, ParamPOJO.DataType.INTEGER);
        ParamPOJO paramPOJO4 = mockPathParam("test4", 6, ParamPOJO.DataType.NUMBER);
        ParamPOJO paramPOJO5 = mockPathParam("test5", 7, ParamPOJO.DataType.LONG);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO, paramPOJO2, paramPOJO3, paramPOJO4, paramPOJO5 };
        when(params.getRequired()).thenReturn(paramPOJOs);

        RequestContext.set(RequestConstants.URI, "/3/abc/{test}/{test2}/{test3}/{test4}/{test5}");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/works/true/2132/32.532/2147483648");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(5, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof String);
        assertEquals("works", test);

        Object test2 = parsedParams.get("test2");
        assertTrue(test2 instanceof Boolean);
        assertEquals(true, test2);

        Object test3 = parsedParams.get("test3");
        assertTrue(test3 instanceof Integer);
        assertEquals(2132, test3);

        Object test4 = parsedParams.get("test4");
        assertTrue(test4 instanceof Double);
        assertEquals(32.532, test4);

        Object test5 = parsedParams.get("test5");
        assertTrue(test5 instanceof Long);
        assertEquals(2147483648L, test5);
    }

    @Test
    public void testEnumParams() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockQueryParam("test", ParamPOJO.DataType.ENUM);
        mockJavaType(paramPOJO, "com.flipkart.poseidon.TestEnum");
        ParamPOJO paramPOJO1 = mockHeaderParam("test1", ParamPOJO.DataType.ENUM);
        mockJavaType(paramPOJO1, "com.flipkart.poseidon.TestEnum");
        ParamPOJO paramPOJO2 = mockPathParam("test2", 2, ParamPOJO.DataType.ENUM);
        mockJavaType(paramPOJO2, "com.flipkart.poseidon.TestEnum");

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO, paramPOJO1, paramPOJO2 };
        when(params.getRequired()).thenReturn(paramPOJOs);
        load(params);

        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("test", new String[] { "XYZ" });

        Map<String, String> headers = new HashMap<>();
        headers.put("test1", "ABC");

        RequestContext.set(RequestConstants.URI, "/3/{test2}/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/ABC/xyz", parameterMap, headers);

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(3, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof TestEnum);
        assertEquals(TestEnum.XYZ, test);

        Object testPath = parsedParams.get("test2");
        assertTrue(testPath instanceof TestEnum);
        assertEquals(TestEnum.ABC, testPath);

        Object testHeader = parsedParams.get("test1");
        assertTrue(testHeader instanceof TestEnum);
        assertEquals(TestEnum.ABC, testHeader);
    }

    @Test(expected = BadRequestException.class)
    public void testQueryParamEnumWrong() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockQueryParam("test", ParamPOJO.DataType.ENUM);
        mockJavaType(paramPOJO, "com.flipkart.poseidon.TestEnum");

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);

        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("test", new String[] { "XYZ-A" });

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz", parameterMap);

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());
        fail();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testQueryParamEnumWrongClass() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockQueryParam("test", ParamPOJO.DataType.ENUM);
        mockJavaType(paramPOJO, "com.flipkart.poseidon.TestEnum123");

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);
        load(params);

        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("test", new String[] { "XYZ-A" });

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz", parameterMap);

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());
        fail();
    }

    @Test
    public void testBodyParam() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockBodyParam("test", "com.flipkart.poseidon.TestPOJO");

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);
        load(params);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        request.setAttribute(RequestConstants.BODY, configuration.getObjectMapper().writeValueAsString(new TestPOJO("xyz", true)));
        request.setAttribute(RequestConstants.BODY_BYTES, configuration.getObjectMapper().writeValueAsBytes(new TestPOJO("abc", true)));

        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof TestPOJO);
        TestPOJO testPOJO = (TestPOJO) test;
        assertEquals("xyz", testPOJO.getAbc());
        assertEquals(true, testPOJO.isTest());
    }

    @Test
    public void testBodyParamList() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockBodyParam("test",
                new VariableModel("java.util.List",
                        new VariableModel[] { new VariableModel("com.flipkart.poseidon.TestPOJO") }));

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);
        load(params);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        request.setAttribute(RequestConstants.BODY, configuration.getObjectMapper().writeValueAsString(Collections.singletonList(new TestPOJO("xyz", true))));

        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof List);
        Object testPOJOBlob = ((List) test).get(0);
        assertTrue(testPOJOBlob instanceof TestPOJO);
        TestPOJO testPOJO = (TestPOJO) testPOJOBlob;
        assertEquals("xyz", testPOJO.getAbc());
        assertEquals(true, testPOJO.isTest());
    }

    @Test
    public void testBodyParamMap() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockBodyParam("test",
                new VariableModel("java.util.Map", new VariableModel[] {
                        new VariableModel("java.lang.String"),
                        new VariableModel("com.flipkart.poseidon.TestPOJO")
                }));

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);
        load(params);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        request.setAttribute(RequestConstants.BODY, configuration.getObjectMapper().writeValueAsString(Collections.singletonMap("key", new TestPOJO("xyz", true))));

        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof Map);
        Object testPOJOBlob = ((Map) test).get("key");
        assertTrue(testPOJOBlob instanceof TestPOJO);
        TestPOJO testPOJO = (TestPOJO) testPOJOBlob;
        assertEquals("xyz", testPOJO.getAbc());
        assertEquals(true, testPOJO.isTest());
    }

    @Test
    public void testBodyParamGenerics() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockBodyParam("test",
                new VariableModel("com.flipkart.poseidon.Wrapper", new VariableModel[] {
                        new VariableModel("java.util.Map", new VariableModel[] {
                                new VariableModel("java.lang.String"),
                                new VariableModel("com.flipkart.poseidon.TestPOJO")
                        })
                }));

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);
        load(params);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        request.setAttribute(RequestConstants.BODY, configuration.getObjectMapper().writeValueAsString(new Wrapper<>(Collections.singletonMap("key", new TestPOJO("xyz", true)))));

        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof Wrapper);
        Object wrapperMap = ((Wrapper) test).getObj();
        assertTrue(wrapperMap instanceof Map);
        Object testPOJOBlob = ((Map) wrapperMap).get("key");
        assertTrue(testPOJOBlob instanceof TestPOJO);
        TestPOJO testPOJO = (TestPOJO) testPOJOBlob;
        assertEquals("xyz", testPOJO.getAbc());
        assertEquals(true, testPOJO.isTest());
    }

    @Test
    public void testBodyBytesParam() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockBodyParam("test", "com.flipkart.poseidon.TestPOJO");

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);
        load(params);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        request.setAttribute(RequestConstants.BODY_BYTES, configuration.getObjectMapper().writeValueAsBytes(new TestPOJO("xyz", true)));

        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof TestPOJO);
        TestPOJO testPOJO = (TestPOJO) test;
        assertEquals("xyz", testPOJO.getAbc());
        assertEquals(true, testPOJO.isTest());
    }

    @Test
    public void testBodyBytesParamList() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockBodyParam("test",
                new VariableModel("java.util.List",
                        new VariableModel[] { new VariableModel("com.flipkart.poseidon.TestPOJO") }));

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);
        load(params);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        request.setAttribute(RequestConstants.BODY_BYTES, configuration.getObjectMapper().writeValueAsBytes(Collections.singletonList(new TestPOJO("xyz", true))));

        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof List);
        Object testPOJOBlob = ((List) test).get(0);
        assertTrue(testPOJOBlob instanceof TestPOJO);
        TestPOJO testPOJO = (TestPOJO) testPOJOBlob;
        assertEquals("xyz", testPOJO.getAbc());
        assertEquals(true, testPOJO.isTest());
    }

    @Test
    public void testBodyBytesParamMap() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockBodyParam("test",
                new VariableModel("java.util.Map", new VariableModel[] {
                        new VariableModel("java.lang.String"),
                        new VariableModel("com.flipkart.poseidon.TestPOJO")
                }));

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);
        load(params);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        request.setAttribute(RequestConstants.BODY_BYTES, configuration.getObjectMapper().writeValueAsBytes(Collections.singletonMap("key", new TestPOJO("xyz", true))));

        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof Map);
        Object testPOJOBlob = ((Map) test).get("key");
        assertTrue(testPOJOBlob instanceof TestPOJO);
        TestPOJO testPOJO = (TestPOJO) testPOJOBlob;
        assertEquals("xyz", testPOJO.getAbc());
        assertEquals(true, testPOJO.isTest());
    }

    @Test
    public void testBodyBytesParamGenerics() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockBodyParam("test",
                new VariableModel("com.flipkart.poseidon.Wrapper", new VariableModel[] {
                        new VariableModel("java.util.Map", new VariableModel[] {
                                new VariableModel("java.lang.String"),
                                new VariableModel("com.flipkart.poseidon.TestPOJO")
                        })
                }));

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);
        load(params);

        RequestContext.set(RequestConstants.URI, "/3/abc/xyz");
        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        request.setAttribute(RequestConstants.BODY_BYTES, configuration.getObjectMapper().writeValueAsBytes(new Wrapper<>(Collections.singletonMap("key", new TestPOJO("xyz", true)))));

        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());

        Object test = parsedParams.get("test");
        assertTrue(test instanceof Wrapper);
        Object wrapperMap = ((Wrapper) test).getObj();
        assertTrue(wrapperMap instanceof Map);
        Object testPOJOBlob = ((Map) wrapperMap).get("key");
        assertTrue(testPOJOBlob instanceof TestPOJO);
        TestPOJO testPOJO = (TestPOJO) testPOJOBlob;
        assertEquals("xyz", testPOJO.getAbc());
        assertEquals(true, testPOJO.isTest());
    }

    private ParamPOJO mockPathParam(String name, int position) {
        return mockPathParam(name, position, ParamPOJO.DataType.STRING);
    }

    private ParamPOJO mockPathParam(String name, int position, ParamPOJO.DataType type) {
        ParamPOJO paramPOJO = spy(ParamPOJO.class);
        when(paramPOJO.getName()).thenReturn(name);
        when(paramPOJO.getDatatype()).thenReturn(ParamPOJO.DataType.STRING);
        when(paramPOJO.isPathparam()).thenReturn(true);
        when(paramPOJO.getPosition()).thenReturn(position);
        when(paramPOJO.getDatatype()).thenReturn(type);
        return paramPOJO;
    }

    private ParamPOJO mockQueryParam(String name, ParamPOJO.DataType type) {
        ParamPOJO paramPOJO = spy(ParamPOJO.class);
        when(paramPOJO.getName()).thenReturn(name);
        when(paramPOJO.getDatatype()).thenReturn(type);
        return paramPOJO;
    }

    private ParamPOJO mockBodyParam(String name, String javaType) {
        ParamPOJO paramPOJO = spy(ParamPOJO.class);
        when(paramPOJO.getName()).thenReturn(name);
        when(paramPOJO.getType()).thenReturn(new VariableModel(javaType));
        when(paramPOJO.getJavatype()).thenReturn(javaType);
        when(paramPOJO.isBody()).thenReturn(true);
        return paramPOJO;
    }

    private ParamPOJO mockBodyParam(String name, VariableModel variableModel) {
        ParamPOJO paramPOJO = spy(ParamPOJO.class);
        when(paramPOJO.getName()).thenReturn(name);
        when(paramPOJO.getType()).thenReturn(variableModel);
        when(paramPOJO.isBody()).thenReturn(true);
        return paramPOJO;
    }

    private ParamPOJO mockHeaderParam(String name, ParamPOJO.DataType type) {
        ParamPOJO paramPOJO = spy(ParamPOJO.class);
        when(paramPOJO.getName()).thenReturn(name);
        when(paramPOJO.getDatatype()).thenReturn(type);
        when(paramPOJO.isHeader()).thenReturn(true);
        return paramPOJO;
    }

    private void mockJavaType(ParamPOJO paramPOJO, String javaType) {
        when(paramPOJO.getJavatype()).thenReturn(javaType);
    }

    private HttpServletRequest mockHttpServletRequest(String path) {
        return mockHttpServletRequest(path, new HashMap<>(), new HashMap<>());
    }

    private HttpServletRequest mockHttpServletRequest(String path, Map<String, String[]> parameterMap) {
        return mockHttpServletRequest(path, parameterMap, new HashMap<>());
    }

    private HttpServletRequest mockHttpServletRequest(String path, Map<String, String[]> parameterMap, Map<String, String> headers) {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getPathInfo()).thenReturn(path);
        when(servletRequest.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
        headers.forEach((h, v) -> {
            when(servletRequest.getHeader(eq(h))).thenReturn(v);
        });
        when(servletRequest.getParameterMap()).thenReturn(parameterMap);
        return servletRequest;
    }

    private void load(ParamsPOJO params) {
        EndpointPOJO endpointPOJO = spy(EndpointPOJO.class);
        when(endpointPOJO.getParams()).thenReturn(params);

        new APIBuildable(null, endpointPOJO, configuration, new HashMap<>());
    }
}