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

package com.flipkart.poseidon.api;

import com.flipkart.poseidon.core.RequestContext;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.flipkart.poseidon.constants.RequestConstants.HEADERS;

/**
 * Created by shrey.garg on 06/04/16.
 */
public interface Headers {
    /*
     * Configuration of headers to be passed through to services transparently (say x-request-id).
     * Distributed tracing headers don't need to be defined here.
     * Headers that are to be used by DataSources explicitly and not used globally should not be defined here.
     */
    Set<HeaderConfiguration> getGlobalHeaders();

    static Optional<String> getGlobalHeader(String headerName) {
        Map<String, String> headers = (Map<String, String>) RequestContext.get(HEADERS);
        return headers != null ? Optional.ofNullable(headers.get(headerName)) : Optional.empty();
    }
}
