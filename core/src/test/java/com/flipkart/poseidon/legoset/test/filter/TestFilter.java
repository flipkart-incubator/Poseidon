/*
 * Copyright 2017 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon.legoset.test.filter;

import com.flipkart.poseidon.datasources.ServiceClient;
import com.flipkart.poseidon.filters.AbstractFilter;
import com.flipkart.poseidon.legoset.PoseidonLegoSetTest;
import com.flipkart.poseidon.legoset.test.client.TestClient;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;
import flipkart.lego.api.entities.Response;
import flipkart.lego.api.exceptions.BadRequestException;
import flipkart.lego.api.exceptions.InternalErrorException;
import flipkart.lego.api.exceptions.ProcessingException;

import javax.inject.Inject;

/**
 * Created by shrey.garg on 04/10/18.
 */
@Name(PoseidonLegoSetTest.TEST_FILTER)
@Version(major = 4, minor = 1, patch = 6)
public class TestFilter extends AbstractFilter {

    @Inject
    public TestFilter(LegoSet legoSet, @ServiceClient TestClient testClient) {
        super(legoSet);
    }

    @Override
    public void filterRequest(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {

    }

    @Override
    public void filterResponse(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {

    }
}
