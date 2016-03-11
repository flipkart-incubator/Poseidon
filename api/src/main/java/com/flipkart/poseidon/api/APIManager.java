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

import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.pojos.EndpointPOJO;
import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import static com.flipkart.poseidon.helpers.ObjectMapperHelper.getMapper;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class APIManager {

    private static final Logger logger = getLogger(APIManager.class);
    private final PoseidonLegoSet legoSet;
    private final Configuration configuration;

    @Autowired
    public APIManager(PoseidonLegoSet legoSet, Configuration configuration) {
        this.legoSet = legoSet;
        this.configuration = configuration;
    }

    public void init() {
        updateLegoSet(readConfigs());
        (new DirectoryWatcher()).start();
    }

    private void updateLegoSet(String configs) {
        APILoader loader = new APILoader(legoSet, configs, configuration);
        try {
            legoSet.updateBuildables(loader.getBuildableMap());
        } catch (Exception exception) {
            logger.error("Unable to update legoset", exception);
        }
    }

    private String readConfigs() {
        List<String> validConfigs = new ArrayList<>();

        Path dir = Paths.get(configuration.getApiFilesPath());
        scanAndAdd(dir, validConfigs);

        return "[" + Joiner.on(",").join(validConfigs) + "]";
    }

    private void scanAndAdd(Path dir, List<String> validConfigs) {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
            for (Path entry : files) {
                File file = entry.toFile();
                if (file.isDirectory()) {
                    scanAndAdd(entry, validConfigs);
                    continue;
                }
                if ("json".equals(FilenameUtils.getExtension(file.getName()))) {
                    try {
                        String config = FileUtils.readFileToString(file);
                        if (validateConfig(config)) {
                            validConfigs.add(config);
                        }
                    } catch (IOException e) {
                        logger.error("Unable to read one of the local config. Filename = [[" + file.getName() + "]]");
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Local override directory not found.");
        }

    }

    private boolean validateConfig(String config) {
        try {
            getMapper().readValue(config, EndpointPOJO.class);
        } catch (IOException e) {
            logger.error("Unable to parse one of the config. Content = [[" + config + "]]");
            return false;
        }

        return true;
    }

    class DirectoryWatcher extends Thread {
        private WatchService watcher;

        public DirectoryWatcher() {
            try {
                Path dir = Paths.get(configuration.getApiFilesPath());
                watcher = dir.getFileSystem().newWatchService();
                dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            } catch (IOException e) {
                logger.error("Unable to initialize local override directory watcher.");
                watcher = null;
            }
        }

        @Override
        public void run() {
            if (watcher != null) {
                try {
                    while (true) {
                        WatchKey watchKey = watcher.take();
                        for (WatchEvent<?> ignored : watchKey.pollEvents()) ;
                        updateLegoSet(readConfigs());

                        if (!watchKey.reset()) {
                            logger.error("Local override directory no longer valid.");
                            watchKey.cancel();
                            watcher.close();
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Unable to update local changes", e);
                }
            }
        }
    }
}