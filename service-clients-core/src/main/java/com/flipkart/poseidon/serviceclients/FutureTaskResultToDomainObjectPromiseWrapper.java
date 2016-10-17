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
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Created by mohan.pandian on 18/03/15.
 * <p/>
 * A Promise that wraps a Future <TaskResult>, typically returned by TaskContext.executeAsyncCommand()
 * and eventually delivers the data from TaskResult (DomainObject)
 */
public class FutureTaskResultToDomainObjectPromiseWrapper<DomainObject> implements Promise<DomainObject> {

    private final List<Future<TaskResult>> futureList = new ArrayList<>();
    private PromiseBrokenException promiseBrokenException;
    private ResponseMerger<DomainObject> responseMerger;

    public FutureTaskResultToDomainObjectPromiseWrapper(Future<TaskResult> future) {
        futureList.add(future);
    }

    public FutureTaskResultToDomainObjectPromiseWrapper(ResponseMerger<DomainObject> responseMerger) {
        this.responseMerger = responseMerger;
    }

    @Override
    public boolean isRealized() {
        for (Future<TaskResult> future : futureList) {
            if (!future.isDone()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isFullfilled() throws IllegalStateException {
        for (Future<TaskResult> future : futureList) {
            if (future.isCancelled()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isBroken() throws IllegalStateException {
        for (Future<TaskResult> future : futureList) {
            if (future.isCancelled()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void await() throws InterruptedException {
        try {
            for (Future<TaskResult> future : futureList) {
                future.get();
            }
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
            for (Future<TaskResult> future : futureList) {
                future.get(timeout, timeUnit);
            }
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
        try {
            ServiceResponse<DomainObject> serviceResponse = new ServiceResponse<>();
            for (Future<TaskResult> futureResult : futureList) {
                TaskResult result = futureResult.get();
                if (result == null) {
                    throw new PromiseBrokenException("Task result is null");
                }
                ServiceResponse<DomainObject> response = (ServiceResponse<DomainObject>) result.getData();
                if (!response.getIsSuccess())
                    throw response.getException();
                serviceResponse.addData(response.getDataList());
            }
            if (responseMerger != null) {
                return responseMerger.mergeResponse(serviceResponse.getDataList());
            } else {
                return serviceResponse.getDataList().get(0);
            }
        } catch (ExecutionException exception) {
            checkAndThrowServiceClientException(exception);
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
            ServiceResponse<DomainObject> serviceResponse = new ServiceResponse<>();
            for (Future<TaskResult> futureResult : futureList) {
                TaskResult result = futureResult.get(timeout, timeUnit);
                if (result == null) {
                    throw new PromiseBrokenException("Task result is null");
                }
                ServiceResponse<DomainObject> response = (ServiceResponse<DomainObject>) result.getData();
                if (!response.getIsSuccess())
                    throw response.getException();
                serviceResponse.addData(response.getDataList());
            }
            if (responseMerger != null) {
                return responseMerger.mergeResponse(serviceResponse.getDataList());
            } else {
                return serviceResponse.getDataList().get(0);
            }
        } catch (ExecutionException exception) {
            checkAndThrowServiceClientException(exception);
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
            taskResult = futureList.get(0).get();
            if (taskResult == null) {
                throw new PromiseBrokenException("Task result is null");
            }

            ServiceResponse<DomainObject> response = (ServiceResponse<DomainObject>) taskResult.getData();
            return response.getHeaders();
        } catch (ExecutionException exception) {
            checkAndThrowServiceClientException(exception);
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new InterruptedException(exception.getMessage());
        } catch (CancellationException exception) {
            promiseBrokenException = new PromiseBrokenException(exception);
            throw new PromiseBrokenException(promiseBrokenException);
        }
    }

    @Override
    public synchronized void addListener(PromiseListener promiseListener) {
        throw new UnsupportedOperationException("Adding listeners is not supported");
    }

    public void addFutureForTask(Future<TaskResult> future) {
        futureList.add(future);
    }

    public void addFutureForTask(List<Future<TaskResult>> future) {
        futureList.addAll(future);
    }

    public List<Future<TaskResult>> getFutureList() {
        return futureList;
    }

    /**
     * If root cause of ExecutionException is ServiceClientException, just throw it.
     * Ex: ServiceResponseDecoder throwing ServiceClientException for 5xx responses.
     *
     * @param exception
     * @throws PromiseBrokenException
     */
    private void checkAndThrowServiceClientException(ExecutionException exception) throws ServiceClientException {
        Throwable generatedException = Optional.ofNullable(ExceptionUtils.getRootCause(exception)).orElse(exception);
        if (generatedException instanceof ServiceClientException) {
            throw (ServiceClientException) generatedException;
        }
    }
}
