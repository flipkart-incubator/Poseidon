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
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Created by mohan.pandian on 12/03/17.
 */
public class FiberCompletionService<T> {
    private final Channel<Future<T>> channel = Channels.newChannel(-1);

    @Suspendable
    public Future<T> submit(Callable<T> callable) {
        return new Fiber<T>() {
            @Override
            protected T run() throws SuspendExecution, InterruptedException {
                try {
                    return callable.call();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    channel.send(this);
                }
                return null;
            }
        }.start();
    }

    @Suspendable
    public Future<T> take() throws InterruptedException, SuspendExecution {
        return channel.receive();
    }

    public void shutdown() {
        channel.close();
    }
}
