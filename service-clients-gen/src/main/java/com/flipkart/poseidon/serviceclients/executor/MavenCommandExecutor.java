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

import com.flipkart.poseidon.serviceclients.idl.pojo.Version;

import java.io.File;
import java.io.IOException;

/**
 * Created by mohan.pandian on 14/04/15.
 *
 * Executes mvn commands using CommandExecutor
 */
public class MavenCommandExecutor {
    public static void setVersion(String directory, String moduleName, Version version) throws IOException {
        // mvn versions:set -DnewVersion=1.0.1 -f sample/pom.xml
        // mvn versions:commit
        File baseDirectory = new File(directory);
        CommandExecutor.execute(baseDirectory, "mvn", "versions:set",
                "-DnewVersion=" + version, "-f", moduleName + File.separator + "pom.xml");
        CommandExecutor.execute(baseDirectory, "mvn", "versions:commit");
    }
}
