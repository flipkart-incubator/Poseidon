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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Created by mohan.pandian on 14/04/15.
 *
 * Executes any system command and returns the string output
 * or throws exception in case of error
 */
public class CommandExecutor {
    private final static Logger logger = LoggerFactory.getLogger(CommandExecutor.class);

    public static String execute(File directory, String... command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(directory);

        Process process = processBuilder.start();
        String output = readOutput(process);
        int exitCode = -1;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            logger.error("Exception waiting for process", e);
        }
        if (exitCode != 0) {
            throw new CommandFailedException(output, exitCode);
        }
        return output;
    }

    private static String readOutput(Process process) throws IOException {
        InputStream stream = process.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }
}
