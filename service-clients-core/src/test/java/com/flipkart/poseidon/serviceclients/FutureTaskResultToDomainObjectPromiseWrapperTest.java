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

import com.flipkart.phantom.task.spi.TaskResult;
import flipkart.lego.concurrency.api.PromiseListener;
import flipkart.lego.concurrency.exceptions.PromiseBrokenException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FutureTaskResultToDomainObjectPromiseWrapper.class)
public class FutureTaskResultToDomainObjectPromiseWrapperTest {

    FutureTaskResultToDomainObjectPromiseWrapper wrapper;
    Future<TaskResult> future = mock(Future.class);

    PromiseListener listener1 = mock(PromiseListener.class);
    PromiseListener listener2 = mock(PromiseListener.class);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        wrapper = spy(new FutureTaskResultToDomainObjectPromiseWrapper(future));

        wrapper.addListener(listener1);
        wrapper.addListener(listener2);
    }

    @Test
    public void testIsRealized() throws Exception {
        when(future.isDone()).thenReturn(true);
        assertTrue(wrapper.isRealized());
    }

    @Test
    public void testIsFulfilled() throws Exception {
        when(future.isCancelled()).thenReturn(false);
        assertTrue(wrapper.isFullfilled());
    }

    @Test
    public void testIsBroken() throws Exception {
        when(future.isCancelled()).thenReturn(false);
        assertFalse(wrapper.isBroken());
    }

    @Test
    public void testAwaitExecutionException() throws Exception {
        exception.expect(InterruptedException.class);
        when(future.get()).thenThrow(ExecutionException.class);
        wrapper.await();
    }

    @Test
    public void testAwaitCancellationException() throws Exception {
        when(future.get()).thenThrow(CancellationException.class);
        wrapper.await();
    }

    @Test
    public void testAwait() throws Exception {
        wrapper.await();
    }

    @Test
    public void testGetNullTaskResult() throws Exception {
        when(future.get()).thenReturn(null);
        exception.expectMessage(equalTo("Task result is null"));
        exception.expect(PromiseBrokenException.class);
        wrapper.get();
    }

    @Test
    public void testGetSuccessCase() throws Exception {
        ServiceResponse<String> response = new ServiceResponse<>("test", new HashMap<String, String>() {{
            put("header1", "value1");
        }});
        TaskResult<ServiceResponse> result = new TaskResult<>(true, "response", response);
        when(future.get()).thenReturn(result);
        String answer = (String) wrapper.get();
        assertEquals("test", answer);
        assertEquals(wrapper.getHeaders().get("header1"), "value1");
    }

    @Test
    public void testGetExceptionCase() throws Exception {
        ServiceResponse response = new ServiceResponse(new ServiceClientException("test"), new HashMap<String, String>() {{
            put("header1", "value1");
        }});
        TaskResult<ServiceResponse> result = new TaskResult<>(true, "response", response);
        when(future.get()).thenReturn(result);
        exception.expect(ServiceClientException.class);
        exception.expect(PromiseBrokenException.class);
        exception.expectMessage(equalTo("test"));
        assertEquals(wrapper.getHeaders().get("header1"), "value1");
        wrapper.get();

    }

}