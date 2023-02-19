/*
 * Copyright 2015 Flipkart Internet, pvt ltd.
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

import flipkart.lego.concurrency.exceptions.PromiseBrokenException;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

public class ServiceResponseTest {

    @Test
    public void testGetData() throws Exception {
        ServiceResponse<String> response = new ServiceResponse<>("string", new HashMap<String, String>() {{
            put("header1", "value1");
        }});
        assertEquals(response.getDataList().get(0), "string");
        assertEquals(response.getHeaders().get("header1"), "value1");
        assertTrue(response.getIsSuccess());
        assertNull(response.getException());
    }

    @Test
    public void testGetException() throws Exception {
        ServiceResponse response = new ServiceResponse(new ServiceClientException("exception", null), new HashMap<String, String>() {{
            put("header1", "value1");
        }});
        assertThat(response.getException(), instanceOf(ServiceClientException.class));
        assertThat(response.getException(), instanceOf(PromiseBrokenException.class));
        assertEquals(response.getHeaders().get("header1"), "value1");
        assertFalse(response.getIsSuccess());
        assertTrue(response.getDataList().isEmpty());
    }

    @Test
    public void testCaseInsensitiveGetHeader() throws Exception {
        ServiceResponse<String> response = new ServiceResponse<>("string", new HashMap<String, String>() {{
            put("heAder1", "value1");
        }});
        assertEquals("value1", response.getHeaders().get("Header1"));
        assertEquals("value1", response.getHeaders().get("header1"));
        assertEquals("value1", response.getHeaders().get("hEader1"));
    }

}