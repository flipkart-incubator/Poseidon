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

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * This can be used to provide custom response handling for exceptions
 *
 * Created by shrey.garg on 16/02/16.
 */
public interface ExceptionMapper {
    /*
     * This method provides the root cause of the Throwable thrown by the application,
     * which can be used to modify the response (Status code, response body etc).
     * If the response has been modified, then the method should return true, false otherwise.
     */
    boolean map(Throwable e, HttpServletResponse response) throws IOException;
}
