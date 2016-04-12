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

import com.sun.codemodel.JCodeModel;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by mohan.pandian on 13/03/15.
 *
 * Uses jsonschema2pojo (https://github.com/joelittlejohn/jsonschema2pojo)
 * to generate pojos from json schema files
 */
public class PojoGenerator {
    private static final PojoGenerator POJO_GENERATOR = new PojoGenerator();
    private GenerationConfig generationConfig = new DefaultGenerationConfig() {
        @Override
        public boolean isIncludeHashcodeAndEquals() {
            return false;
        }

        @Override
        public boolean isIncludeToString() {
            return false;
        }
    };
    private RuleFactory ruleFactory = new RuleFactory(generationConfig, new Jackson2Annotator(), new SchemaStore());
    private SchemaMapper mapper = new SchemaMapper(ruleFactory, new SchemaGenerator());

    private PojoGenerator() {}

    public static PojoGenerator getInstance() {
        return POJO_GENERATOR;
    }

    public void generate(String pojoJson, JCodeModel jCodeModel, String destinationFolder, String className, String packageName) throws IOException {
        mapper.generate(jCodeModel, className, packageName, pojoJson);
        jCodeModel.build(new File(destinationFolder), (PrintStream) null);
    }
}
