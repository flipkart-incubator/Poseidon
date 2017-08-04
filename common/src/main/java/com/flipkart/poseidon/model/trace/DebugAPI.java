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

package com.flipkart.poseidon.model.trace;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;

/**
 * Created by shrey.garg on 30/05/17.
 */
@JsonPropertyOrder({
        "apiStatus",
        "serviceCallInfo"
})
public class DebugAPI {
    private Map<String, List<ServiceCallDebug>> serviceCallInfo;
    private int apiStatus;
    private Object apiResponse;

    public Map<String, List<ServiceCallDebug>> getServiceCallInfo() {
        return serviceCallInfo;
    }

    public void setServiceCallInfo(Map<String, List<ServiceCallDebug>> serviceCallInfo) {
        this.serviceCallInfo = serviceCallInfo;
    }

    public Object getApiResponse() {
        return apiResponse;
    }

    public void setApiResponse(Object apiResponse) {
        this.apiResponse = apiResponse;
    }

    public int getApiStatus() {
        return apiStatus;
    }

    public void setApiStatus(int apiStatus) {
        this.apiStatus = apiStatus;
    }
}
