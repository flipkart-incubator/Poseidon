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

import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.poseidon.handlers.http.impl.SinglePoolHttpTaskHandler;

import java.util.Map;

/**
 * Can be used for OAuth 2.0 protocol
 *
 * OAuthTokenGenerator used in this task handler only supports grant_type as client_credentials.
 */
public class OAuthPoolHttpTaskHandler extends SinglePoolHttpTaskHandler {

    private OAuthTokenGenerator oAuthTokenGenerator;

    public OAuthPoolHttpTaskHandler(){}

    /**
     * interface method implementation
     */
    @Override
    public void init(TaskContext taskContext) throws Exception {
        if (oAuthTokenGenerator == null) {
            throw new Exception("oAuthTokenGenerator cannot be null at oAuthTaskHandler init step");
        }
        super.init(taskContext);
    }

    /**
     * Interface method implementation. @see TaskHandler#getName
     */
    @Override
    public String getName() { return "OAuthPoolHttpTaskHandler"; }

    /**
     * Gets RequestHeaders
     *
     * @param params
     */
    @Override
    protected Map<String, String> getRequestHeaders(Map<String, String> params) {
        Map<String, String> requestHeaders = super.getRequestHeaders(params);
        requestHeaders.put("Authorization", "Bearer " + oAuthTokenGenerator.getAccessToken());
        return requestHeaders;
    }

    public void setOAuthTokenGenerator(OAuthTokenGenerator oAuthTokenGenerator) { this.oAuthTokenGenerator = oAuthTokenGenerator; }
}
