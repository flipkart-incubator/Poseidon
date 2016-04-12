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

package com.flipkart.poseidon.serviceclients.generator;

import com.flipkart.poseidon.serviceclients.executor.CommandFailedException;
import com.flipkart.poseidon.serviceclients.executor.GitCommandExecutor;
import com.flipkart.poseidon.serviceclients.executor.MavenCommandExecutor;
import com.flipkart.poseidon.serviceclients.idl.pojo.ServiceIDL;
import com.flipkart.poseidon.serviceclients.idl.pojo.Version;
import com.flipkart.poseidon.serviceclients.idl.reader.IDLReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mohan.pandian on 06/04/15.
 *
 * Handles automatic versioning of service module.
 * Reads existing file from GIT, compares against local file,
 * bumps major version if there is a backward incompatible change,
 * bumps minor version if there is a backward compatible change
 * else no change in version. Updates pom.xml accordingly.
 */
public class Versioner {
    private final static Logger logger = LoggerFactory.getLogger(Versioner.class);
    private static final Versioner VERSIONER = new Versioner();

    private Versioner() {
    }

    public static Versioner getInstance() {
        return VERSIONER;
    }

    public void updateVersion(String moduleParentPath, String moduleName, String filePath, Version version) {
        try {
            String relativeFilePath = filePath.substring(filePath.lastIndexOf(moduleName));
            String json = GitCommandExecutor.getHeadVersion(moduleParentPath, relativeFilePath);
            ServiceIDL oldServiceIdl = IDLReader.convertToIDL(json);
            ServiceIDL newServiceIdl = IDLReader.getIDL(filePath);

            if (changeVersion(oldServiceIdl, newServiceIdl, moduleName, version)) {
                logger.info("Updating version as {} for {}", version, moduleName);
                MavenCommandExecutor.setVersion(moduleParentPath, moduleName, version);
            }
        } catch(CommandFailedException e) {
            logger.error("Command failed with exit code {}. Output {}", e.getExitCode(), e.getOutput());
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    public boolean changeVersion(ServiceIDL oldServiceIdl, ServiceIDL newServiceIdl, String moduleName, Version version) {
        if (oldServiceIdl.equals(newServiceIdl)) {
            logger.info("No changes detected for {}", moduleName);
            return false;
        }

        if (!isBackwardCompatible(oldServiceIdl, newServiceIdl)) {
            logger.info("Bumping major version for {}", moduleName);
            version.setMajor(version.getMajor() + 1);
        } else {
            logger.info("Bumping minor version for {}", moduleName);
            version.setMinor(version.getMinor() + 1);
        }
        return true;
    }

    public boolean isBackwardCompatible(ServiceIDL oldServiceIdl, ServiceIDL newServiceIdl) {
        if (oldServiceIdl.getEndPoints().size() != newServiceIdl.getEndPoints().size()) {
            return false;
        }

        return true;
    }
}
