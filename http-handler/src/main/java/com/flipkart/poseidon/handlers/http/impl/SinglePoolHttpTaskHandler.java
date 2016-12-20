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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.phantom.task.impl.RequestCacheableHystrixTaskHandler;
import com.flipkart.phantom.task.spi.TaskRequestWrapper;
import com.flipkart.phantom.task.spi.TaskResult;
import com.flipkart.phantom.task.spi.Decoder;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.poseidon.handlers.http.HttpResponseDecoder;
import com.flipkart.poseidon.handlers.http.HttpResponseData;
import com.google.common.base.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.hash.Hashing.murmur3_32;

import static com.flipkart.poseidon.handlers.http.HandlerConstants.HTTP_HEADERS;
import static com.flipkart.poseidon.handlers.http.HandlerConstants.HTTP_METHOD;
import static com.flipkart.poseidon.handlers.http.HandlerConstants.HTTP_URI;
import static com.flipkart.poseidon.handlers.http.HandlerConstants.X_CACHE_REQUEST;


public class SinglePoolHttpTaskHandler extends RequestCacheableHystrixTaskHandler {

    /** Log instance of this class */
    private static final Logger logger = LogFactory.getLogger(SinglePoolHttpTaskHandler.class);
    /*
    * Making this static so that only single instance exists!
     */
    protected static ObjectMapper objectMapper = new ObjectMapper();

    /** Invalid String */
    private final String invalid = "INVALID";

    /** extra time above the socket timeout given for execution in miliseconds */
    private final int extraExecutionTime = 100;

    /** command prefix - command is constructed as poolname + prefix */
    public final String commandPrefix = "HttpRequest";

    /** default handler, pool, command names - these must be overriden in beans */
    public String poolName = invalid;

    /** default parameters for pool creation */
    public String host = "localhost";
    public int port = 80;
    public int connectionTimeout = 1000;
    public int operationTimeout = 1000;
    public int queueSize = 0;
    private int timeToLiveInSecs= -1;
    public boolean isSecure = false;
    public String accept = "application/json";
    public String contentType = "application/json";
    public boolean requestCompressionEnabled = false;
    public boolean responseCompressionEnabled = false;
    private boolean requestCachingEnabled = false;

    private final static String colon = ":";

    /** default parameters for async Http Execution */

    public boolean enableAsyncExecution =  false;

    /** the connection pool which handles the requests */
    private HttpConnectionPool pool;

    /** interface method implementation */
    @Override
    public String getName() {
        return poolName + "Http";
    }

    /** interface method implementation */
    @Override
    public String[] getCommands() {
        return new String[]{poolName + commandPrefix};
    }

    /** interface method implementation */
    @Override
    public void init(TaskContext taskContext) throws Exception {

        // make sure a poolname is specified
        if (invalid.equals(poolName)) {
            throw new Exception("Invalid pool name specified");
        }

        // Calculate the maxConnections from poolSize params.
        int maxConnections = 10;
        if(getConcurrentPoolSizeParams() != null && !getConcurrentPoolSizeParams().isEmpty()) {
            maxConnections = 0;
            for(Integer poolSize: getConcurrentPoolSizeParams().values()) {
                maxConnections += poolSize;
            }
        }

        // create the pool object
        pool = new HttpConnectionPool(poolName,host,port,isSecure,connectionTimeout,operationTimeout + extraExecutionTime,
                maxConnections,queueSize, timeToLiveInSecs);

        // set appropriate headers
        if (!accept.isEmpty()) pool.setHeader("Accept", accept);
        if (!contentType.isEmpty()) pool.setHeader("Content-Type", contentType);
        pool.setRequestGzipEnabled(isRequestCompressionEnabled());
        pool.setResponseGzipEnabled(isResponseCompressionEnabled());

        // set executor timeouts to a little more than the operation timeout
        executorTimeouts.put(poolName + commandPrefix, operationTimeout);
    }

    @Override
    public <T, S> TaskResult<T> execute(TaskContext taskContext, String command,
                                     TaskRequestWrapper<S> taskRequestWrapper,Decoder<T> decoder) throws RuntimeException {
        Map<String, Object> params = taskRequestWrapper.getParams();
        byte[] data = (byte[]) taskRequestWrapper.getData();

        Map<String,String> requestHeaders = getRequestHeaders(params);
        // Ingest phantom provided headers like zipkin etc
        if (taskRequestWrapper.getHeaders().isPresent()) {
            taskRequestWrapper.getHeaders().get().stream().forEach(entry -> requestHeaders.put(entry.getKey(), entry.getValue()));
        }

        try {
            HttpRequestBase request = this.pool.createHttpRequest((String) params.get(HTTP_URI), data, requestHeaders, (String) params.get(HTTP_METHOD));
            HttpResponse httpResponse =  this.pool.execute(request);

            return  new TaskResult<T>(true, null, ((HttpResponseDecoder<T>) decoder).decode(httpResponse));
        }
        catch (Exception e) {
            handleException(e, (String) params.get(HTTP_URI), (String) params.get(HTTP_METHOD));
        }
        return null;
    }

