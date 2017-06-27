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

package com.flipkart.poseidon;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import flipkart.lego.concurrency.executors.CompositeCompletionService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by mohan.pandian on 15/03/17.
 */
public class CompositeFiberCompletionService implements CompositeCompletionService {
    private final Channel<Future> channel = Channels.newChannel(-1);

    @Override
    @Suspendable
    public Future submit(Callable callable) {
        return new Fiber() {
            @Override
            protected Object run() throws SuspendExecution, InterruptedException {
                try {
                    return callable.call();
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    System.out.println("Exception in CFCS: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                } finally {
                    channel.send(this);
                }
            }
        }.start();
    }

    @Override
    @Suspendable
    public List<Future> submit(List<Callable> callables) {
        List<Future> futures = new ArrayList<>();
        for (Callable callable : callables) {
            futures.add(submit(callable));
        }
        return futures;
    }

    @Override
    @Suspendable
    public void wait(List<Future> futures, long timeout) throws TimeoutException, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < futures.size(); i++) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;
            long remainingTime = timeout - elapsedTime;
            if (remainingTime > 0) {
                try {
                    Future future = channel.receive(remainingTime, TimeUnit.MILLISECONDS);
                    if (future == null) {
                        throw new TimeoutException();
                    }
                    future.get();
                } catch (SuspendExecution suspendExecution) {
                    suspendExecution.printStackTrace();
                }
            }
        }
    }

    public void shutdown() {
        channel.close();
    }
}
