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

package com.flipkart.poseidon.serviceclients;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This context is used to pass information specific to the service clients
 *
 * Created by shrey.garg on 06/04/16.
 */
public class ServiceContext {

    private static final ThreadLocal<Map<String, Object>> context = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };

    private static final ThreadLocal<Boolean> isImmutable = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private static final ThreadLocal<Boolean> isDebug = ThreadLocal.withInitial(() -> false);

    private static final ThreadLocal<Map<String, List<ServiceDebug>>> debugResponses = ThreadLocal.withInitial(ConcurrentHashMap::new);
    private static final ThreadLocal<Map<String, Queue<String>>> collectedHeaders = ThreadLocal.withInitial(HashMap::new);

    private static final ThreadLocal<Map<String, FanoutContext>> fanoutContext = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * initialize an empty service context, it will cleanup previous value of the threadlocal if used in a threadpool
     * @param responseHeadersToCollect
     */
    public static void initialize(List<String> responseHeadersToCollect) {
        if (isImmutable.get()) {
            throw new UnsupportedOperationException();
        }

        context.remove();
        isImmutable.set(false);

        if (responseHeadersToCollect == null || responseHeadersToCollect.isEmpty()) {
            return;
        }

        responseHeadersToCollect.forEach(header -> {
            collectedHeaders.get().put(header.toLowerCase(), new ConcurrentLinkedQueue<>());
        });
    }

    /**
     * initialize a new service context with the given context, it will cleanup previous value of
     * the threadlocal if it is being used in a threadpool
     *
     * @param state State of the previous thread's context
     */
    public static void initialize(ServiceContextState state) {
        if (isImmutable.get()) {
            throw new UnsupportedOperationException();
        }

        context.remove();
        context.get().putAll(state.ctxt);

        if (state.debug) {
            isDebug.set(state.debug);

            debugResponses.remove();
            debugResponses.set(state.serviceResponses);

        }

        fanoutContext.remove();
        fanoutContext.set(state.fanoutContext);
        collectedHeaders.remove();
        collectedHeaders.set(state.collectedHeaders);
        isImmutable.set(false);
    }

    /**
     * Set's the value for a given key in the service context, this value will be accessible from the context
     * using the get method.
     *
     * @param key Key to set
     * @param value Value to set
     */
    public static void set(String key, Object value) {
        if (isImmutable.get()) {
            throw new UnsupportedOperationException();
        }

        context.get().put(key, value);
    }

    /**
     * Enables the collection of service response in ServiceContext
     */
    public static void enableDebug() {
        if (isImmutable.get()) {
            throw new UnsupportedOperationException();
        }

        isDebug.set(true);
    }

    /**
     * Disables the collection of service response in ServiceContext
     */
    public static void disableDebug() {
        if (isImmutable.get()) {
            throw new UnsupportedOperationException();
        }

        isDebug.set(false);
    }

    public static boolean isDebug() {
        return isDebug.get();
    }

    public static void addDebugResponse(String key, ServiceDebug response) {
        if (!isDebug()) {
            return;
        }

        debugResponses.get().computeIfAbsent(key, k -> new ArrayList<>()).add(response);
    }

    public static Map<String, List<ServiceDebug>> getDebugResponses() {
        return debugResponses.get();
    }

    public static Map<String, FanoutContext> getFanoutContext(){
        return fanoutContext.get();
    }

    public static void addFanoutContext(String key, int count, long startTime, long endTime){
        fanoutContext.get().putIfAbsent(key, new FanoutContext());
        int newCount = fanoutContext.get().get(key).getCount() + count;
        int newTotalCount = fanoutContext.get().get(key).getTotalCount() + count;
        fanoutContext.get().get(key).getStarTime().add(startTime);
        fanoutContext.get().get(key).setCount(newCount);
        fanoutContext.get().get(key).setTotalCount(newTotalCount);
        fanoutContext.get().get(key).getEndTime().add(endTime);
    }
    /**
     * Get's the value for a given key from the service context.
     *
     * @param key Key to get
     * @return {@link Object} - Value for given key
     */
    public static <T> T get(String key) {
        return (T) context.get().get(key);
    }

    /**
     * Get's a snapshot of the service context as a a Map
     *
     * @return context map
     */
    public static Map<String, Object> getContextMap() {
        return new HashMap<>(context.get());
    }

    /**
     * Shuts down the service context by cleaning up the threadlocal.
     */
    public static void shutDown() {
        context.remove();
        isImmutable.remove();
        isDebug.remove();
        debugResponses.remove();
        fanoutContext.remove();
    }

    static Map<String, Queue<String>> getCollectedHeaders() {
        return collectedHeaders.get();
    }

    public static List<String> getCollectedHeaders(String header) {
        return Optional.ofNullable(getCollectedHeaders())
                .map(m -> m.get(header.toLowerCase()))
                .map(ArrayList::new)
                .orElseGet(null);
    }

    public static ServiceContextState getState() {
        ServiceContextState state = new ServiceContextState();
        state.ctxt = getContextMap();
        state.collectedHeaders = getCollectedHeaders();
        state.debug = isDebug();
        state.serviceResponses = getDebugResponses();
        state.fanoutContext = getFanoutContext();
        return state;
    }
}
