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

package com.flipkart.poseidon.sample.api;

import com.flipkart.poseidon.api.APILegoSet;

import java.util.Arrays;
import java.util.List;

public class SampleLegoSet extends APILegoSet {
    @Override
    public List<String> getPackagesToScan() {
        return Arrays.asList(
                "com.flipkart.poseidon.sample.datasources",
                "com.flipkart.poseidon.serviceclients", // for generated service clients
                "com.flipkart.poseidon.sample.serviceclients", // for handwritten service clients like ArithmeticServiceClient
                "com.flipkart.poseidon.sample.api.filters");
    }
}
