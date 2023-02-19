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

package com.flipkart.poseidon.serviceclients;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by venkata.lakshmi on 30/03/15.
 */
public class ServiceResponse<T> {

    private boolean isSuccess;
    private List<T> dataList = new ArrayList<>();
    private Map<String, String> headers;
    private ServiceClientException exception;

    public ServiceResponse() {}

    public ServiceResponse(T data, Map<String, String> headers) {
        this.isSuccess = true;
        this.dataList.add(data);
        this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((k,v) -> this.headers.put(k,v));
    }

    public ServiceResponse(ServiceClientException e, Map<String, String> headers) {
        this.exception = e;
        this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((k,v) -> this.headers.put(k,v));
    }

    public void addData(List<T> data) {
        if (data != null && !data.isEmpty()) {
            this.dataList.addAll(data);
        }
    }

    public List<T> getDataList() {
        return dataList;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public ServiceClientException getException() {
        return exception;
    }

    public boolean getIsSuccess() {
        return isSuccess;
    }
}
