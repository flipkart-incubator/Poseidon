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
import com.flipkart.poseidon.handlers.http.utils.StringUtils;
import com.flipkart.poseidon.helper.ClassPathHelper;
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.pojos.EndpointPOJO;
import com.google.common.reflect.ClassPath;
import flipkart.lego.api.entities.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.flipkart.poseidon.helpers.ObjectMapperHelper.getMapper;

/**
 * Created by shrey.garg on 16/06/16.
 */
public class APIValidator {
    private static final Logger logger = LoggerFactory.getLogger(APIValidator.class);

    private static final List<EndpointPOJO> pojos = new ArrayList<>();

    public static void main(String[] args) {
        boolean skipAPIValidator = Boolean.valueOf(System.getProperty("skipAPIValidator"));
        if (!skipAPIValidator) {
            Path dir = Paths.get(args[0]);
            CustomValidator customValidator = null;

            boolean validateDataSources = false;
            List<String> packagesToScan = new ArrayList<>();

            if (args.length > 1) {
                if (!StringUtils.isNullOrEmpty(args[1].trim())) {
                    try {
                        final Class<?> customValidatorClass = Class.forName(args[1]);
                        if (!CustomValidator.class.isAssignableFrom(customValidatorClass)) {
                            logger.error("Validator class passed does not implement CustomValidator");
                            System.exit(-1);
                        } else {
                            customValidator = (CustomValidator) customValidatorClass.newInstance();
                        }
                    } catch (ClassNotFoundException e) {
                        logger.error("Wrong CustomValidator passed", e);
                        System.exit(-1);
                    } catch (Exception e) {
                        logger.error("Something went wrong while constructing CustomValidator", e);
                        System.exit(-1);
                    }
                }

                if (args.length > 2) {
                    validateDataSources = true;
                    final String[] datasourcePackages = args[2].split(",");
                    packagesToScan = Arrays.asList(datasourcePackages);
                }
            }

            List<String> validConfigs = new ArrayList<>();
            try {
                APIManager.scanAndAdd(dir, validConfigs);
                for (String config : validConfigs) {
                    pojos.add(getMapper().readValue(config, EndpointPOJO.class));
                }

                Set<ClassPath.ClassInfo> classInfos = ClassPathHelper.getPackageClasses(Thread.currentThread().getContextClassLoader(), packagesToScan);
                Map<String, Class<? extends DataSource<?>>> datasources = new HashMap<>();
                for (ClassPath.ClassInfo classInfo : classInfos) {
                    Class clazz = Class.forName(classInfo.getName());
                    if (Modifier.isAbstract(clazz.getModifiers())) {
                        continue;
                    }

                    if (DataSource.class.isAssignableFrom(clazz)) {
                        datasources.put(PoseidonLegoSet.getBlockId(clazz).get(), clazz);
                    }
                }

                Map<String, List<String>> errors = new HashMap<>();
                for (EndpointPOJO pojo : pojos) {
                    List<String> pojoErrors = EndpointValidator.validate(pojo, datasources, validateDataSources);

                    if (customValidator != null) {
                        pojoErrors.addAll(customValidator.validate(pojo));
                    }

                    if (!pojoErrors.isEmpty()) {
                        errors.put(pojo.getHttpMethod() + " " + pojo.getUrl(), pojoErrors);
                    }
                }

                if (!errors.isEmpty()) {
                    logger.error(ValidatorUtils.getFormattedErrorMessages(errors));
                    System.exit(-1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
}
