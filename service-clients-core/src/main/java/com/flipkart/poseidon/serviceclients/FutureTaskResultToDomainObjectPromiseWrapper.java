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
import flipkart.lego.concurrency.api.Promise;
import flipkart.lego.concurrency.api.PromiseListener;
import flipkart.lego.concurrency.exceptions.PromiseBrokenException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by mohan.pandian on 18/03/15.
 *
 * A Promise that wraps a Future <TaskResult>, typically returned by TaskContext.executeAsyncCommand()
 * and eventually delivers the data from TaskResult (DomainObject)
 */
public class FutureTaskResultToDomainObjectPromiseWrapper<DomainObject> implements Promise<DomainObject> {

    private final Future<TaskResult> future;
    private final List<Future<TaskResult>> futureList = new ArrayList<>();
    private final List<PromiseListener> promiseListeners = new ArrayList<>();
    private PromiseBrokenException promiseBrokenException;
    private ResponseMerger<DomainObject> responseMerger;

    public FutureTaskResultToDomainObjectPromiseWrapper(Future<TaskResult> future) {
        this.future = future;
    }

    public FutureTaskResultToDomainObjectPromiseWrapper(ResponseMerger<DomainObject> responseMerger){
        this.future = null;
        this.responseMerger = responseMerger;
    }

    public FutureTaskResultToDomainObjectPromiseWrapper() {
        future = null;
    }

    @Override
    public boolean isRealized() {
        return future.isDone();
    }

    @Override
    public boolean isFullfilled() throws IllegalStateException {
        return !future.isCancelled();
    }

    @Override
    public boolean isBroken() throws IllegalStateException {
        return future.isCancelled();
    }

    @Override
    public void await() throws InterruptedException {
        try {
            future.get();
            triggerListeners();
        } catch (ExecutionException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new InterruptedException(exception.getMessage());
        } catch (CancellationException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
        }

    }

    @Override
    public void await(long timeout, TimeUnit timeUnit) throws InterruptedException {
        try {
            future.get(timeout, timeUnit);
            triggerListeners();
        } catch (ExecutionException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new InterruptedException(exception.getMessage());
        } catch (CancellationException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
        } catch (TimeoutException ignored) {
        }
    }

    @Override
    public DomainObject get() throws PromiseBrokenException, InterruptedException {
        if (responseMerger != null) {
            return get(responseMerger);
        }
        try {
            TaskResult taskResult = future.get();
            if (taskResult == null) {
                throw new PromiseBrokenException("Task result is null");
            }
            triggerListeners();

            ServiceResponse<DomainObject> response = (ServiceResponse<DomainObject>) taskResult.getData();
            if (!response.getIsSuccess())
                throw response.getException();

            return response.getData();
        } catch (ExecutionException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new InterruptedException(exception.getMessage());
        } catch (CancellationException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new PromiseBrokenException(promiseBrokenException);
        }
    }

    private DomainObject get(ResponseMerger<DomainObject> merger) throws PromiseBrokenException, InterruptedException {
        try {
            ServiceResponse<DomainObject> response = new ServiceResponse<>();
            for (Future<TaskResult> futureResult : futureList) {
                TaskResult result = futureResult.get();
                if (result == null) {
                    throw new PromiseBrokenException("Task result is null");
                }
                triggerListeners();
                response.addData(((ServiceResponse<DomainObject>) result.getData()).getData());
            }
            return merger.mergeResponse(response.returnDataList());
        } catch (ExecutionException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new InterruptedException(exception.getMessage());
        } catch (CancellationException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new PromiseBrokenException(promiseBrokenException);
        }
    }

    public Map<String, String> getHeaders() throws PromiseBrokenException, InterruptedException {
        try {
            TaskResult taskResult;
            if (futureList.isEmpty()) {
                taskResult = future.get();
            } else {
                taskResult = futureList.get(0).get();
            }

            if (taskResult == null) {
                throw new PromiseBrokenException("Task result is null");
            }

            ServiceResponse<DomainObject> response = (ServiceResponse<DomainObject>) taskResult.getData();
            return response.getHeaders();
        } catch (ExecutionException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new InterruptedException(exception.getMessage());
        } catch (CancellationException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new PromiseBrokenException(promiseBrokenException);
        }
    }

    @Override
    public DomainObject get(long timeout, TimeUnit timeUnit) throws PromiseBrokenException, TimeoutException, InterruptedException {
        try {
            TaskResult taskResult = future.get(timeout, timeUnit);
            if (taskResult == null) {
                throw new PromiseBrokenException("Task result is null");
            }
            triggerListeners();

            ServiceResponse<DomainObject> response = (ServiceResponse<DomainObject>) taskResult.getData();
            if (!response.getIsSuccess())
                throw response.getException();

            return response.getData();
        } catch (ExecutionException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new InterruptedException(exception.getMessage());
        } catch (CancellationException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new PromiseBrokenException(promiseBrokenException);
        }
    }

    @Override
    public synchronized void addListener(PromiseListener promiseListener) {
        if (isRealized()) {
            triggerListener(promiseListener);
        } else {
            promiseListeners.add(promiseListener);
        }
    }

    //synchronized code block to avoid race conditions when adding listener
    private synchronized void triggerListeners() {
        for (PromiseListener promiseListener : promiseListeners) {
            triggerListener(promiseListener);
        }
    }

    private void triggerListener(PromiseListener promiseListener) {
        if (isFullfilled()) {
            try {
                promiseListener.whenFullfilled(future.get());
            } catch (Exception ignored) {
            }
        } else {
            promiseListener.whenBroken(promiseBrokenException);
        }
    }

    public void addFutureForTask(Future<TaskResult> future) {
        futureList.add(future);
    }

    public Future<TaskResult> getFuture() {
        return future;
    }
}
