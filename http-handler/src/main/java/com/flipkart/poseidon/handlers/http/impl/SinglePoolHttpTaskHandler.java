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
    public int maxConnections = 10;
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
        Map<String,String> params = taskRequestWrapper.getParams();
        byte[] data = (byte[]) taskRequestWrapper.getData();

        Map<String,String> requestHeaders = getRequestHeaders(params);

        try {
            HttpRequestBase request = this.pool.createHttpRequest(params.get("uri"), data, requestHeaders, params.get("method"));
            HttpResponse httpResponse =  this.pool.execute(request);

            return  new TaskResult<T>(true, null, ((HttpResponseDecoder<T>) decoder).decode(httpResponse));
        }
        catch (Exception e) {
            handleException(e, params.get("uri"), params.get("method"));
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
    public <T, S> TaskResult<T> execute(TaskContext taskContext, String command, Map<String, String> params, S data) {

        Map<String,String> requestHeaders = getRequestHeaders(params);

        if(params.get("executeAsync") != null && Boolean.parseBoolean(params.get("executeAsync"))){
            return processAsyncHttpRequest(taskContext,command,params,(byte[]) data);
        }
        TaskResult result;
        try {
            result  = handleHttpResponse(makeRequest(params.get("method"),params.get("uri"), (byte[]) data, requestHeaders));
        }
        catch (Exception e) {
            result =  handleException(e, params.get("uri"), params.get("method"));
        }
        return result;
    }

    @Override
    public <S> String getCacheKey(Map<String, String> requestParams, S data) {
        if (!requestCachingEnabled || requestParams == null) {
            return null;
        }

        boolean hasURI = requestParams.containsKey("uri");
        boolean isGet = "GET".equalsIgnoreCase(requestParams.get("method"));
        boolean askedTobeCached = "true".equalsIgnoreCase(requestParams.get("X-Cache-Request"));
        if (!hasURI || (!isGet && !askedTobeCached)) {
            return null;
        }

        String cacheKey = requestParams.get("uri") + (data != null ? new String((byte[]) data) : "");
        return murmur3_32().hashString(cacheKey, Charsets.UTF_16LE).toString();
    }

    /** interface method implementation */
    @Override
    public <T, S> TaskResult<T> getFallBack(TaskContext taskContext, String command, Map<String, String> params, S data) {
        return null;
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
    protected Map<String,String> getRequestHeaders(Map<String,String> params) {
        Map<String,String> requestHeaders = new HashMap<String, String>();

        if (params != null ) {
            if(params.containsKey("requestID")) {
                requestHeaders.put("X-Request-ID",params.get("requestID"));
            }
            if(params.containsKey("headers")) {
                try{
                    Map<String,String> customHeaders = objectMapper.readValue(params.get("headers"), Map.class);
                    requestHeaders.putAll(customHeaders);
                } catch (Exception e) {
                    logger.info("Error while parsing custom header" + e.getMessage());
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
    protected  TaskResult processAsyncHttpRequest(TaskContext taskContext,String command, Map<String,String> params, byte[] data){
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
            handleException(e,params.get("uri"),params.get("method"));
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

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
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
