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

package com.flipkart.poseidon.constants;

/**
 * Created by akshay.kesarwan on 13/07/15.
 */
public class RequestConstants {
    public static final String BODY = "body";
    public static final String BODY_BYTES = "body_bytes";
    public static final String SOURCE_ADDRESS = "sourceAddress";
    public static final String FILE_UPLOAD_TMP_DIR = "/tmp";
    public static final String PARAMS = "params";
    public static final String TIMER_CONTEXT = "timerContext";
    public static final String IS_ASYNC = "poseidon.request.async";

    // Used in RequestContext as keys
    public static final String METHOD = "poseidon.method";
    public static final String URI = "poseidon.uri";
    public static final String ENDPOINT_NAME = "poseidon.endpoint.name";
    public static final String ENDPOINT_METHOD = "poseidon.endpoint.method";
    public static final String REDIRECT_URL = "poseidon.redirectURL";
    public static final String HEADERS = "poseidon.headers";
    public static final String ENABLE_TRACING = "poseidon.enableTracing";
    public static final String API_ANNOTATIONS = "poseidon.endpoint.annotations";
}
