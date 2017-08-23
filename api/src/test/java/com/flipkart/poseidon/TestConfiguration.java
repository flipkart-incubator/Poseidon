/*
 * Copyright 2017 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon;

import com.flipkart.poseidon.api.*;

/**
 * Created by shrey.garg on 26/07/17.
 */
public class TestConfiguration implements Configuration {
    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public boolean sendServerVersion() {
        return false;
    }

    @Override
    public String getRewriteFilePath() {
        return null;
    }

    @Override
    public String getAccessLogConfigFilePath() {
        return null;
    }

    @Override
    public boolean isAccessLogEnabled() {
        return false;
    }

    @Override
    public String getRotationStatusFilePath() {
        return null;
    }

    @Override
    public String getApiFilesPath() {
        return null;
    }

    @Override
    public String[] getFilterIds() {
        return new String[0];
    }

    @Override
    public String getAppName() {
        return null;
    }

    @Override
    public TracingConfiguration getTracingConfiguration() {
        return null;
    }

    @Override
    public JettyConfiguration getJettyConfiguration() {
        return null;
    }

    @Override
    public ExceptionMapper getExceptionMapper() {
        return null;
    }

    @Override
    public Headers getHeadersConfiguration() {
        return null;
    }
}
