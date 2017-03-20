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
import com.netflix.hystrix.HystrixThreadPool;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by mohan.pandian on 14/03/17.
 */
public class HystrixContextScheduler extends Scheduler {
    private final FiberScheduler fiberScheduler;

    public HystrixContextScheduler(FiberScheduler fiberScheduler) {
        if (fiberScheduler == null)
            throw new IllegalArgumentException("Fiber scheduler is null");
        this.fiberScheduler = fiberScheduler;
    }

    public HystrixContextScheduler(HystrixConcurrencyStrategy concurrencyStrategy, HystrixThreadPool threadPool, Func0<Boolean> shouldInterruptThread) {
        this();
    }

    private HystrixContextScheduler() {
        this(DefaultFiberScheduler.getInstance());
    }

    @Override
    public Scheduler.Worker createWorker() {
        return new EventLoopScheduler();
    }


    private class EventLoopScheduler extends Scheduler.Worker implements Subscription {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();

        private EventLoopScheduler() {
        }

        @Override
        public Subscription schedule(final Action0 action) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return Subscriptions.empty();
            }

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
        }

        @Override
        public Subscription schedule(final Action0 action, final long delayTime, final TimeUnit unit) {
            final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();

            final Subscription s = Subscriptions.from(new Fiber<Void>(fiberScheduler, new SuspendableRunnable() {

                @Override
                public void run() throws InterruptedException, SuspendExecution {
                    Fiber.sleep(delayTime, unit);
                    try {
                        if (innerSubscription.isUnsubscribed()) {
                            return;
                        }
                        // now that the delay is past schedule the work to be done for real on the UI thread
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
