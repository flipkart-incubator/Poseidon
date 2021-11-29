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

import com.flipkart.poseidon.datasources.RequestAttribute;
import com.flipkart.poseidon.pojos.ParamPOJO;
import com.flipkart.poseidon.pojos.ParamsPOJO;
import com.flipkart.poseidon.pojos.TaskPOJO;
import flipkart.lego.api.entities.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

import static com.flipkart.poseidon.validator.ValidatorUtils.*;

/**
 * Created by shrey.garg on 06/07/16.
 */
public class TaskValidator {

    private static final Logger logger = LogFactory.getLogger(TaskValidator.class);

    public static List<String> validate(Map<String, TaskPOJO> tasks, ParamsPOJO params, Map<String, Class<? extends DataSource<?>>> datasources, boolean validateDataSources) {
        List<String> errors = new ArrayList<>();
        boolean skipUnusedFieldValidation = Boolean.valueOf(System.getProperty("poseidon.skip.unused_params.validator"));
        for (Map.Entry<String, TaskPOJO> entry : tasks.entrySet()) {
            String taskName = entry.getKey();
            TaskPOJO task = entry.getValue();
            final Class<? extends DataSource<?>> dsClass = datasources.get(task.getName());
            if (isNullOrEmpty(task.getName())) {
                errors.add("No datasource defined for Task: " + braced(taskName));
            } else if (dsClass == null && validateDataSources) {
                errors.add("Datasource used does not exist for Task: " + braced(taskName));
            }

            final Set<String> datasourceRequestAttributes = new HashSet<>();
            if (dsClass != null && validateDataSources) {
                final List<? extends Constructor<?>> injectableConstructors = findInjectableConstructors(dsClass);
                if (!injectableConstructors.isEmpty()) {
                    final Constructor<?> constructor = injectableConstructors.get(0);
                    final Parameter[] parameters = constructor.getParameters();
                    for (Parameter parameter : parameters) {
                        final RequestAttribute requestAttribute = parameter.getAnnotation(RequestAttribute.class);
                        if (requestAttribute != null) {
                            datasourceRequestAttributes.add(Optional.of(requestAttribute.value()).filter(StringUtils::isNotEmpty).orElse(parameter.getName()));
                        }
                    }
                }
            }

            final Map<String, Object> context = Optional.ofNullable(task.getContext()).orElse(new HashMap<>());
            for (Map.Entry<String, Object> contextEntry : context.entrySet()) {
                final String key = contextEntry.getKey();
                if (!datasourceRequestAttributes.contains(key)) {
                    if (!skipUnusedFieldValidation) {
                        errors.add("ContextParam: " + braced(key) + " used in Task: " + braced(taskName) + " is not used in the Datasource");
                    }else {
                        logger.info("Unused fields validation is disabled");
                    }
                }

                Object contextEntryValue = contextEntry.getValue();
                // Could be literals like boolean true/false
                if (!(contextEntryValue instanceof String)) {
                    continue;
                }
                String fullContextParam = stripBraces((String) contextEntryValue);
                boolean isOptional = fullContextParam.charAt(0) == '#';
                if (isOptional) {
                    fullContextParam = fullContextParam.substring(1);
                }

                boolean isExpression = fullContextParam.charAt(0) == '$';
                if (isExpression) {
                    fullContextParam = fullContextParam.substring(1);
                } else {
                    continue;
                }

                String contextParam = fullContextParam.split("\\.")[0];

                boolean located = false;
                if (contextParam != null && !contextParam.isEmpty()) {
                    if (params != null) {
                        if (params.getRequired() != null) {
                            for (ParamPOJO param : params.getRequired()) {
                                if (contextParam.equals(param.getName()) || contextParam.equals(param.getInternalName())) {
                                    located = true;
                                    if (isOptional) {
                                        errors.add("Param: " + braced(param.getName()) + " used in Task: " + braced(taskName) + " is not optional");
                                    }
                                }
                            }
                        }

                        if (params.getOptional() != null) {
                            for (ParamPOJO param : params.getOptional()) {
                                if (contextParam.equals(param.getName()) || contextParam.equals(param.getInternalName())) {
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

    private static  <T> List<Constructor<T>> findInjectableConstructors(Class<T> klass) {
        final Constructor<T>[] declaredConstructors = (Constructor<T>[]) klass.getDeclaredConstructors();
        final List<Constructor<T>> injectableConstructors = Arrays.stream(declaredConstructors).filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if (injectableConstructors.size() > 1) {
            throw new UnsupportedOperationException(klass.getName() + " has more than one injectable constructor");
        }
        return injectableConstructors;
    }
}
