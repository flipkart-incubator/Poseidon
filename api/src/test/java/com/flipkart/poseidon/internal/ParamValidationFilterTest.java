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

import com.flipkart.poseidon.api.Configuration;
import com.flipkart.poseidon.constants.RequestConstants;
import com.flipkart.poseidon.core.PoseidonRequest;
import com.flipkart.poseidon.core.PoseidonResponse;
import com.flipkart.poseidon.core.RequestContext;
import com.flipkart.poseidon.pojos.ParamPOJO;
import com.flipkart.poseidon.pojos.ParamsPOJO;
import flipkart.lego.api.exceptions.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Created by shrey.garg on 14/07/16.
 */
public class ParamValidationFilterTest {
    private Configuration configuration = null;

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

    private ParamPOJO mockPathParam(String name, int position) {
        ParamPOJO paramPOJO = spy(ParamPOJO.class);
        when(paramPOJO.getName()).thenReturn(name);
        when(paramPOJO.getDatatype()).thenReturn(ParamPOJO.DataType.STRING);
        when(paramPOJO.isPathparam()).thenReturn(true);
        when(paramPOJO.getPosition()).thenReturn(position);
        return paramPOJO;
    }

    private HttpServletRequest mockHttpServletRequest(String path) {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getPathInfo()).thenReturn(path);
        when(servletRequest.getHeaderNames()).thenReturn(Collections.enumeration(new ArrayList<>()));
        return servletRequest;
    }
}