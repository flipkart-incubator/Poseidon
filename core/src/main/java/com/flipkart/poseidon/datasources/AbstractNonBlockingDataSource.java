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

package com.flipkart.poseidon.datasources;

import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import flipkart.lego.api.entities.DataType;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;
import flipkart.lego.concurrency.api.NonBlockingDataSource;

public abstract class AbstractNonBlockingDataSource<S, T extends DataType> extends NonBlockingDataSource<S,T> {

    protected final PoseidonLegoSet legoset;
    protected final Request request;

    public AbstractNonBlockingDataSource(LegoSet legoset, Request request) {
        this.legoset = (PoseidonLegoSet) legoset;
        this.request = request;
    }
}
