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

import com.flipkart.poseidon.pojos.EndpointPOJO;
import com.flipkart.poseidon.pojos.ParamPOJO;
import com.flipkart.poseidon.pojos.ParamsPOJO;
import com.flipkart.poseidon.pojos.TaskPOJO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
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
    private static final List<String> notNullFields = Arrays.asList("tasks", "httpMethod", "url");
    private static final List<String> mutuallyExclusiveParamTypes = Arrays.asList("pathparam", "body", "header", "multivalue");

    private static final Map<String, List<String>> errors = new HashMap<>();

    public static void main(String[] args) {
        Path dir = Paths.get(args[0]);
        List<String> validConfigs = new ArrayList<>();
        try {
            scanAndAdd(dir, validConfigs);
            for (String config : validConfigs) {
                pojos.add(getMapper().readValue(config, EndpointPOJO.class));
            }

            pojos.stream().forEach(APIValidator::validateEndpoint);

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

    private static void validateEndpoint(EndpointPOJO pojo) {
        try {
            List<String> pojoErrors = new ArrayList<>();

            pojoErrors.addAll(validateFields(pojo));

            pojoErrors.addAll(validateUrl(pojo.getUrl()));

            pojoErrors.addAll(validateTimeout(pojo.getTimeout()));

            if (pojo.getParams() != null) {
                pojoErrors.addAll(validateParams(pojo.getParams(), pojo.getUrl()));
            }

            if (pojo.getTasks() != null && pojo.getParams() != null) {
                pojoErrors.addAll(validateTasks(pojo.getTasks(), pojo.getParams()));
            }

            if (!pojoErrors.isEmpty()) {
                errors.put(pojo.getHttpMethod() + " " + pojo.getUrl(), pojoErrors);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> validateFields(EndpointPOJO pojo) throws IllegalAccessException {
        List<String> errors = new ArrayList<>();

        Field[] fields = EndpointPOJO.class.getDeclaredFields();
        for (Field field : fields) {
            if (notNullFields.contains(field.getName())) {
                field.setAccessible(true);
                Object value = field.get(pojo);
                if (value == null || isEmpty(value)) {
                    errors.add("No " + field.getName() + " defined");
                }
            }
        }

        return errors;
    }

    private static List<String> validateUrl(String value) {
        List<String> errors = new ArrayList<>();

        if (value != null) {
            if (value.trim().isEmpty()) {
                errors.add("Invalid Url. Cannot be Empty.");
            } else {
                for (int i = 0; i < value.length() - 1; i++) {
                    if (value.charAt(i) == '/' && value.charAt(i + 1) == '/') {
                        errors.add("Invalid Url. Cannot contain two \'/\'s consecutively");
                    }
                }
            }
        }

        return errors;
    }

    private static List<String> validateTimeout(long value) {
        List<String> errors = new ArrayList<>();

        if (value <= 0) {
            errors.add("No or invalid timeout defined.");
        }

        return errors;
    }

    private static List<String> validateParams(ParamsPOJO value, String url) throws IllegalAccessException {
        List<String> errors = new ArrayList<>();

        errors.addAll(getParamErrors(value.getRequired(), url));
        errors.addAll(getParamErrors(value.getOptional(), url));

        errors.addAll(detectDuplicateParams(value.getRequired(), value.getOptional()));

        if (value.getOptional() != null) {
            boolean containsPathParam = Arrays.asList(value.getOptional()).stream().anyMatch(ParamPOJO::isPathparam);
            if (containsPathParam) {
                errors.add("Pathparam cannot be optional");
            }
        }

        return errors;
    }

    private static List<String> validateTasks(Map<String, TaskPOJO> tasks, ParamsPOJO params) {
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, TaskPOJO> entry : tasks.entrySet()) {
            String taskName = entry.getKey();
            TaskPOJO task = entry.getValue();
            if (isNullOrEmpty(task.getName())) {
                errors.add("No datasource defined for Task: " + braced(taskName));
            }
            Map<String, Object> context = Optional.ofNullable(task.getContext()).orElse(new HashMap<>());
            for (Map.Entry<String, Object> contextEntry : context.entrySet()) {
                String fullContextParam = stripBraces((String) contextEntry.getValue());
                boolean isOptional = fullContextParam.charAt(0) == '#';
                if (isOptional) {
                    fullContextParam = fullContextParam.substring(1);
                }

                boolean isExpression = fullContextParam.charAt(0) == '$';
                if (isExpression) {
                    fullContextParam = fullContextParam.substring(1);
                }

                String contextParam = fullContextParam.split("\\.")[0];

                boolean located = false;
                if (contextParam != null && !contextParam.isEmpty()) {
                    if (params.getRequired() != null) {
                        for (ParamPOJO param : params.getRequired()) {
                            if (contextParam.equals(param.getName())) {
                                located = true;
                                if (isOptional) {
                                    errors.add("Param: " + braced(param.getName()) + " used in Task: " + braced(taskName) + " is not optional");
                                }
                            }
                        }
                    }

                    if (params.getOptional() != null) {
                        for (ParamPOJO param : params.getOptional()) {
                            if (contextParam.equals(param.getName())) {
                                located = true;
                                if (!isOptional && param.getDefaultValue() == null) {
                                    errors.add("Param: " + braced(param.getName()) + " used in Task: " + braced(taskName) + " is optional. Add a \'#\'");
                                }
                            }
                        }
                    }

                    for (String param : tasks.keySet()) {
                        if (contextParam.equals(param)) {
                            located = true;
                        }
                    }

                    if (!located || contextParam.equals(taskName)) {
                        errors.add("Param: " + braced(contextParam) + " used in Task: " + braced(taskName) + " is not defined anywhere");
                    }
                }
            }
        }

        return errors;
    }

    private static List<String> getMarkedParamTypes(ParamPOJO param) throws IllegalAccessException {
        List<String> marks = new ArrayList<>();
        Field[] fields = ParamPOJO.class.getDeclaredFields();
        for (Field field : fields) {
            if (boolean.class.isAssignableFrom(field.getType()) && mutuallyExclusiveParamTypes.contains(field.getName())) {
                field.setAccessible(true);
                Boolean value = field.getBoolean(param);
                if (value) {
                    marks.add(field.getName());
                }
            }
        }
        return marks;
    }

    private static List<String> getParamErrors(ParamPOJO[] params, String url) throws IllegalAccessException {
        List<String> errors = new ArrayList<>();
        if (params != null && params.length > 0) {
            for (ParamPOJO param : params) {
                List<String> paramTypes = getMarkedParamTypes(param);
                if (paramTypes.size() > 1) {
                    errors.add("Param: " + braced(param.getName()) + " cannot be " + getPrintableCSVs(paramTypes) + " at the same time");
                }

                if (param.isPathparam()) {
                    if (param.getPosition() <= 0) {
                        errors.add("Param: " + braced(param.getName()) + " has been marked as pathparam, it needs a position as well");
                    } else {
                        if (url != null) {
                            if (url.charAt(0) == '/') {
                                url = url.substring(1);
                            }

                            String[] parts = url.split("/");
                            if (param.getPosition() > parts.length) {
                                errors.add("Invalid position for pathparam: " + braced(param.getName()));
                            }
                        }
                    }
                }

                if (!isNullOrEmpty(param.getJavatype()) && param.getDatatype() != null) {
                    errors.add("Param: " + braced(param.getName()) + " cannot have both datatype and javatype");
                }

                if (param.isBody() && param.getDatatype() != null) {
                    errors.add("Param: " + braced(param.getName()) + " is a body param, it cannot have a datatype");
                }
            }
        }
        return errors;
    }

    private static List<String> detectDuplicateParams(ParamPOJO[] paramsOne, ParamPOJO[] paramsTwo) {
        List<String> errors = new ArrayList<>();
        if (paramsOne != null && paramsTwo != null) {
            for (ParamPOJO paramOne : paramsOne) {
                for (ParamPOJO paramTwo : paramsTwo) {
                    if (paramOne.getName() != null && paramTwo.getName() != null
                            && paramOne.getName().equalsIgnoreCase(paramTwo.getName())) {
                        errors.add("Param: " + braced(paramOne.getName()) + " exists as both required and optional parameter");
                    }
                }
            }
        }
        return errors;
    }

    private static void scanAndAdd(Path dir, List<String> validConfigs) {
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
                        validConfigs.add(config);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isEmpty(Object value) {
        if (value instanceof List) {
            return ((List) value).isEmpty();
        } else if (value instanceof Map) {
            return ((Map) value).isEmpty();
        } else if (value instanceof String) {
            return ((String) value).isEmpty();
        }
        return false;
    }

    private static String getPrintableCSVs(List list) {
        if (list.size() < 1) {
            return "";
        } else if (list.size() == 1) {
            return String.valueOf(list.get(0));
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size() - 2; i++) {
            builder.append(list.get(i)).append(", ");
        }
        builder.append(list.get(list.size() - 2)).append(" and ").append(list.get(list.size() - 1));
        return builder.toString();
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String stripBraces(String value) {
        if (value.length() < 5) {
            return value;
        }

        return value.substring(2, value.length() - 2);
    }

    private static String braced(String value) {
        return "{{ " + value + " }}";
    }
}
