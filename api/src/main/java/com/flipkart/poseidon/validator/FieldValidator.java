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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.flipkart.poseidon.validator.ValidatorUtils.isEmpty;

/**
 * Created by shrey.garg on 06/07/16.
 */
public class FieldValidator {
    private static final List<String> notNullFields = Arrays.asList("tasks", "httpMethod", "url");

    public static List<String> validate(EndpointPOJO pojo) throws IllegalAccessException {
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

        errors.addAll(validateUrl(pojo.getUrl()));

        errors.addAll(validateTimeout(pojo.getTimeout()));

        return errors;
    }

    public static List<String> validateUrl(String value) {
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

            if (!value.startsWith("/")) {
                errors.add("Invalid Url. Start the url with a \'/\'");
            }

            if (value.endsWith("/")) {
                errors.add("Invalid Url. Do not end the url with a '/'");
            }
        }

        return errors;
    }

    public static List<String> validateTimeout(long value) {
        List<String> errors = new ArrayList<>();

        if (value <= 0) {
            errors.add("No or invalid timeout defined.");
        }

        return errors;
    }
}
