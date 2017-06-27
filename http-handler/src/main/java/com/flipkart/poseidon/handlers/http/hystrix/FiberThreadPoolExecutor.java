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

package com.flipkart.poseidon.handlers.http.hystrix;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

import java.util.concurrent.*;

/**
 * Created by mohan.pandian on 06/03/17.
 */
public class FiberThreadPoolExecutor extends ThreadPoolExecutor {
    public FiberThreadPoolExecutor() {
        this(20, 20, 2000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(20));
    }

    public FiberThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public FiberThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public FiberThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public FiberThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void execute(Runnable command) {
        new Fiber<Void>() {
            @Override
            protected Void run() throws SuspendExecution {
                command.run();
                return null;
            }
        }.start();
    }

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
                    System.out.println("Exception in FTPE: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                } finally {
                }
            }
        }.start();
    }
}