    /**
     * This is a common Http Task Handler used across the website. If you are making changes in this code and have no idea why,
     * Hell Hath No Fury For you!!
     *
     * <b>HIGHLY SENSITIVE CODE. </b>
     *
     * @param taskContext
     * @param command
     * @param params
     * @param data
     * @return
     */
    @Override
    public <T, S> TaskResult<T> execute(TaskContext taskContext, String command, Map<String, Object> params, S data) {

        Map<String,String> requestHeaders = getRequestHeaders(params);

        if(params.get("executeAsync") != null && Boolean.parseBoolean((String) params.get("executeAsync"))){
            return processAsyncHttpRequest(taskContext,command,params,(byte[]) data);
        }
        TaskResult result;
        try {
            result  = handleHttpResponse(makeRequest((String) params.get(HTTP_METHOD), (String) params.get(HTTP_URI), (byte[]) data, requestHeaders));
        }
        catch (Exception e) {
            result =  handleException(e, (String) params.get(HTTP_URI), (String) params.get(HTTP_METHOD));
        }
        return result;
    }

    @Override
    public <S> String getCacheKey(Map<String, String> requestParams, S data) {
        if (!requestCachingEnabled || requestParams == null) {
            return null;
        }

        boolean hasURI = requestParams.containsKey(HTTP_URI);
        boolean isGet = "GET".equalsIgnoreCase(requestParams.get(HTTP_METHOD));
        boolean askedTobeCached = "true".equalsIgnoreCase(requestParams.get(X_CACHE_REQUEST));
        if (!hasURI || (!isGet && !askedTobeCached)) {
            return null;
        }

        String cacheKey = requestParams.get(HTTP_URI) + (data != null ? new String((byte[]) data) : "");
        return murmur3_32().hashString(cacheKey, Charsets.UTF_16LE).toString();
    }

    /** interface method implementation */
    @Override
    public <T, S> TaskResult<T> getFallBack(TaskContext taskContext, String command, Map<String, Object> params, S data) {
        throw new UnsupportedOperationException("No fallback available.");
    }

    /**
     * No fallback for execution failures. Throw an exception here so that hystrix propagates it up to the caller.
     * If we just return null, hystrix will mark fallback success and return null result to caller but we want failure in execute() to bubble up.
     *
     * @param taskContext
     * @param command
     * @param taskRequestWrapper
     * @param decoder
     * @param <T>
     * @param <S>
     * @return
     * @throws RuntimeException
     */
    @Override
    public <T, S> TaskResult<T> getFallBack(TaskContext taskContext, String command, TaskRequestWrapper<S> taskRequestWrapper, Decoder<T> decoder) throws RuntimeException {
        throw new UnsupportedOperationException("No fallback available.");
    }

    /** interface method implementation */
    @Override
    public void shutdown(TaskContext taskContext) throws Exception {
    }

