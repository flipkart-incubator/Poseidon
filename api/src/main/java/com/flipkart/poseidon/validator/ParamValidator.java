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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.flipkart.poseidon.validator.ValidatorUtils.braced;
import static com.flipkart.poseidon.validator.ValidatorUtils.getPrintableCSVs;
import static com.flipkart.poseidon.validator.ValidatorUtils.isNullOrEmpty;

/**
 * Created by shrey.garg on 06/07/16.
 */
public class ParamValidator {
    private static final List<String> mutuallyExclusiveParamTypes = Arrays.asList("pathparam", "body", "header", "multivalue");

    public static List<String> validate(ParamsPOJO value, String url) throws IllegalAccessException {
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

                if ((param.getDatatype() != null && param.getDatatype() == ParamPOJO.DataType.ENUM) && (param.getType() == null || isNullOrEmpty(param.getType().getType()))) {
                    errors.add("Param: " + braced(param.getName()) + " does not have type defined for ENUM parameter");
                }

                if (!isNullOrEmpty(param.getJavatype()) && (param.getDatatype() != null && param.getDatatype() != ParamPOJO.DataType.ENUM)) {
                    errors.add("Param: " + braced(param.getName()) + " cannot have both datatype and javatype except for ENUM types");
                }

                if (param.isBody() && param.getDatatype() != null) {
                    errors.add("Param: " + braced(param.getName()) + " is a body param, it cannot have a datatype");
                }
            }
        }
        return errors;
    }

    public static List<String> getMarkedParamTypes(ParamPOJO param) throws IllegalAccessException {
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

    public static List<String> detectDuplicateParams(ParamPOJO[] paramsOne, ParamPOJO[] paramsTwo) {
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
}
