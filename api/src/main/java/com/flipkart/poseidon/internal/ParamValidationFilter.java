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

package com.flipkart.poseidon.internal;

import com.fasterxml.jackson.databind.JavaType;
import com.flipkart.poseidon.api.Configuration;
import com.flipkart.poseidon.constants.RequestConstants;
import com.flipkart.poseidon.core.PoseidonRequest;
import com.flipkart.poseidon.core.RequestContext;
import com.flipkart.poseidon.model.annotations.Description;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Trace;
import com.flipkart.poseidon.model.annotations.Version;
import com.flipkart.poseidon.pojos.ParamPOJO;
import com.flipkart.poseidon.pojos.ParamsPOJO;
import flipkart.lego.api.entities.Filter;
import flipkart.lego.api.entities.Request;
import flipkart.lego.api.entities.Response;
import flipkart.lego.api.exceptions.BadRequestException;
import flipkart.lego.api.exceptions.InternalErrorException;
import flipkart.lego.api.exceptions.ProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Trace(false)
@Name("ParamValidationFilter")
@Version(major = 1, minor = 0, patch = 0)
@Description("Poseidon filter to validate API parameters")
public class ParamValidationFilter implements Filter {

    private final ParamsPOJO params;
    private final Configuration configuration;

    private static final Logger logger = getLogger(ParamValidationFilter.class);

    public ParamValidationFilter() {
        this(new ParamsPOJO(), null);
    }

    public ParamValidationFilter(ParamsPOJO params, Configuration configuration) {
        this.params = params;
        this.configuration = configuration;
    }

