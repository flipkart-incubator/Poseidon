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

package com.flipkart.poseidon.internal;

import com.flipkart.hydra.composer.Composer;
import com.flipkart.hydra.composer.DefaultComposer;
import com.flipkart.hydra.composer.exception.ComposerEvaluationException;
import com.flipkart.hydra.composer.exception.ComposerInstantiationException;
import com.flipkart.hydra.dispatcher.Dispatcher;
import com.flipkart.hydra.dispatcher.exception.DispatchFailedException;
import com.flipkart.hydra.task.Task;
import flipkart.lego.concurrency.api.CompositeFuture;
import flipkart.lego.concurrency.api.NonBlockingDataSource;
import flipkart.lego.concurrency.api.Promise;
import flipkart.lego.concurrency.promises.FutureWrapperPromise;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class NonBlockingDispatcher implements Dispatcher {
    private final Map<String, Object> responses;
    private final Map<String, NonBlockingDataSource> datasources;
    private final Map<Future<Object>, String> futures;

    public NonBlockingDispatcher() {
        this.responses = new HashMap<>();
        this.datasources = new HashMap<>();
        this.futures = new HashMap<>();
    }

    @Override
    public Object execute(Map<String, Object> params, Map<String, Task> tasks, Object context) throws DispatchFailedException, ComposerEvaluationException {
        return execute(params, tasks, context, false);
    }

    @Override
    public Object execute(Map<String, Object> params, Map<String, Task> tasks, Object context, boolean isAlreadyParsed) throws DispatchFailedException, ComposerEvaluationException {
        try {
            DefaultComposer defaultComposer = new DefaultComposer(context, isAlreadyParsed);
            return execute(params, tasks, defaultComposer);
        } catch (ComposerInstantiationException e) {
            throw new DispatchFailedException("Unable to create composer.", e);
        }
    }

    @Override
    public Promise<Object> execute(Map<String, Object> params, Map<String, Task> tasks, Composer composer) throws DispatchFailedException, ComposerEvaluationException {
        responses.putAll(params);

        for (String key : tasks.keySet()) {
            Task task = tasks.get(key);
            List<String> dependencies = task.getDependencies();
            Map<String, Object> collectedDependencies = collectDependencies(dependencies);
            if (collectedDependencies.size() == dependencies.size()) {
                Future<Object> future = dispatchTask(key, task, collectedDependencies);
                futures.put(future, key);
            }
        }

        if (futures.isEmpty()) {
            throw new DispatchFailedException("No possible resolution of dependencies found.");
        }

        Collection<Future<Object>> futureCollection = futures.keySet();
        CompositeFuture<Object> compositeFuture = new CompositeFuture<>(futureCollection, true);
        return new FutureWrapperPromise<>(compositeFuture);
    }

    @Override
    public void shutdown() {
    }

    private Map<String, Object> collectDependencies(List<String> dependencies) {
        Map<String, Object> collectedDependencies = new HashMap<>();
        for (String dependency : dependencies) {
            if (responses.containsKey(dependency)) {
                collectedDependencies.put(dependency, responses.get(dependency));
            }
        }

        return collectedDependencies;
    }

    private Future<Object> dispatchTask(String key, Task task, Map<String, Object> responses) throws DispatchFailedException {
        try {
            Callable<Object> callable = task.getCallable(responses);
            if (! (callable instanceof NonBlockingDataSource)) {
                throw new UnsupportedOperationException("Async API should use only NonBlockingDataSource");
            }
            NonBlockingDataSource nbDataSource = (NonBlockingDataSource) callable;
            datasources.put(key, nbDataSource);
            return nbDataSource.call();
        } catch (Exception e) {
            throw new DispatchFailedException("Failed to dispatch task", e);
        }
    }

    public Object map(Composer composer) throws ComposerEvaluationException {
        futures.keySet().stream().forEach(future -> {
            try {
                String key = futures.get(future);
                NonBlockingDataSource nbDataSource = datasources.get(key);
                responses.put(key, nbDataSource.get());
            } catch (Exception e) {
                // TODO
            }
        });
        List<String> dependencies = composer.getDependencies();
        Map<String, Object> collectedDependencies = collectDependencies(dependencies);
        return composer.compose(collectedDependencies);
    }
}
