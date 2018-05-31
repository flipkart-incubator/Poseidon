/*
 * Copyright 2018 Flipkart Internet, pvt ltd.
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

import com.fasterxml.jackson.databind.JavaType;

/**
 * Created by shrey.garg on 22/05/18.
 */
public class ServiceResponseInfo {
    private JavaType type;
    private Class<? extends ServiceClientException> exceptionClass;

    public ServiceResponseInfo(JavaType type, Class<? extends ServiceClientException> exceptionClass) {
        this.type = type;
        this.exceptionClass = exceptionClass;
    }

    public JavaType getType() {
        return type;
    }

    public void setType(JavaType type) {
        this.type = type;
    }

    public Class<? extends ServiceClientException> getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(Class<? extends ServiceClientException> exceptionClass) {
        this.exceptionClass = exceptionClass;
    }
}
