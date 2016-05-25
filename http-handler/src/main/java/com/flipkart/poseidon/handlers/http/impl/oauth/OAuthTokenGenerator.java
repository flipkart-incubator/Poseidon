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

    public OAuthTokenGenerator() {}

    /**
     * interface method implementation
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

    private void validate() throws Exception{
        if (StringUtils.isNullOrEmpty(clientId) || StringUtils.isNullOrEmpty(secret)) {
            throw new Exception("clientId or secret cannot be null");
        }
    }

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
            tokenResponse = new ObjectMapper().readValue(new String(responseData.getResponseBody()), OAuthTokenResponse.class);
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

    private void refreshAccessToken() {
        TokenRefresher tokenRefresher = new TokenRefresher();
        thread = new Thread(tokenRefresher);
        thread.start();
    }

    public String getAccessToken() { return accessToken; }

    private String uriBuilder(String path, Map<String, String> params) {
        URIBuilder builder = new URIBuilder();
        builder.setPath(path);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.addParameter(entry.getKey(),entry.getValue());
        }
        return builder.toString();
    }

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
        thread.join();
        super.shutdown(taskContext);
    }

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
                // trigger an alert for any error
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