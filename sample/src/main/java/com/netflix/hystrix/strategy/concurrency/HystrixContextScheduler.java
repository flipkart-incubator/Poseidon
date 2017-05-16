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

package com.netflix.hystrix.strategy.concurrency;

import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.concurrent.Semaphore;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by mohan.pandian on 14/03/17.
 */
public class HystrixContextScheduler extends Scheduler {
    private final FiberScheduler fiberScheduler;
    private final Semaphore semaphore;

    public HystrixContextScheduler(FiberScheduler fiberScheduler, Semaphore semaphore) {
        if (fiberScheduler == null)
            throw new IllegalArgumentException("Fiber scheduler is null");
        this.fiberScheduler = fiberScheduler;
        this.semaphore = semaphore;
    }

    public HystrixContextScheduler(Semaphore semaphore) {
        this(DefaultFiberScheduler.getInstance(), semaphore);
    }

    @Override
    public Scheduler.Worker createWorker() {
        return new EventLoopScheduler(semaphore);
    }


    private class EventLoopScheduler extends Scheduler.Worker implements Subscription {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private final Semaphore semaphore;

        private EventLoopScheduler(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public Subscription schedule(final Action0 action) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return Subscriptions.empty();
            }

            if (!semaphore.tryAcquire()) {
                throw new RejectedExecutionException();
            }

            try {
                final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
                final Subscription s = Subscriptions.from(new Fiber<Void>(fiberScheduler, new SuspendableRunnable() {

                    @Override
                    public void run() throws SuspendExecution {
                        try {
                            if (innerSubscription.isUnsubscribed()) {
                                return;
                            }
                            action.call();
                        } finally {
                            // remove the subscription now that we're completed
                            Subscription s = sf.get();
                            if (s != null) {
                                innerSubscription.remove(s);
                            }
                        }
                    }
                }).start());

                sf.set(s);
                innerSubscription.add(s);
                return Subscriptions.create(new Action0() {

                    @Override
                    public void call() {
                        s.unsubscribe();
                        innerSubscription.remove(s);
                    }

                });
            } finally {
                semaphore.release();
            }
        }

        @Override
        public Subscription schedule(final Action0 action, final long delayTime, final TimeUnit unit) {
            throw new IllegalStateException("Hystrix does not support delayed scheduling");
        }

        @Override
        public void unsubscribe() {
            innerSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }
    }
}
