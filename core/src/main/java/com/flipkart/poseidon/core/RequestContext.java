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

package com.flipkart.poseidon.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Request level context where a request implies a single invocation of Lego. Implemented as a thread local which is
 * used to hold things like requestId, or other request level information to accessed by various elements in the call
 * stack.
 */

public class RequestContext {

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

    /**
     * initialize an empty request context, it will cleanup previous value of the threadlocal if used in a threadpool
     */
    public static void initialize() {
        if (isImmutable.get()) {
            throw new UnsupportedOperationException();
        }

        context.remove();
        isImmutable.set(false);
    }

    /**
     * initialize a new request context with the given context, it will cleanup previous value of
     * the threadlocal if it is being used in a threadpool
     *
     * @param ctxt Context map
     */
    public static void initialize(Map<String, Object> ctxt) {
        if (isImmutable.get()) {
            throw new UnsupportedOperationException();
        }

        context.remove();
        context.get().putAll(ctxt);

        isImmutable.set(true);
    }

    /**
     * Set's the value for a given key in the request context, this value will be accessible from the context
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
     * Get's the value for a given key from the request context.
     *
     * @param key Key to get
     * @return {@link Object} - Value for given key
     */
    public static <T> T get(String key) {
        return (T) context.get().get(key);
    }

    /**
     * Get's a snapshot of the request context as a a Map
     *
     * @return context map
     */
    public static Map<String, Object> getContextMap() {
        return new HashMap<>(context.get());
    }

    /**
     * Shuts down the request context by cleaning up the threadlocal.
     */
    public static void shutDown() {
        context.remove();
        isImmutable.remove();
    }
}
