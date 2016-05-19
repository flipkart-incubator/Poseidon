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

import com.flipkart.poseidon.annotations.Name;
import com.flipkart.poseidon.annotations.Version;
import com.flipkart.poseidon.datasources.AbstractDataSource;
import com.flipkart.poseidon.legoset.PoseidonLegoSetTest;
import flipkart.lego.api.entities.DataType;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;

/**
 * Created by shrey.garg on 19/05/16.
 */
@Name(PoseidonLegoSetTest.PROPER_DS_NAME)
@Version(major = 4, minor = 1, patch = 6)
public class ProperDataSource extends AbstractDataSource {
    public ProperDataSource(LegoSet legoset, Request request) {
        super(legoset, request);
    }

    @Override
    public DataType call() throws Exception {
        return null;
    }

    @Override
    public String getId() throws UnsupportedOperationException {
        return "OldIdForProperDataSource";
    }
}
