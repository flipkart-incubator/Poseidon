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

package com.flipkart.poseidon.core;

import com.google.common.net.MediaType;
import flipkart.lego.api.entities.Response;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PoseidonResponse implements Response {

    private final ConcurrentHashMap<String, String> attributes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> headers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> multiValueHeaders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Cookie> cookies = new ConcurrentHashMap<>();
    private final List<Object> mappedBeans = new ArrayList<>();
    private Object response;
    private MediaType contentType;
    private int statusCode;
    private Optional<HttpServletResponse> httpResponse;

    @Override
    public Object getResponse() {
        return response;
    }

    @Override
    public void setResponse(Object responseBody) {
        response = responseBody;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public void setContentType(MediaType contentType) {
        this.contentType = contentType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public ConcurrentHashMap<String, String> getAttributes() {
        return attributes;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void removeHeader(String key) {
        headers.remove(key);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public ConcurrentHashMap<String, String> getHeaders() {
        return headers;
    }

    public void addMultiValueHeader(String key, String value) {
        if (value == null) {
            return;
        }

        List<String> values = Optional.ofNullable(multiValueHeaders.get(key)).orElseGet(ArrayList::new);
        values.add(value);
        multiValueHeaders.put(key, values);
    }

    public void removeMultiValueHeader(String key) {
        multiValueHeaders.remove(key);
    }

    public void removeMultiValueHeaderValue(String key, String value) {
        Optional<List<String>> optionalValues = Optional.ofNullable(multiValueHeaders.get(key));
        if (!optionalValues.isPresent()) {
            return;
        }

        optionalValues.get().remove(value);
    }

    public List<String> getMultiValueHeaderValues(String key) {
        return multiValueHeaders.get(key);
    }

    public ConcurrentHashMap<String, List<String>> getMultiValueHeaders() {
        return multiValueHeaders;
    }

    public void addCookie(String key, Cookie value) {
        cookies.put(key, value);
    }

    public void removeCookie(String key) {
        cookies.remove(key);
    }

    public Cookie getCookie(String key) {
        return cookies.get(key);
    }

    public ConcurrentHashMap<String, Cookie> getCookies() {
        return cookies;
    }

    public List<Object> getMappedBeans() {
        return mappedBeans;
    }

    public void addMappedBeans(Collection<Object> mappedBeans) {
        this.mappedBeans.addAll(mappedBeans);
    }

    public Optional<HttpServletResponse> getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(Optional<HttpServletResponse> httpResponse) {
        this.httpResponse = httpResponse;
    }
}