    /**
     * Gets RequestHeaders
     *
     * @param params
     */
    protected Map<String,String> getRequestHeaders(Map<String,Object> params) {
        Map<String,String> requestHeaders = new HashMap<>();

        if (params != null ) {
            if(params.containsKey("requestID")) {
                requestHeaders.put("X-Request-ID", (String) params.get("requestID"));
            }
            if(params.containsKey(HTTP_HEADERS)) {
                Map<String, Object> customHeaders = (Map<String, Object>) params.get(HTTP_HEADERS);
                if (customHeaders != null) {
                    for (Map.Entry<String, Object> entry: customHeaders.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            requestHeaders.put(entry.getKey(), entry.getValue().toString());
                        }
                    }
                }
            }
        }
        return requestHeaders;
    }

    /**
     * This makes the call to the service via the Http Client Framework
     *
     * @param method
     * @param uri
     * @param data
     * @param requestHeaders
     * @return
     * @throws Exception
     */
    protected final HttpResponseData makeRequest(String method, String uri, byte[] data,
                                                 Map<String,String> requestHeaders) throws Exception{
        HttpRequestBase request = this.pool.createHttpRequest(uri, data, requestHeaders, method);
        return getHttpResponseData(this.pool.execute(request));
    }

    /**
     *
     * @param taskContext
     * @param command
     * @param params
     * @param data
     * @return
     */
    protected  TaskResult processAsyncHttpRequest(TaskContext taskContext,String command, Map<String,Object> params, byte[] data){
        HttpResponse response = null;
        try{
            if(!enableAsyncExecution){
                response = getDummyHttpResponse(HttpStatus.SC_SERVICE_UNAVAILABLE);
                return handleHttpResponse(getHttpResponseData(response));
            } else {
                params.remove("executeAsync");
                response = getDummyHttpResponse(HttpStatus.SC_OK);
                taskContext.executeAsyncCommand(command,data,params);
                return handleHttpResponse(getHttpResponseData(response));
            }
        } catch (Exception e){
            handleException(e, (String) params.get(HTTP_URI), (String) params.get(HTTP_METHOD));
        }
        return new TaskResult(false,null,null);
    }

    /**
     *  exception handler
     *
     * @param e
     * @param uri
     * @param method
     * @return
     */
    protected TaskResult handleException(Exception e, String uri, String method) {
        throw new RuntimeException(e.getMessage() + "(URI = " + uri + ", METHOD = " + method + ")", e);
    }

    /**
     * Method to be overwritten by sub-classes for handling responses.
     *
     * @param responseData
     * @return  TaskResult
     * @throws Exception
     */
    protected TaskResult handleHttpResponse(final HttpResponseData responseData) throws Exception{
        return new TaskResult(true, null, responseData.getResponseBody(),
                (responseData.getStatusCode() + colon).getBytes());
    }

    /**
     *
     * @param responseHeaders
     * @return
     */
    protected Map<String,String> getResponseHeaders(Header[] responseHeaders) {
        Map<String,String> headers = new HashMap<String,String>();
        if (responseHeaders != null) {
            for (Header header : responseHeaders) {
                headers.put(header.getName(), header.getValue());
            }
        }
        return headers;
    }

    /**
     * Handles Http Response Object and converts into the TaskResult
     *
     * @param httpResponse
     * @return
     * @throws Exception
     */
    private HttpResponseData getHttpResponseData(HttpResponse httpResponse) throws Exception{
        return new HttpResponseData(httpResponse.getStatusLine().getStatusCode(), getResponseBody(httpResponse),
                getResponseHeaders(httpResponse.getAllHeaders()));
    }

    /**
     * Helper method that converts Http response Body into a string.
     */
    private byte[] getResponseBody(HttpResponse response) throws Exception {
        if (response.getEntity() != null) {
            return  EntityUtils.toByteArray(response.getEntity());
        }
        return "".getBytes();
    }


    private HttpResponse getDummyHttpResponse(int statusCode)
    {
        HttpResponse response = null;
        switch(statusCode){
            case HttpStatus.SC_OK:
                response = new BasicHttpResponse(HttpVersion.HTTP_1_1,HttpStatus.SC_OK, "OK");
                break;
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                response = new BasicHttpResponse(HttpVersion.HTTP_1_1,HttpStatus.SC_INTERNAL_SERVER_ERROR,"Internal Server Error");
                break;
            case HttpStatus.SC_SERVICE_UNAVAILABLE:
                response = new BasicHttpResponse(HttpVersion.HTTP_1_1,HttpStatus.SC_SERVICE_UNAVAILABLE,"Service Unavailable");
                break;
            default:
                response = new BasicHttpResponse(HttpVersion.HTTP_1_1,HttpStatus.SC_BAD_REQUEST,"Bad Request");
                break;
        }
        return response;
    }


    /** Getters / Setters */
    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getOperationTimeout() {
        return operationTimeout;
    }

    public void setOperationTimeout(int operationTimeout) {
        this.operationTimeout = operationTimeout;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public void setSecure(boolean secure) {
        isSecure = secure;
    }

    public String getAccept() {
        return accept;
    }

    public void setAccept(String accept) {
        this.accept = accept;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Boolean getEnableAsyncExecution() {
        return enableAsyncExecution;
    }

    public void setEnableAsyncExecution(Boolean enableAsyncExecution) {
        this.enableAsyncExecution = enableAsyncExecution;
    }

    public int getTimeToLiveInSecs() {
        return timeToLiveInSecs;
    }

    public void setTimeToLiveInSecs(int timeToLiveInSecs) {
        this.timeToLiveInSecs = timeToLiveInSecs;
    }

    public boolean isRequestCompressionEnabled() {
        return requestCompressionEnabled;
    }

    public void setRequestCompressionEnabled(boolean requestCompressionEnabled) {
        this.requestCompressionEnabled = requestCompressionEnabled;
    }

    public boolean isResponseCompressionEnabled() {
        return responseCompressionEnabled;
    }

    public void setResponseCompressionEnabled(boolean responseCompressionEnabled) {
        this.responseCompressionEnabled = responseCompressionEnabled;
    }

    public boolean isRequestCachingEnabled() {
        return requestCachingEnabled;
    }

    public void setRequestCachingEnabled(boolean requestCachingEnabled) {
        this.requestCachingEnabled = requestCachingEnabled;
    }
}
