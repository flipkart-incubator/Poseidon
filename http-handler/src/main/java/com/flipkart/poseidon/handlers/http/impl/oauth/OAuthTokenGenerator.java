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
package com.flipkart.poseidon.handlers.http.impl.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.poseidon.handlers.http.HttpResponseData;
import com.flipkart.poseidon.handlers.http.impl.SinglePoolHttpTaskHandler;
import com.flipkart.poseidon.handlers.http.utils.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth Token Generator gets the token at init step and creates a thread which refreshes it.
 *
 * Thread will refresh token after 90% of expiry time
 *
 * we only support grant_type as client_credentials while making call to Oauth end point.
 */
public class OAuthTokenGenerator extends SinglePoolHttpTaskHandler {
    private static final Logger logger = LogFactory.getLogger(OAuthTokenGenerator.class);
    private Thread thread;
    private String clientId;
    private String secret;
    private String accessToken;
    private String uri;
    private String authUriPath;
    private String authMethod;
    private Map<String, String> authParams;
    private long expiryTime;
    private boolean stopThread = false;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public OAuthTokenGenerator() {}

    /**
     * interface method implementation
     *
     * validate clientId and secret
     *
     * try to generate access token. If not success, raises an error
     *
     * If succeeded to get access token, we create thread to refresh access token #see refreshAccessToken method
     */
    @Override
    public void init(TaskContext taskContext) throws Exception {
        validate();
        super.init(taskContext);
        uri = uriBuilder(authUriPath, authParams);
        if (!generateAccessToken()) {
            logger.error("failed to get access token at Init step");
            throw new Exception("failed to get access token at OAuthTokenGenerator Init");
        }
        refreshAccessToken();
    }

    /**
     * validates clientId and secret
     * @throws Exception
     */
    private void validate() throws Exception{
        if (StringUtils.isNullOrEmpty(clientId) || StringUtils.isNullOrEmpty(secret)) {
            throw new Exception("clientId or secret cannot be null");
        }
    }

    /**
     * makes call to Oauth end point. On success, return true. Else false.
     * @return true or false
     */
    private boolean generateAccessToken() {
        HttpResponseData responseData;
        try {
            responseData = makeRequest(authMethod, uri, null, getRequestHeaders());
        } catch (Exception e) {
            logger.error("unable to make request to Oauth end point");
            return false;
        }

        if (responseData.getStatusCode() != HttpStatus.SC_OK) {
            logger.error("failed to get access token from OAuth endpoint. error: {}", new String(responseData.getResponseBody()));
            return false;
        }

        OAuthTokenResponse tokenResponse;
        try {
            tokenResponse = objectMapper.readValue(new String(responseData.getResponseBody()), OAuthTokenResponse.class);
        } catch (Exception e){
            logger.error("unable to cast response from Oauth to OAuthTokenResponse");
            return false;
        }

        accessToken = tokenResponse.getAccessToken();

        // tokenResponse has expiry time in seconds.
        //sleep for 90% of expiry time in milli seconds.
        expiryTime = tokenResponse.getExpiresIn() * (900);   // 1000 * (90 / 100) ; 1000 for milli seconds and (90/100) for 90% of time
        return true;
    }

    /**
     * creates a thread and starts it.
     */
    private void refreshAccessToken() {
        TokenRefresher tokenRefresher = new TokenRefresher();
        thread = new Thread(tokenRefresher);
        thread.start();
    }

    /**
     * will be used to access accessToken from task handler
     *
     * @return string
     */
    public String getAccessToken() { return accessToken; }

    /**
     * builds uri for Oauth end point given UriPath and Params
     *
     * @param path
     * @param params
     * @return string
     */
    private String uriBuilder(String path, Map<String, String> params) {
        URIBuilder builder = new URIBuilder();
        builder.setPath(path);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.addParameter(entry.getKey(),entry.getValue());
        }
        return builder.toString();
    }

    /**
     * get request headers for making call to Oauth end point
     * @return
     */
    private Map<String, String> getRequestHeaders() {
        Map<String, String> requestHeaders = new HashMap<String, String>();
        String credentials = clientId + ":" + secret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        requestHeaders.put("Authorization", "Basic " + encodedCredentials);
        return requestHeaders;
    }

    /**
     * makes stopThread flag as true in shutdown process
     */
    @Override
    public void shutdown(TaskContext taskContext) throws Exception{
        stopThread = true;
        thread.interrupt();
        thread.join();
        super.shutdown(taskContext);
    }

    /**
     * implements Runnable interface
     *
     * Thread will sleep for 90% of expiry time and refreshes then and update accessToken variable with new token
     *
     * If we fail to get new access token, we will sleep for 5 secs and then again try to get new access token.
     */
    private class TokenRefresher implements Runnable {
        public void run() {
            refreshAccessToken();
        }

        private void refreshAccessToken() {
            try {
                while (!stopThread) {
                    Thread.sleep(expiryTime);
                    while(!stopThread && !generateAccessToken()) {
                        logger.error("failed to refresh access token. sleeping for 5 seconds");
                        Thread.sleep(5000); //sleeping for 5 seconds and retry for refreshing access token.
                    };
                }
            } catch (Exception exception) {
                logger.error("error occurred while fetching Oauth access token.");
            }
        }
    }

    public void setClientId(String clientId) { this.clientId = clientId; }

    public void setSecret(String secret) { this.secret = secret; }

    public void setAuthUriPath(String authUriPath) { this.authUriPath = authUriPath; }

    public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }

    public void setAuthParams(Map<String, String> authParams) { this.authParams = authParams; }
}