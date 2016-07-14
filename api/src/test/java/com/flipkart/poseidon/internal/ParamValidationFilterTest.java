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
import static org.mockito.Mockito.when;

/**
 * Created by shrey.garg on 14/07/16.
 */
public class ParamValidationFilterTest {
    private Configuration configuration = null;

    @Before
    public void setUp() throws Exception {
        RequestContext.set(RequestConstants.URI, "/123/345");
    }

    @Test
    public void testPathParam() throws Exception {
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 4);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);

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

        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());
        assertEquals("xyz", parsedParams.get("test"));
    }

    @Test
    public void testPathParamInfiniteTail() throws Exception {
        RequestContext.set(RequestConstants.URI, "/123/345/543/{TAIL}");
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 4);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO };
        when(params.getRequired()).thenReturn(paramPOJOs);

        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe/yui/ase");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(1, parsedParams.size());
        assertEquals("qwe/yui/ase", parsedParams.get("test"));
    }

    @Test
    public void testPathParamInfiniteTailMiddle() throws Exception {
        RequestContext.set(RequestConstants.URI, "/123/{MIDDLE}/543/{TAIL}");
        ParamsPOJO params = mock(ParamsPOJO.class);

        ParamPOJO paramPOJO = mockPathParam("test", 4);
        ParamPOJO paramPOJO2 = mockPathParam("test2", 3);

        ParamPOJO[] paramPOJOs = new ParamPOJO[] { paramPOJO, paramPOJO2 };
        when(params.getRequired()).thenReturn(paramPOJOs);

        HttpServletRequest servletRequest = mockHttpServletRequest("/3/abc/xyz/qwe/yui/ase");

        PoseidonRequest request = new PoseidonRequest(servletRequest);
        new ParamValidationFilter(params, configuration).filterRequest(request, new PoseidonResponse());

        Map<String, Object> parsedParams = request.getAttribute(RequestConstants.PARAMS);
        assertEquals(2, parsedParams.size());
        assertEquals("qwe/yui/ase", parsedParams.get("test"));
        assertEquals("xyz", parsedParams.get("test2"));
    }

    private ParamPOJO mockPathParam(String name, int position) {
        ParamPOJO paramPOJO = mock(ParamPOJO.class);
        when(paramPOJO.getName()).thenReturn(name);
        when(paramPOJO.getDatatype()).thenReturn(ParamPOJO.DataType.STRING);
        when(paramPOJO.getPosition()).thenReturn(position);
        when(paramPOJO.isPathparam()).thenReturn(true);
        return paramPOJO;
    }

    private HttpServletRequest mockHttpServletRequest(String path) {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getPathInfo()).thenReturn(path);
        when(servletRequest.getHeaderNames()).thenReturn(Collections.enumeration(new ArrayList<>()));
        return servletRequest;
    }
}