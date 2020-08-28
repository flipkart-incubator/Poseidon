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

package com.flipkart.poseidon.api;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.session.SessionCacheFactory;

import java.util.List;

/**
 * Configuration to be provided by application to tune jetty.
 *
 * Created by mohan.pandian on 10/02/16.
 */
public interface JettyConfiguration {

    /*
     * Number of threads dedicated to accepting incoming connections. Can be >= 1 and <= number of processors.
     * Return -1 to use jetty's default logic of determining number of acceptors based on available processors
     */
    int getAcceptors();

    /*
     * Number of threads that look after the activity on a socket. Can be >= 1 and <= number of processors.
     * Return -1 to use jetty's default logic of determining number of selectors based on available processors
     */
    int getSelectors();

    /*
     * Number of connection requests that can be queued up before the operating system starts to send rejections.
     * Return -1 to use jetty/OS default
     */
    int getAcceptQueueSize();

    /*
     * Bounded task queue size - https://wiki.eclipse.org/Jetty/Howto/High_Load#Thread_Pool. By default, jetty uses
     * an unbounded task queue which is quite dangerous from recoverability aspect
     */
    int getTaskQueueSize();

    /*
     * Minimum number of threads including acceptors, selectors and worker threads. Even after threadIdelTimeout
     * jetty will retain these many threads. Jetty's default is 8
     */
    int getMinThreads();

    /*
     * Maximum numer of threads including acceptors, selectors and worker threads. After threadIdelTimeout jetty
     * may stop these threads. Jetty's default is 200
     */
    int getMaxThreads();

    /*
     * Threads that are idle for longer than this period may be stopped. Timeout in milli seconds. Jetty's default
     * is 60000 ms / 1 minute
     */
    int getThreadIdleTimeout();

    /*
     * Filters that the application would like to be executed in the jetty thread
     */
    default List<JettyFilterConfiguration> getJettyFilterConfigurations() {
        return null;
    }

    /*
     * Allows configuration of jetty's connection factory
     */
    default HttpConnectionFactory getHttpConnectionFactory() {
        return null;
    }

    /*
     * Session Id manager which manages the session. This is required if session has to be managed in the request lifecycle
     * Default is null which means session is not managed.
     */
    default SessionIdManager getSessionIdManager(Server server) {
        return null;
    }

    /*
     * Session Cache factory which will be responsible to provide the session cache class.
     * Default is null which means jetty will use its default (DefaultSessionCacheFactory).
     */
    default SessionCacheFactory getSessionCacheFactory() {
        return null;
    }
}
