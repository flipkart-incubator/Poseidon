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

package com.flipkart.poseidon.sample.api.config;

import com.flipkart.poseidon.api.HeaderConfiguration;
import com.flipkart.poseidon.api.Headers;
import com.flipkart.poseidon.sample.commons.SampleConstants;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SampleHeaders implements Headers {
    private Set<HeaderConfiguration> passThroughHeaders;

    public SampleHeaders() {
        passThroughHeaders = new HashSet();
        passThroughHeaders.add(new HeaderConfiguration(SampleConstants.REQUEST_ID_HEADER, () -> UUID.randomUUID().toString()));
    }

    @Override
    public Set<HeaderConfiguration> getGlobalHeaders() {
        return passThroughHeaders;
    }
}
