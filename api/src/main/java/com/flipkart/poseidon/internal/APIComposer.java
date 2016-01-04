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

import com.flipkart.hydra.composer.DefaultComposer;
import com.flipkart.hydra.composer.exception.ComposerEvaluationException;
import com.flipkart.hydra.composer.exception.ComposerInstantiationException;
import com.flipkart.poseidon.mappers.Mapper;

import java.util.*;

/**
 * APIComposer extends {@link DefaultComposer} to provide Mapper functionalities
 */
public class APIComposer extends DefaultComposer {
    private final Map<String, Object> allDependecies = new HashMap<>();
    private final Set<Mapper> mappers;
    private final List<Object> mappedBeans;

    public APIComposer(Object context, Map<String, Object> initialParams, Set<Mapper> mappers, List<Object> mappedBeans) throws ComposerInstantiationException {
        super(context);
        allDependecies.putAll(initialParams);
        this.mappers = mappers;
        this.mappedBeans = mappedBeans;
    }

    @Override
    public Object compose(Map<String, Object> values) throws ComposerEvaluationException {
        Object response = super.compose(values);

        allDependecies.putAll(values);
        for(Mapper mapper: mappers) {
            Object mappedBean = mapper.map(allDependecies);
            if (mappedBean != null) {
                mappedBeans.add(mappedBean);
            }
        }
        return response;
    }
}
