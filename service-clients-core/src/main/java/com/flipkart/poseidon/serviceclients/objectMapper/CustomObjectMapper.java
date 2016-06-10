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

package com.flipkart.poseidon.serviceclients.objectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by venkata.lakshmi on 16/04/15.
 *
 * Allows to configure object mapper instance. Creates a custom object mapper for a service
 */
public abstract class CustomObjectMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomObjectMapper() {
        configure(objectMapper);
    }
    public abstract void configure(final ObjectMapper objectMapper);

    public final ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
