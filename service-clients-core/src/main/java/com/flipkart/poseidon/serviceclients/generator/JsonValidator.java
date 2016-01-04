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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by mohan.pandian on 15/04/15.
 *
 * Validates pojo and service json files
 */
public class JsonValidator {
    private final static Logger logger = LoggerFactory.getLogger(JsonValidator.class);
    private static final JsonValidator JSON_VALIDATOR = new JsonValidator();

    private JsonValidator() {
    }

    public static JsonValidator getInstance() {
        return JSON_VALIDATOR;
    }

    private void validate(String schemaPath, String filePath) throws IOException, ProcessingException {
        JsonNode schema = JsonLoader.fromResource(schemaPath);
        JsonNode json = JsonLoader.fromPath(filePath);
        com.github.fge.jsonschema.main.JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();
        ProcessingReport report = validator.validate(schema, json);

        if ((report == null) || !report.isSuccess()) {
            logger.error("Invalid JSON");
            if (report != null) {
                throw new ProcessingException(report.toString());
            } else {
                throw new ProcessingException("JSON validation report is null for " + filePath);
            }
        }
    }

    public void validateService(String filePath) throws IOException, ProcessingException {
        validate("/ServiceJsonSchema.json", filePath);
    }

    public void validatePojo(String filePath) throws IOException, ProcessingException {
        validate("/PojoJsonSchema.json", filePath);
    }
}
