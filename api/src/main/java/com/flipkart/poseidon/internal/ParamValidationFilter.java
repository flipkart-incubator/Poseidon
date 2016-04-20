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

import com.flipkart.poseidon.pojos.ParamPOJO;
import com.flipkart.poseidon.pojos.ParamsPOJO;
import com.flipkart.poseidon.constants.RequestConstants;
import com.flipkart.poseidon.core.PoseidonRequest;
import com.google.common.base.Joiner;
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

import static com.flipkart.poseidon.datasources.util.CallableNameHelper.canonicalName;
import static com.flipkart.poseidon.helpers.ObjectMapperHelper.getMapper;
import static org.slf4j.LoggerFactory.getLogger;

public class ParamValidationFilter implements Filter {

    private final ParamsPOJO params;

    private static final Logger logger = getLogger(ParamValidationFilter.class);

    public ParamValidationFilter() {
        this(new ParamsPOJO());
    }

    public ParamValidationFilter(ParamsPOJO params) {
        this.params = params;
    }

    @Override
    public void filterRequest(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {
        PoseidonRequest poseidonRequest = (PoseidonRequest) request;
        Map<String, Object> parsedParams = new HashMap<>();
        parsedParams.putAll(validateParams(poseidonRequest, params.getRequired(), true));
        parsedParams.putAll(validateParams(poseidonRequest, params.getOptional(), false));
        poseidonRequest.setAttribute(RequestConstants.PARAMS, parsedParams);
    }

    @Override
    public void filterResponse(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {

    }

    private Map<String, Object> validateParams(PoseidonRequest poseidonRequest, ParamPOJO[] params, boolean failOnMissingValue) throws BadRequestException {
        Map<String, Object> parsedParams = new HashMap<>(); //Initial params that will be passed to hydra to START processing
        if(params != null) {
            for (ParamPOJO param : params) {
                String name = param.getName();
                String internalName = param.getInternalName();
                ParamPOJO.DataType datatype = param.getDatatype();
                boolean multivalue = param.getMultivalue();
                boolean isBodyRequest = param.isBody();
                boolean isHeader = param.isHeader();
                boolean isPathParam = param.isPathparam();
                Object value = null;
                if(isHeader) {
                    String attribute = poseidonRequest.getHeader(name);
                    if (failOnMissingValue && attribute == null) {
                        throw new BadRequestException("Missing header : " + name);
                    }
                    value = attribute;
                }
                else if(isBodyRequest) {
                    String bodyString = poseidonRequest.getAttribute(RequestConstants.BODY);
                    if(!StringUtils.isEmpty(bodyString)) {
                        try {
                            if ((param.getJavatype() == null || param.getJavatype().isEmpty()) &&
                                    (param.getDatatype() == null)) {
                                value = bodyString;
                            } else {
                                value = getMapper().readValue(bodyString, Class.forName(param.getJavatype()));
                            }
                        } catch (IOException e) {
                            logger.error("Error in reading body : {}", e.getMessage());
                        } catch (ClassNotFoundException e) {
                            logger.error("Error in finding class for body : {}", e.getMessage());
                        }
                    }
                    if(failOnMissingValue && value == null) {
                        throw new BadRequestException("Request Body is either missing or invalid for : " + name);
                    }
                } else if (isPathParam) {
                    int pos = param.getPosition();
                    String[] splitUrl = poseidonRequest.getUrl().split("/");
                    value = splitUrl[pos];
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
                    value = parseParamValues(name, (String[]) attribute, datatype, multivalue);
                }
                if (internalName != null && !internalName.isEmpty()) {
                    parsedParams.put(internalName, value);
                } else {
                    parsedParams.put(name, value);
                }
            }
        }
        return parsedParams;
    }

    private Object parseParamValues(String name, String[] values, ParamPOJO.DataType datatype, boolean multivalue) throws BadRequestException {
        try {
            if (values != null) {
                if (!multivalue && values.length > 1) {
                    throw new BadRequestException("Multiple values provided for parameter : " + name);
                }

                List parsedValues = getValues(values, datatype);
                return multivalue ? parsedValues : parsedValues.get(0);
            }

            return null;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Incorrect value provided for parameter : " + name + " (Expected value - " + datatype + ")");
        }
    }

    private List getValues(String[] values, ParamPOJO.DataType datatype) {
        switch (datatype) {
            case NUMBER:
                return getDoubleValues(values);
            case INTEGER:
                return getIntegerValues(values);
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

    private List<String> getStringValues(String[] values) {
        return Arrays.asList(values);
    }

    @Override
    public String getShortDescription() {
        return "Internal parameter filter to validate and extract the mentioned parameters from the request.";
    }

    @Override
    public String getDescription() {
        return getShortDescription();
    }

    @Override
    public String getId() throws UnsupportedOperationException {
        return getName() + "_" + Joiner.on(".").join(getVersion());
    }

    @Override
    public String getName() throws UnsupportedOperationException {
        return canonicalName(getClass().getSimpleName(), "Filter", "Filter");
    }

    @Override
    public List<Integer> getVersion() throws UnsupportedOperationException {
        return Arrays.asList(1, 0, 0);
    }
}
