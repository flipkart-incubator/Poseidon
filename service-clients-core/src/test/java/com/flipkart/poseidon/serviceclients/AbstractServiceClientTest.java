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

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for AbstractServiceClient
 *
 * Created by mohan.pandian on 22/08/16.
 */
public class AbstractServiceClientTest {

    private AbstractServiceClient serviceClient;

    @Before
    public void setup() {
        serviceClient = new TestAbstractServiceClient();
    }

    @Test
    public void testMultiValueParamURI() {
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

    @Test
    public void testEncodePathParam() throws JsonProcessingException {
        assertEquals("path_param", serviceClient.encodePathParam("path_param"));
        assertEquals("path%20param", serviceClient.encodePathParam("path param"));
        assertEquals("path+param", serviceClient.encodePathParam("path+param"));
    }

    @Test
    public void testPathParamToString() throws JsonProcessingException {
        assertEquals(TestEnum.TEST_ENUM_VALUE1.value, serviceClient.pathParamToString(TestEnum.TEST_ENUM_VALUE1));
        assertEquals("", serviceClient.pathParamToString(ImmutableList.of()));
        assertEquals("path param1,path param2", serviceClient.pathParamToString(ImmutableList.of("path param1", "path param2")));
        assertEquals("test enum value 1,test enum value 2",
                serviceClient.pathParamToString(ImmutableList.of(TestEnum.TEST_ENUM_VALUE1, TestEnum.TEST_ENUM_VALUE2)));
        assertEquals("", serviceClient.pathParamToString((Object) null));
        assertEquals("", serviceClient.pathParamToString(null));
        assertEquals("512", serviceClient.pathParamToString(512));
        assertEquals("{\"testField\":false}", serviceClient.pathParamToString(new TestObject()));

    }

    public static class TestAbstractServiceClient extends AbstractServiceClient {

        @Override
        protected String getCommandName() {
            return null;
        }

    }

    private enum TestEnum {
        TEST_ENUM_VALUE1("test enum value 1"),
        TEST_ENUM_VALUE2("test enum value 2");

        private final String value;

        TestEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String toString() {
            return this.value;
        }

    }

    private static class TestObject {
        private boolean testField;

        public boolean isTestField() {
            return testField;
        }
    }
}
