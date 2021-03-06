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

package com.flipkart.poseidon.legoset.test;

import com.flipkart.poseidon.datasources.AbstractDataSource;
import com.flipkart.poseidon.datasources.ServiceClient;
import com.flipkart.poseidon.legoset.PoseidonLegoSetTest;
import com.flipkart.poseidon.legoset.test.client.TestClient;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import flipkart.lego.api.entities.DataType;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;

import javax.inject.Inject;

/**
 * Created by shrey.garg on 19/05/16.
 */
@Name(PoseidonLegoSetTest.PROPER_INJECTABLE_DS_NAME)
@Version(major = 4, minor = 1, patch = 6)
public class InjectableProperDataSource extends AbstractDataSource {

    @Inject
    public InjectableProperDataSource(LegoSet legoset, Request request, String injected, @ServiceClient TestClient testClient) {
        super(legoset, request);
    }

    @Override
    public DataType call() throws Exception {
        return null;
    }
}
