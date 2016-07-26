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

package com.flipkart.poseidon.datasources;

import flipkart.lego.api.entities.Request;

import java.util.HashMap;
import java.util.Map;

public class DataSourceRequest implements Request {

    private final Map<String, Object> attributes;

    public DataSourceRequest() {
        attributes = new HashMap<>();
    }

    /**
     * Constructor to Clone Existing DataSourceRequest
     */
    public DataSourceRequest(DataSourceRequest dataSourceRequest) {

        attributes = new HashMap<>(dataSourceRequest.getAttributeMap());
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes.putAll(attributes);
    }

    @Override
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    @Override
    public Map<String, Object> getAttributeMap() {
        return new HashMap<>(attributes);
    }

    @Override
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    @Override
    public String toString() {
        return attributes.toString();
    }
}
