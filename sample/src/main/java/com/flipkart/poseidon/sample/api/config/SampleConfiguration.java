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

package com.flipkart.poseidon.sample.api.config;

import com.flipkart.poseidon.api.*;
import com.flipkart.poseidon.sample.commons.SampleConstants;

public class SampleConfiguration implements Configuration {

    private int port;

    private String rewriteFilePath;

    private String accessLogConfigFilePath;

    private String rotationStatusFilePath;

    private String apiFilesPath;

    private TracingConfiguration tracingConfiguration;

    private JettyConfiguration jettyConfiguration;

    private ExceptionMapper exceptionMapper;

    private Headers headers;

    private boolean isAccessLogEnabled;

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean sendServerVersion() {
        return false;
    }

    @Override
    public String getRewriteFilePath() {
        return rewriteFilePath;
    }

    public void setRewriteFilePath(String rewriteFilePath) {
        this.rewriteFilePath = rewriteFilePath;
    }

    @Override
    public String getAccessLogConfigFilePath() {
        return accessLogConfigFilePath;
    }

    public void setAccessLogConfigFilePath(String accessLogConfigFilePath) {
        this.accessLogConfigFilePath = accessLogConfigFilePath;
    }

    @Override
    public String getRotationStatusFilePath() {
        return rotationStatusFilePath;
    }

    public void setRotationStatusFilePath(String rotationStatusFilePath) {
        this.rotationStatusFilePath = rotationStatusFilePath;
    }

    @Override
    public String getApiFilesPath() {
        return apiFilesPath;
    }

    public void setApiFilesPath(String apiFilesPath) {
        this.apiFilesPath = apiFilesPath;
    }

    @Override
    public String[] getFilterIds() {
        return null;
    }

    @Override
    public String getAppName() {
        return SampleConstants.APP_NAME;
    }

    @Override
    public TracingConfiguration getTracingConfiguration() {
        return tracingConfiguration;
    }

    public void setTracingConfiguration(TracingConfiguration tracingConfiguration) {
        this.tracingConfiguration = tracingConfiguration;
    }

    @Override
    public JettyConfiguration getJettyConfiguration() {
        return jettyConfiguration;
    }

    public void setJettyConfiguration(JettyConfiguration jettyConfiguration) {
        this.jettyConfiguration = jettyConfiguration;
    }

    @Override
    public ExceptionMapper getExceptionMapper(){
        return exceptionMapper;
    }

    public void setExceptionMapper(ExceptionMapper exceptionMapper) {
        this.exceptionMapper = exceptionMapper;
    }

    @Override
    public Headers getHeadersConfiguration() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    @Override
    public boolean isAccessLogEnabled() {
        return isAccessLogEnabled;
    }

    public void setIsAccessLogEnabled(boolean isAccessLogEnabled) {
        this.isAccessLogEnabled = isAccessLogEnabled;
    }
}
