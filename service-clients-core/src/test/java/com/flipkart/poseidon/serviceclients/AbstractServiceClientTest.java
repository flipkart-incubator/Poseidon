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

package com.flipkart.poseidon.serviceclients;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for AbstractServiceClient
 *
 * Created by mohan.pandian on 22/08/16.
 */
public class AbstractServiceClientTest {
    @Test
    public void testMultiValueParamURI() {
        AbstractServiceClient serviceClient = new TestAbstractServiceClient();
        assertEquals("", serviceClient.getMultiValueParamURI("key", null));
        assertEquals("", serviceClient.getMultiValueParamURI("key", Arrays.asList()));
        assertEquals("key=", serviceClient.getMultiValueParamURI("key", Arrays.asList("")));
        assertEquals("key=a", serviceClient.getMultiValueParamURI("key", Arrays.asList("a")));
        assertEquals("key=a", serviceClient.getMultiValueParamURI("key", Arrays.asList("a", null)));
        assertEquals("key=a&key=b&key=c", serviceClient.getMultiValueParamURI("key", Arrays.asList("a", "b", "c")));
        assertEquals("key=a&key=&key=c", serviceClient.getMultiValueParamURI("key", Arrays.asList("a", "", "c")));
        assertEquals("key=a&key=b%26c&key=c", serviceClient.getMultiValueParamURI("key", Arrays.asList("a", "b&c", "c")));
        assertEquals("key=10&key=20&key=30", serviceClient.getMultiValueParamURI("key", Arrays.asList(10, 20, 30)));
    }

    public static class TestAbstractServiceClient extends AbstractServiceClient {

        @Override
        protected String getCommandName() {
            return null;
        }

    }
}
