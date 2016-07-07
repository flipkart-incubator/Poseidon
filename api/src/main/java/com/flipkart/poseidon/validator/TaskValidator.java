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

import com.flipkart.poseidon.pojos.ParamPOJO;
import com.flipkart.poseidon.pojos.ParamsPOJO;
import com.flipkart.poseidon.pojos.TaskPOJO;

import java.util.*;

import static com.flipkart.poseidon.validator.ValidatorUtils.braced;
import static com.flipkart.poseidon.validator.ValidatorUtils.isNullOrEmpty;
import static com.flipkart.poseidon.validator.ValidatorUtils.stripBraces;

/**
 * Created by shrey.garg on 06/07/16.
 */
public class TaskValidator {
    public static List<String> validate(Map<String, TaskPOJO> tasks, ParamsPOJO params) {
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
                } else {
                    errors.add("Param: " + braced(fullContextParam) + " used in Task: " + braced(taskName) + " is not an expression");
                }

                String contextParam = fullContextParam.split("\\.")[0];

                boolean located = false;
                if (contextParam != null && !contextParam.isEmpty()) {
                    if (params != null) {
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
}
