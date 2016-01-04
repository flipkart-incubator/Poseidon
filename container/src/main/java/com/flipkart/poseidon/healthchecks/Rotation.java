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

package com.flipkart.poseidon.healthchecks;

import com.codahale.metrics.health.HealthCheck;

import java.io.File;

/**
 * This is a codahale {@link HealthCheck} that checks to see if the service is in rotation or not.
 */
public class Rotation extends HealthCheck {

    private final String statusFilePath;

    /**
     * Takes as input the file path of the file which maintains the service rotation status
     *
     * @param rotationStatusFile Full path of rotation status file
     */
    public Rotation(String rotationStatusFile) {
        statusFilePath = rotationStatusFile;
    }

    /**
     * Returns a {@link Result} that indicates healthy(in rotation) or unhealthy(out of rotation)
     *
     * @return a {@link Result} that indicates healthy(in rotation) or unhealthy(out of rotation)
     * @throws Exception If any exception occurs
     */
    @Override
    protected Result check() throws Exception {
        File statusFile = new File(statusFilePath);

        if (statusFile.exists()) {
            return HealthCheck.Result.healthy("In Rotation");
        } else {
            return HealthCheck.Result.unhealthy("Out Of Rotation");
        }
    }
}
