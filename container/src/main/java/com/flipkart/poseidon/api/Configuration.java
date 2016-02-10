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

package com.flipkart.poseidon.api;

public interface Configuration {

    public int getPort();

    public boolean sendServerVersion();

    public String getRewriteFilePath();

    public String getLogbackAccessPath();

    public String getRotationStatusFilePath();

    public String getApiFilesPath();

    public String[] getFilterIds();

    public String getAppName();

    public TracingConfiguration getTracingConfiguration();

    /*
     * Return null to use default values at framework
     */
    public JettyConfiguration getJettyConfiguration();
}