    @Override
    public void filterRequest(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {
        PoseidonRequest poseidonRequest = (PoseidonRequest) request;
        Map<String, Object> parsedParams = new HashMap<>();
        if (params != null) {
            parsedParams.putAll(validateParams(poseidonRequest, params.getRequired(), true));
            parsedParams.putAll(validateParams(poseidonRequest, params.getOptional(), false));
        }
        poseidonRequest.setAttribute(RequestConstants.PARAMS, parsedParams);
    }

    @Override
    public void filterResponse(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {

    }

    private Map<String, Object> validateParams(PoseidonRequest poseidonRequest, ParamPOJO[] params, boolean failOnMissingValue) throws BadRequestException {
        Map<String, Object> parsedParams = new HashMap<>(); //Initial params that will be passed to hydra to START processing
        if(params != null) {
            final List<ParamPOJO> pathParams = new ArrayList<>();
            for (ParamPOJO param : params) {
                String name = param.getName();
                String internalName = param.getInternalName();
                ParamPOJO.DataType datatype = param.getDatatype();
                Object defaultValue = param.getDefaultValue();
                String separator = param.getSeparator();
                boolean multivalue = param.getMultivalue() || separator != null;
                boolean isBodyRequest = param.isBody();
                boolean isHeader = param.isHeader();
                boolean isPathParam = param.isPathparam();
                Object value = null;
                if(isHeader) {
                    String attribute = poseidonRequest.getHeader(name);
                    if (failOnMissingValue && attribute == null) {
                        throw new BadRequestException("Missing header : " + name);
                    }
                    if (!failOnMissingValue && attribute == null && defaultValue != null) {
                        // Optional param, value is not present but default is specified
                        value = defaultValue;
                    } else {
                        value = parseParamValues(name, new String[] { attribute }, datatype, multivalue, param.getJavaType());
                    }
                } else if(isBodyRequest) {
                    final String bodyString = poseidonRequest.getAttribute(RequestConstants.BODY);
                    final byte[] bodyBytes = poseidonRequest.getAttribute(RequestConstants.BODY_BYTES);
                    if(!StringUtils.isEmpty(bodyString)) {
                        try {
                            if (param.getJavaType() == null && param.getDatatype() == null) {
                                value = bodyString;
                            } else {
                                value = configuration.getObjectMapper().readValue(bodyString, param.getJavaType());
                            }
                        } catch (IOException e) {
                            logger.error("Error in reading body : {}", e.getMessage());
                        }
                    } else if (bodyBytes != null && bodyBytes.length > 0) {
                        try {
                            if (param.getJavaType() == null && param.getDatatype() == null) {
                                value = configuration.getObjectMapper().writeValueAsString(bodyBytes);
                            } else {
                                value = configuration.getObjectMapper().readValue(bodyBytes, param.getJavaType());
                            }
                        } catch (IOException e) {
                            logger.error("Error in reading body : {}", e.getMessage());
                        }
                    }
                    if(failOnMissingValue && value == null) {
                        throw new BadRequestException("Request Body is either missing or invalid for : " + name);
                    }
                } else if (isPathParam) {
                    param.setGreedyPosition(param.getPosition());
                    pathParams.add(param);
                } else if (param.isFile()) {
                    value = poseidonRequest.getAttribute(name);
                    if (failOnMissingValue && value == null) {
                        throw new BadRequestException("Missing parameter : " + name);
                    }
                } else {
                    Object attribute = poseidonRequest.getAttribute(name);

                    if (failOnMissingValue && attribute == null) {
                        throw new BadRequestException("Missing parameter : " + name);
                    }

                    if (multivalue && separator != null && attribute != null) {
                        attribute = ((String[]) attribute)[0].split(separator);
                    }

                    if (!failOnMissingValue && attribute == null && defaultValue != null) {
                        // Optional param, value is not present but default is specified
                        value = defaultValue;
                    } else {
                        value = parseParamValues(name, (String[]) attribute, datatype, multivalue, param.getJavaType());
                    }
                }

                if (internalName != null && !internalName.isEmpty()) {
                    parsedParams.put(internalName, value);
                } else {
                    parsedParams.put(name, value);
                }
            }

            if (!pathParams.isEmpty()) {
                pathParams.sort((a, b) -> a.getPosition() - b.getPosition());
                for (int i = 0; i < pathParams.size(); i++) {
                    ParamPOJO param = pathParams.get(i);
                    String name = param.getName();
                    String internalName = param.getInternalName();
                    String value = null;
                    ParamPOJO.DataType datatype = param.getDatatype();

                    int pos = param.getPosition();
                    int greedyPos = param.getGreedyPosition();
                    String[] splitUrl = poseidonRequest.getUrl().split("/");
                    String[] splitActual = ((String) RequestContext.get(RequestConstants.URI)).split("/");

                    if (pos >= splitUrl.length) {
                        throw new BadRequestException("Missing path parameter : " + name);
                    }

                    boolean isGreedyParam = splitActual[pos].length() >= 2 && splitActual[pos].startsWith("*") && splitActual[pos].endsWith("*");

                    if (splitUrl.length == splitActual.length || !isGreedyParam) {
                        value = splitUrl[greedyPos];
                    } else if (isGreedyParam) {
                        int nextPos = greedyPos + 1;
                        if (pos == splitActual.length - 1) {
                            nextPos = splitUrl.length;
                        } else {
                            for (int j = greedyPos + 1; j < splitUrl.length; j++) {
                                if (splitActual[pos + 1].equals(splitUrl[j])) {
                                    nextPos = j;
                                    break;
                                }
                            }
                        }

                        for (int j = i + 1; j < pathParams.size(); j++) {
                            ParamPOJO nextParam = pathParams.get(j);
                            nextParam.setGreedyPosition(nextParam.getGreedyPosition() + (nextPos - greedyPos - 1));
                        }

                        value = getGreedyPathParam(greedyPos, nextPos, splitUrl);
                    }

                    if (value == null) {
                        throw new BadRequestException("Missing path parameter : " + name);
                    }

                    Object convertedValue = parseParamValues(name, new String[] { value }, datatype, false, param.getJavaType());
                    if (internalName != null && !internalName.isEmpty()) {
                        parsedParams.put(internalName, convertedValue);
                    } else {
                        parsedParams.put(name, convertedValue);
                    }
                }
            }
        }
        return parsedParams;
    }

    private String getGreedyPathParam(int start, int end, String[] url) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < end; i++) {
            builder.append(url[i]).append("/");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    private Object parseParamValues(String name, String[] values, ParamPOJO.DataType datatype, boolean multivalue, JavaType javaType) throws BadRequestException {
        try {
            if (values != null) {
                if (!multivalue && values.length > 1) {
                    throw new BadRequestException("Multiple values provided for parameter : " + name);
                }

                List parsedValues = getValues(values, datatype, javaType);
                return multivalue ? parsedValues : parsedValues.get(0);
            }

            return null;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Incorrect value provided for parameter : " + name + " (Expected value - " + datatype + ")");
        }
    }

    private List getValues(String[] values, ParamPOJO.DataType datatype, JavaType javaType) throws BadRequestException {
        switch (datatype) {
            case NUMBER:
                return getDoubleValues(values);
            case INTEGER:
                return getIntegerValues(values);
            case LONG:
                return getLongValues(values);
            case BOOLEAN:
                return getBooleanValues(values);
            case ENUM:
                return getEnumValues(values, javaType);
        }

        return getStringValues(values);
    }

    private List<Double> getDoubleValues(String[] values) {
        List<Double> doubleValues = new ArrayList<>();
        for (String value : values) {
            doubleValues.add(Double.parseDouble(value));
        }

        return doubleValues;
    }

    private List<Integer> getIntegerValues(String[] values) {
        List<Integer> integerValues = new ArrayList<>();
        for (String value : values) {
            integerValues.add(Integer.parseInt(value));
        }

        return integerValues;
    }

    private List<Long> getLongValues(String[] values) {
        return Arrays.stream(values).map(Long::parseLong).collect(Collectors.toList());
    }

    private List<Boolean> getBooleanValues(String[] values) {
        List<Boolean> booleanValues = new ArrayList<>();
        for (String value : values) {
            booleanValues.add(Boolean.parseBoolean(value));
        }

        return booleanValues;
    }

    private List<String> getStringValues(String[] values) {
        return Arrays.asList(values);
    }

    private List getEnumValues(String[] values, JavaType javaType) throws BadRequestException {
        List enumValues = new ArrayList<>();
        for (String value : values) {
            try {
                enumValues.add(configuration.getObjectMapper().convertValue(value, javaType));
            } catch (IllegalArgumentException e) {
                logger.error("Wrong value passed for enum : {}", e.getMessage());
                throw new BadRequestException("Wrong value passed for enum, javatype: " + javaType + " value: " + value);
            } catch (Exception e) {
                logger.error("Error in reading enum : {}", e.getMessage());
                throw new BadRequestException("Error in reading enum, javatype: " + javaType + " value: " + value);
            }
        }

        return enumValues;
    }
}
