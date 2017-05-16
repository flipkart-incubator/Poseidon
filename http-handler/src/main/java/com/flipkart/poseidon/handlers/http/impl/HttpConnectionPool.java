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

package com.flipkart.poseidon.handlers.http.impl;

import co.paralleluniverse.fibers.httpasyncclient.FiberCloseableHttpAsyncClient;
import com.flipkart.poseidon.handlers.http.HttpDelete;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.protocol.HttpContext;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class HttpConnectionPool {

    /**
     * Defaults
     */
    private static final Integer defaultMaxConnections = 120;
    private static final Integer defaultProcessQueueSize = 100;
    private static final Integer defaultConnectionTimeout = 0;
    private static final Integer defaultOperationTimeout = 0;
    private static final Boolean defaultSecure = false;
    private static final Integer defaultPort = 80;

    /* Strings */
    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final String COMPRESSION_TYPE = "gzip";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";

    /**
     * socket connection timeToLive in seconds
     */
    private int timeToLiveInSecs = -1;

    private static final Logger logger = LogFactory.getLogger(HttpConnectionPool.class);

    /**
     * The variables holding the Pool details
     */
    private String name;
    private CloseableHttpAsyncClient client;
    private String host;
    private Integer port;
    private Boolean secure;
    private Map<String, String> headers;
    private Semaphore processQueue;
    private boolean requestGzipEnabled;
    private boolean responseGzipEnabled;
    private PoolingNHttpClientConnectionManager connectionManager;
    private DummyFutureCallback futureCallback;
    private static final ThreadFactory threadFactory;

    static {
        threadFactory = new ThreadFactoryBuilder().setNameFormat("reactor-%d").setUncaughtExceptionHandler((Thread t, Throwable e) ->
                logger.error("Uncaught exception in thread {} - {}", t.getName(), e.getMessage())).build();
    }


    /**
     * Add a value to headers
     */
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * Constructor
     *
     * @param host              Host Name
     * @param port              Port Name
     * @param secure
     * @param connectionTimeout
     * @param operationTimeout
     * @param maxConnections
     * @param processQueueSize
     * @param timeToLiveInSecs
     */
    protected HttpConnectionPool(final String name, String host, Integer port, Boolean secure, Integer connectionTimeout,
                                 Integer operationTimeout, Integer maxConnections, Integer processQueueSize,
                                 Integer timeToLiveInSecs) throws Exception {
        this.name = name;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.headers = new HashMap<String, String>();
        this.processQueue = new Semaphore(processQueueSize + maxConnections);
        if (timeToLiveInSecs != null) {
            this.timeToLiveInSecs = timeToLiveInSecs;
        }
        this.requestGzipEnabled = false;
        this.responseGzipEnabled = false;


        RegistryBuilder<SchemeIOSessionStrategy> registryBuilder = RegistryBuilder.create();
        if (this.secure) {
            registryBuilder.register("https", SSLIOSessionStrategy.getDefaultStrategy());
        } else {
            registryBuilder.register("http", NoopIOSessionStrategy.INSTANCE);
        }

        IOReactorConfig reactorConfig = IOReactorConfig.custom().
                setConnectTimeout(connectionTimeout).
                setSoTimeout(operationTimeout).
                setIoThreadCount(1).
                build();

        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(reactorConfig);
        connectionManager = new PoolingNHttpClientConnectionManager(ioReactor, null, registryBuilder.build(), null, null, timeToLiveInSecs, TimeUnit.SECONDS);
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnections);
        connectionManager.setMaxPerRoute(new HttpRoute(new HttpHost(host, port)), maxConnections);


        HttpAsyncClientBuilder httpAsyncClientBuilder = HttpAsyncClientBuilder.create().
                disableCookieManagement().
                setThreadFactory(threadFactory).
                setConnectionManager(connectionManager);

        httpAsyncClientBuilder.addInterceptorLast((HttpRequest httpRequest, HttpContext httpContext) -> {
            if (isResponseGzipEnabled() && !httpRequest.containsHeader(ACCEPT_ENCODING)) {
                httpRequest.addHeader(ACCEPT_ENCODING, COMPRESSION_TYPE);
            }
        });
        /*
        Apache async http client doesn't support compression
        http://httpclient-users.hc.apache.narkive.com/HTCFbPfx/httpasyncclient-basicasyncrequestproducer-prevents-httprequestinterceptor-from-replacing-httpentity-
        Hence we manually decompress after executing request. Check execute(HttpRequestBase request) below.
        This looks better too as decompression won't block IO dispatcher threads
        httpAsyncClientBuilder.addInterceptorFirst((HttpResponse httpResponse, HttpContext httpContext) -> {
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                Header ceheader = entity.getContentEncoding();
                if (ceheader != null) {
                    HeaderElement[] codecs = ceheader.getElements();
                    for (int i = 0; i < codecs.length; i++) {
                        if (codecs[i].getName().equalsIgnoreCase(COMPRESSION_TYPE)) {
                            httpResponse.setEntity(new GzipDecompressingEntity(httpResponse.getEntity()));
                            return;
                        }
                    }
                }
            }
        });
        */
        futureCallback = new DummyFutureCallback();

        this.client = FiberCloseableHttpAsyncClient.wrap(httpAsyncClientBuilder.build());
        this.client.start();


    }


    /**
     * Builds a pool from params
     *
     * @param params
     * @return a new HttpConnectionPool
     * @throws Exception, in case of errors
     */
    public static HttpConnectionPool build(Map<String, String> params) throws Exception {
        String host = params.get("host");
        if (host == null) {
            throw new Exception("host not specified");
        }

        String name = params.get("name");
        Integer port = params.get("port") != null ? Integer.parseInt(params.get("port")) : defaultPort;
        Integer connectionTimeout = params.get("connectionTimeout") != null ? Integer.parseInt(params.get("connectionTimeout")) : defaultConnectionTimeout;
        Integer operationTimeout = params.get("operationTimeout") != null ? Integer.parseInt(params.get("operationTimeout")) : defaultOperationTimeout;
        Integer maxConnections = params.get("maxConnections") != null ? Integer.parseInt(params.get("maxConnections")) : defaultMaxConnections;
        Integer timeToLiveInSecs = params.get("timeToLiveInSecs") != null ? Integer.parseInt(params.get("timeToLiveInSecs")) : -1;
        Boolean secure = params.get("secure") != null ? Boolean.parseBoolean(params.get("secure")) : defaultSecure;
        Integer processQueueSize = defaultProcessQueueSize;
        try {
            processQueueSize = Integer.parseInt(params.get("processQueueSize"));
        } catch (Exception e) {
        }
        //TODO: Exception quietly consumed
        return new HttpConnectionPool(name, host, port, secure, connectionTimeout, operationTimeout, maxConnections, processQueueSize, timeToLiveInSecs);
    }

    /**
     * Method for executing HTTP GET request
     */
    public HttpResponse doGET(String uri, Map<String, String> requestHeaders) throws Exception {
        HttpGet request = new HttpGet(constructUrl(uri));
        setRequestHeaders(request, requestHeaders);
        return execute(request);
    }

    /**
     * Method for executing HTTP PUT request
     */
    public HttpResponse doPUT(String uri, byte[] data, Map<String, String> requestHeaders) throws Exception {
        HttpPut request = new HttpPut(constructUrl(uri));
        if (data != null) {
            if (this.requestGzipEnabled) {
                request.addHeader(CONTENT_ENCODING, COMPRESSION_TYPE);
                request.setEntity(new GzipCompressingEntity(new ByteArrayEntity(data)));
            } else {
                request.setEntity(new ByteArrayEntity(data));
            }
        }
        setRequestHeaders(request, requestHeaders);
        return execute(request);
    }

    /**
     * Method for executing HTTP POST request
     */
    public HttpResponse doPOST(String uri, byte[] data, Map<String, String> requestHeaders) throws Exception {
        HttpPost request = new HttpPost(constructUrl(uri));
        if (data != null) {
            if (this.requestGzipEnabled) {
                request.addHeader(CONTENT_ENCODING, COMPRESSION_TYPE);
                request.setEntity(new GzipCompressingEntity(new ByteArrayEntity(data)));
            } else {
                request.setEntity(new ByteArrayEntity(data));
            }
        }
        setRequestHeaders(request, requestHeaders);
        return execute(request);
    }

    /**
     * Method for executing HTTP POST request with form params
     */
    public HttpResponse doPOST(String uri, List<NameValuePair> formParams, Map<String, String> requestHeaders) throws Exception {
        HttpPost request = new HttpPost(constructUrl(uri));
        if (this.requestGzipEnabled) {
            request.addHeader(CONTENT_ENCODING, COMPRESSION_TYPE);
            request.setEntity(new GzipCompressingEntity(new UrlEncodedFormEntity(formParams)));
        } else {
            request.setEntity(new UrlEncodedFormEntity(formParams));
        }
        setRequestHeaders(request, requestHeaders);
        return execute(request);
    }

    /**
     * Method for executing HTTP DELETE request
     */
    public HttpResponse doDELETE(String uri, Map<String, String> requestHeaders) throws Exception {
        HttpDelete request = new HttpDelete(constructUrl(uri));
        setRequestHeaders(request, requestHeaders);
        return execute(request);
    }

    /**
     * Method to execute a request
     */
    public HttpResponse execute(HttpRequestBase request) throws Exception {
        if (processQueue.tryAcquire()) {
            HttpResponse response;
            try {
                // Inject timestamp in milliseconds just before sending request on wire.
                // This will help in measuring latencies between client and server.
                if (request.getHeaders(TIMESTAMP_HEADER).length == 0) {
                    request.addHeader(TIMESTAMP_HEADER, String.valueOf(System.currentTimeMillis()));
                }
                response = client.execute(request, futureCallback).get();
                if (isResponseGzipEnabled() && response != null && response.getEntity() != null) {
                    response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                }
            } catch (Exception e) {
                logger.error("{}. Permits: {}. Exception: {}", connectionManager.getTotalStats(), processQueue.availablePermits(), e.getMessage());
                throw e;
            } finally {
                processQueue.release();
            }
            return response;
        } else {
            throw new Exception("PROCESS_QUEUE_FULL POOL:" + name);
        }
    }

    /**
     * Getter/Setter methods
     */
    private void setRequestHeaders(HttpRequestBase request, Map<String, String> headers) {
        Map<String, String> requestHeaders = getRequestHeaders(headers);
        for (String key : requestHeaders.keySet()) {
            request.addHeader(key, requestHeaders.get(key));
        }
    }


    private Map<String, String> getRequestHeaders(Map<String, String> headers) {
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.putAll(this.headers);
        if (headers != null) {
            requestHeaders.putAll(headers);
        }
        return requestHeaders;
    }

    /** End Getter/Setter methods */

    /**
     * Helper method to construct a URL
     */
    private String constructUrl(String uri) {
        if (port == 80) {
            return "http" + (secure ? "s" : "") + "://" + host + uri;
        }
        return "http" + (secure ? "s" : "") + "://" + host + ":" + port + uri;
    }


    /**
     * Method to create HttpRequest
     */

    public HttpRequestBase createHttpRequest(String uri, byte[] data, Map<String, String> requestHeaders, String requestType) {
        if ("GET".equals(requestType)) {
            HttpGet request = new HttpGet(constructUrl(uri));
            setRequestHeaders(request, requestHeaders);
            return request;
        } else if ("POST".equals(requestType)) {
            HttpPost request = new HttpPost(constructUrl(uri));
            setRequestBody(request, data);
            setRequestHeaders(request, requestHeaders);
            return request;

        } else if ("PUT".equals(requestType)) {
            HttpPut request = new HttpPut(constructUrl(uri));
            setRequestBody(request, data);
            setRequestHeaders(request, requestHeaders);
            return request;

        } else if ("DELETE".equals(requestType)) {
            HttpDelete request = new HttpDelete(constructUrl(uri));
            setRequestBody(request, data);
            setRequestHeaders(request, requestHeaders);
            return request;
        } else if ("PATCH".equals(requestType)) {
            HttpPatch request = new HttpPatch(constructUrl(uri));
            setRequestBody(request, data);
            setRequestHeaders(request, requestHeaders);
            return request;
        } else {
            HttpRequestBase request = null;
            logger.error("Invalid requestType+:" + requestType);
            return request;
        }
    }

    /**
     * @param request
     * @param data
     */
    private void setRequestBody(HttpEntityEnclosingRequestBase request, byte[] data) {
        if (data != null) {
            if (this.requestGzipEnabled) {
                request.addHeader(CONTENT_ENCODING, COMPRESSION_TYPE);
                request.setEntity(new GzipCompressingEntity(new ByteArrayEntity(data)));
            } else {
                request.setEntity(new ByteArrayEntity(data));
            }
        }
    }

    public boolean isRequestGzipEnabled() {
        return requestGzipEnabled;
    }

    public void setRequestGzipEnabled(boolean requestGzipEnabled) {
        this.requestGzipEnabled = requestGzipEnabled;
    }

    public boolean isResponseGzipEnabled() {
        return responseGzipEnabled;
    }

    public void setResponseGzipEnabled(boolean responseGzipEnabled) {
        this.responseGzipEnabled = responseGzipEnabled;
    }
}

class DummyFutureCallback implements FutureCallback<HttpResponse> {

    @Override
    public void completed(HttpResponse o) {
    }

    @Override
    public void failed(Exception e) {
    }

    @Override
    public void cancelled() {
    }
}