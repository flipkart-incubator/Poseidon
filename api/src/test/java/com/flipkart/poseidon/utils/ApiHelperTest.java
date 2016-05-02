package com.flipkart.poseidon.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class ApiHelperTest {

    @Test
    public void testGetUrlWithHttpMethod() throws Exception {
        String url = "/india///state/";
        String method = "GET";
        String result = ApiHelper.getUrlWithHttpMethod(url, method);
        assertEquals("GET/india/state/", result);
    }
}