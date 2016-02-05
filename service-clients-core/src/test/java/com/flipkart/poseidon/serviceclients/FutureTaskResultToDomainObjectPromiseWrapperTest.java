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
import com.flipkart.poseidon.serviceclients.batch.ResponseMerger;
import flipkart.lego.concurrency.api.PromiseListener;
import flipkart.lego.concurrency.exceptions.PromiseBrokenException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FutureTaskResultToDomainObjectPromiseWrapper.class)
public class FutureTaskResultToDomainObjectPromiseWrapperTest {

    private FutureTaskResultToDomainObjectPromiseWrapper wrapper;
    private Future<TaskResult> future1 = mock(Future.class);
    private Future<TaskResult> future2 = mock(Future.class);
    private long timeout = 5;
    private TimeUnit timeUnit = TimeUnit.MINUTES;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        wrapper = spy(new FutureTaskResultToDomainObjectPromiseWrapper(future1));
        wrapper.addFutureForTask(future2);
    }

    @Test
    public void testIsRealized() throws Exception {
        when(future1.isDone()).thenReturn(true);
        when(future2.isDone()).thenReturn(true);
        assertTrue(wrapper.isRealized());
    }

    @Test
    public void testIsRealizedFailure() throws Exception {
        when(future1.isDone()).thenReturn(true);
        when(future2.isDone()).thenReturn(false);
        assertFalse(wrapper.isRealized());
    }

    @Test
    public void testIsFulfilled() throws Exception {
        when(future1.isCancelled()).thenReturn(false);
        when(future2.isCancelled()).thenReturn(false);
        assertTrue(wrapper.isFullfilled());
    }

    @Test
    public void testIsFulfilledFailure() throws Exception {
        when(future1.isCancelled()).thenReturn(false);
        when(future2.isCancelled()).thenReturn(true);
        assertFalse(wrapper.isFullfilled());
    }

    @Test
    public void testIsBroken() throws Exception {
        when(future1.isCancelled()).thenReturn(false);
        when(future2.isCancelled()).thenReturn(false);
        assertFalse(wrapper.isBroken());
    }

    @Test
    public void testIsBrokenFailure() throws Exception {
        when(future1.isCancelled()).thenReturn(false);
        when(future2.isCancelled()).thenReturn(true);
        assertTrue(wrapper.isBroken());
    }

    @Test
    public void testAwaitExecutionException() throws Exception {
        exception.expect(InterruptedException.class);
        when(future1.get()).thenThrow(ExecutionException.class);
        wrapper.await();
    }

    @Test
    public void testAwaitCancellationException() throws Exception {
        when(future1.get()).thenThrow(CancellationException.class);
        wrapper.await();
    }

    @Test
    public void testAwait() throws Exception {
        wrapper.await();
        Mockito.verify(future1).get();
        Mockito.verify(future2).get();
    }

    @Test
    public void testAwaitTimeoutExecutionException() throws Exception {
        exception.expect(InterruptedException.class);
        when(future1.get(timeout, timeUnit)).thenThrow(ExecutionException.class);
        wrapper.await(timeout, timeUnit);
    }

    @Test
    public void testAwaitTimeoutCancellationException() throws Exception {
        when(future1.get(timeout, timeUnit)).thenThrow(CancellationException.class);
        wrapper.await(timeout, timeUnit);
    }

    @Test
    public void testAwaitWithTimeOut() throws Exception {
        wrapper.await(timeout, timeUnit);
        Mockito.verify(future1).get(timeout, timeUnit);
        Mockito.verify(future2).get(timeout, timeUnit);
    }

    @Test
    public void testGetNullTaskResult() throws Exception {
        when(future1.get()).thenReturn(null);
        exception.expectMessage(equalTo("Task result is null"));
        exception.expect(PromiseBrokenException.class);
        wrapper.get();
    }

    @Test
    public void testGetFutureCancellationException() throws Exception {
        when(future1.get()).thenThrow(CancellationException.class);
        exception.expect(PromiseBrokenException.class);
        wrapper.get();
    }

    @Test
    public void testGetFutureExecutionException() throws Exception {
        when(future1.get()).thenThrow(ExecutionException.class);
        exception.expect(InterruptedException.class);
        wrapper.get();
    }

    @Test
    public void testGetSuccessCase() throws Exception {
        ServiceResponse<String> response1 = new ServiceResponse<>("test", new HashMap<String, String>() {{
            put("header1", "value1");
        }});

        ServiceResponse<String> response2 = new ServiceResponse<>("test12", new HashMap<String, String>() {{
            put("header1", "value1");
        }});

        TaskResult<ServiceResponse> result1 = new TaskResult<>(true, "response", response1);
        TaskResult<ServiceResponse> result2 = new TaskResult<>(true, "response", response2);

        when(future1.get()).thenReturn(result1);
        when(future2.get()).thenReturn(result2);

        String answer = (String) wrapper.get();
        assertEquals("test", answer);
        assertEquals(wrapper.getHeaders().get("header1"), "value1");
    }

    @Test
    public void testGetSuccessCaseWithMerger() throws Exception {
        ResponseMerger merger = mock(ResponseMerger.class);
        FutureTaskResultToDomainObjectPromiseWrapper wrapper = spy(new FutureTaskResultToDomainObjectPromiseWrapper(merger));
        wrapper.addFutureForTask(future1);
        wrapper.addFutureForTask(future2);
        ServiceResponse<String> response1 = new ServiceResponse<>("test", new HashMap<String, String>() {{
            put("header1", "value1");
        }});

        ServiceResponse<String> response2 = new ServiceResponse<>("test12", new HashMap<String, String>() {{
            put("header1", "value1");
        }});

        TaskResult<ServiceResponse> result1 = new TaskResult<>(true, "response", response1);
        TaskResult<ServiceResponse> result2 = new TaskResult<>(true, "response", response2);

        List<String> responses = new ArrayList<>();
        responses.add("test");
        responses.add("test12");
        when(merger.mergeResponse(responses)).thenReturn("test");

        when(future1.get()).thenReturn(result1);
        when(future2.get()).thenReturn(result2);
        String answer = (String) wrapper.get();
        assertEquals("test", answer);
        assertEquals(wrapper.getHeaders().get("header1"), "value1");
        Mockito.verify(merger).mergeResponse(responses);
    }

    @Test
    public void testGetExceptionCase() throws Exception {
        ServiceResponse response = new ServiceResponse(new ServiceClientException("test"), new HashMap<String, String>() {{
            put("header1", "value1");
        }});
        TaskResult<ServiceResponse> result = new TaskResult<>(true, "response", response);
        when(future1.get()).thenReturn(result);
        exception.expect(ServiceClientException.class);
        exception.expect(PromiseBrokenException.class);
        exception.expectMessage(equalTo("test"));
        assertEquals(wrapper.getHeaders().get("header1"), "value1");
        wrapper.get();
    }

    @Test
    public void testGetNullTaskResultWithTimeout() throws Exception {
        when(future1.get(timeout, timeUnit)).thenReturn(null);
        exception.expectMessage(equalTo("Task result is null"));
        exception.expect(PromiseBrokenException.class);
        wrapper.get(timeout, timeUnit);
    }

    @Test
    public void testGetFutureCancellationExceptionWithTimeout() throws Exception {
        when(future1.get(timeout, timeUnit)).thenThrow(CancellationException.class);
        exception.expect(PromiseBrokenException.class);
        wrapper.get(timeout, timeUnit);
    }

    @Test
    public void testGetFutureExecutionExceptionWithTimeout() throws Exception {
        when(future1.get(timeout, timeUnit)).thenThrow(ExecutionException.class);
        exception.expect(InterruptedException.class);
        wrapper.get(timeout, timeUnit);
    }

    @Test
    public void testGetSuccessCaseWithTimeout() throws Exception {
        ServiceResponse<String> response1 = new ServiceResponse<>("test", new HashMap<String, String>() {{
            put("header1", "value1");
        }});

        ServiceResponse<String> response2 = new ServiceResponse<>("test12", new HashMap<String, String>() {{
            put("header1", "value1");
        }});

        TaskResult<ServiceResponse> result1 = new TaskResult<>(true, "response", response1);
        TaskResult<ServiceResponse> result2 = new TaskResult<>(true, "response", response2);

        when(future1.get(timeout, timeUnit)).thenReturn(result1);
        when(future1.get()).thenReturn(result1);
        when(future2.get(timeout, timeUnit)).thenReturn(result2);

        String answer = (String) wrapper.get(timeout, timeUnit);
        assertEquals("test", answer);
        assertEquals(wrapper.getHeaders().get("header1"), "value1");
    }

    @Test
    public void testGetSuccessCaseWithMergerWithTimeout() throws Exception {
        ResponseMerger merger = mock(ResponseMerger.class);
        FutureTaskResultToDomainObjectPromiseWrapper wrapper = spy(new FutureTaskResultToDomainObjectPromiseWrapper(merger));
        wrapper.addFutureForTask(future1);
        wrapper.addFutureForTask(future2);
        ServiceResponse<String> response1 = new ServiceResponse<>("test", new HashMap<String, String>() {{
            put("header1", "value1");
        }});

        ServiceResponse<String> response2 = new ServiceResponse<>("test12", new HashMap<String, String>() {{
            put("header1", "value1");
        }});

        TaskResult<ServiceResponse> result1 = new TaskResult<>(true, "response", response1);
        TaskResult<ServiceResponse> result2 = new TaskResult<>(true, "response", response2);

        List<String> responses = new ArrayList<>();
        responses.add("test");
        responses.add("test12");
        when(merger.mergeResponse(responses)).thenReturn("test");

        when(future1.get(timeout, timeUnit)).thenReturn(result1);
        when(future1.get()).thenReturn(result1);
        when(future2.get(timeout, timeUnit)).thenReturn(result2);
        String answer = (String) wrapper.get(timeout, timeUnit);
        assertEquals("test", answer);
        assertEquals(wrapper.getHeaders().get("header1"), "value1");
        Mockito.verify(merger).mergeResponse(responses);
    }

    @Test
    public void testGetExceptionCaseWithTimeout() throws Exception {
        ServiceResponse response = new ServiceResponse(new ServiceClientException("test"), new HashMap<String, String>() {{
            put("header1", "value1");
        }});
        TaskResult<ServiceResponse> result = new TaskResult<>(true, "response", response);
        when(future1.get(timeout, timeUnit)).thenReturn(result);
        when(future1.get()).thenReturn(result);
        exception.expect(ServiceClientException.class);
        exception.expect(PromiseBrokenException.class);
        exception.expectMessage(equalTo("test"));
        assertEquals(wrapper.getHeaders().get("header1"), "value1");
        wrapper.get(timeout, timeUnit);
    }

    @Test
    public void testAddListeners() throws Exception {
        PromiseListener listener = mock(PromiseListener.class);
        exception.expect(UnsupportedOperationException.class);
        exception.expectMessage(equalTo("Adding listeners is not supported"));
        wrapper.addListener(listener);
    }

    @Test
    public void testGetSuccessCaseWithEmptyResponse() throws Exception {
        wrapper.addFutureForTask(future1);
        wrapper.addFutureForTask(future2);

        HashMap<String, String> headers = new HashMap<String, String>() {{
            put("header1", "value1");
        }};
        ServiceResponse<String> response1 = new ServiceResponse<>((String) null, headers);

        ServiceResponse<String> response2 = new ServiceResponse<>((String) null, headers);

        TaskResult<ServiceResponse> result1 = new TaskResult<>(true, "response", response1);
        TaskResult<ServiceResponse> result2 = new TaskResult<>(true, "response", response2);

        when(future1.get()).thenReturn(result1);
        when(future2.get()).thenReturn(result2);
        String answer = (String) wrapper.get();
        assertNull(answer);
    }
}