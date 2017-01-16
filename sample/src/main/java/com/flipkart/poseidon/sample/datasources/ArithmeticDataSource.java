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

package com.flipkart.poseidon.sample.datasources;

import com.flipkart.poseidon.datasources.AbstractDataSource;
import com.flipkart.poseidon.model.annotations.Description;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import com.flipkart.poseidon.sample.datatypes.ArithmeticResultDataType;
import com.flipkart.poseidon.sample.serviceclients.ArithmeticServiceClient;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;

/**
 * Executes arithmetic operations using arithmetic service client
 *
 * Created by mohan.pandian on 16/01/17.
 */
@Name("ArithmeticDS")
@Version(major = 1, minor = 0, patch = 0)
@Description("Performs arithmetic operations")
public class ArithmeticDataSource extends AbstractDataSource<ArithmeticResultDataType> {
    public ArithmeticDataSource(LegoSet legoset, Request request) {
        super(legoset, request);
    }

    @Override
    public ArithmeticResultDataType call() throws Exception {
        float num1 = ((Double) request.getAttribute("num1")).floatValue();
        float num2 = ((Double) request.getAttribute("num2")).floatValue();
        String operation = request.getAttribute("operation");

        ArithmeticServiceClient serviceClient = (ArithmeticServiceClient) legoset.getServiceClient("arithmeticSC_1.0.0");
        ArithmeticResultDataType dataType = new ArithmeticResultDataType();
        dataType.setResult(serviceClient.doOperation(num1, num2, operation));
        return dataType;
    }
}
