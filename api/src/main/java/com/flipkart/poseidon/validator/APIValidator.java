/*
 * Copyright 2016 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon.validator;

import com.flipkart.poseidon.api.APIManager;
import com.flipkart.poseidon.pojos.EndpointPOJO;
import com.flipkart.poseidon.pojos.ParamPOJO;
import com.flipkart.poseidon.pojos.ParamsPOJO;
import com.flipkart.poseidon.pojos.TaskPOJO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.flipkart.poseidon.helpers.ObjectMapperHelper.getMapper;
import static com.flipkart.poseidon.validator.ValidatorUtils.*;

/**
 * Created by shrey.garg on 16/06/16.
 */
public class APIValidator {
    private static final Logger logger = LoggerFactory.getLogger(APIValidator.class);

    private static final List<EndpointPOJO> pojos = new ArrayList<>();

    public static void main(String[] args) {
        Path dir = Paths.get(args[0]);
        List<String> validConfigs = new ArrayList<>();
        try {
            APIManager.scanAndAdd(dir, validConfigs);
            for (String config : validConfigs) {
                pojos.add(getMapper().readValue(config, EndpointPOJO.class));
            }

            Map<String, List<String>> errors = new HashMap<>();
            for (EndpointPOJO pojo : pojos) {
                List<String> pojoErrors = EndpointValidator.validate(pojo);
                if (!pojoErrors.isEmpty()) {
                    errors.put(pojo.getHttpMethod() + " " + pojo.getUrl(), pojoErrors);
                }
            }

            if (!errors.isEmpty()) {
                for (Map.Entry<String, List<String>> entry: errors.entrySet()) {
                    logger.error("--------------------------------------------");
                    logger.error("Errors while validating " + entry.getKey());
                    logger.error("--------------------------------------------");
                    entry.getValue().forEach(logger::error);
                    logger.error("");
                }
                System.exit(-1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
