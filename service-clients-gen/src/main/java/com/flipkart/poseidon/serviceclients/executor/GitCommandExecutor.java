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

package com.flipkart.poseidon.serviceclients.executor;

import java.io.File;
import java.io.IOException;

/**
 * Created by mohan.pandian on 14/04/15.
 *
 * Executes Git commands using CommandExecutor
 */
public class GitCommandExecutor {
    public static String getHeadVersion(String directory, String relativeFilePath) throws IOException {
        // git show HEAD:sample/src/main/resources/idl/service/SampleService.json
        return CommandExecutor.execute(new File(directory), "git", "show", "HEAD:" + relativeFilePath);
    }
}
