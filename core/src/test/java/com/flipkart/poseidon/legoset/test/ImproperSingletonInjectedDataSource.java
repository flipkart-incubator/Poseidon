/*
 * Copyright 2020 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon.legoset.test;

import com.flipkart.poseidon.datasources.AbstractDataSource;
import com.flipkart.poseidon.datasources.SystemDataSource;
import com.flipkart.poseidon.legoset.PoseidonLegoSetTest;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import flipkart.lego.api.entities.DataType;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;

import javax.inject.Inject;

@Name(PoseidonLegoSetTest.IMPROPER_INJECTED_SINGLETON_DS_NAME)
@Version(major = 4, minor = 1, patch = 6)
public class ImproperSingletonInjectedDataSource extends AbstractDataSource {
    @Inject
    public ImproperSingletonInjectedDataSource(LegoSet legoset, Request request, @SystemDataSource(PoseidonLegoSetTest.QUALIFIER_INJECTABLE_DS_NAME + "_4.1.6") InjectableSystemDataSource dataSource) {
        super(legoset, request);
        assert dataSource != null;
    }

    @Override
    public DataType call() throws Exception {
        return null;
    }
}
