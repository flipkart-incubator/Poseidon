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

package com.flipkart.poseidon.sample.serviceclients;

import com.flipkart.phantom.task.impl.TaskContextFactory;
import com.flipkart.poseidon.model.annotations.Description;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import flipkart.lego.api.entities.ServiceClient;
import flipkart.lego.api.exceptions.LegoServiceException;

import java.util.HashMap;
import java.util.Map;

/**
 * Executes arithmetic operations like add using phantom's sample ArtithmeticTaskHandler
 *
 * Created by mohan.pandian on 16/01/17.
 */
@Name("arithmeticSC")
@Version(major = 1, minor = 0, patch = 0)
@Description("Arithmetic service client mainly used in sample application to do arithmetic operations")
public class ArithmeticServiceClient implements ServiceClient {
    public float doOperation(float num1, float num2, String operation) {
        Map<String, Object> params = new HashMap<>();
        params.put("num1", num1);
        params.put("num2", num2);

        // Execute the command synchronously for simplicity. But we can execute an async command
        // and return a future too.
        return ((float) TaskContextFactory.getTaskContext().executeCommand(operation, null, params).getData());
    }

    public void init() throws LegoServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutDown() throws LegoServiceException {
        throw new UnsupportedOperationException();
    }
}
