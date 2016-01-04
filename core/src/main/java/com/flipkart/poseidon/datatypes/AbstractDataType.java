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

package com.flipkart.poseidon.datatypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import flipkart.lego.api.entities.DataType;

import java.util.List;

import static com.flipkart.poseidon.datasources.util.CallableNameHelper.canonicalName;

/**
 * AbstractDataType is a {@link DataType} with default implementations for
 * {@link flipkart.lego.api.helpers.Identifiable} and {@link flipkart.lego.api.helpers.Describable}
 */
public abstract class AbstractDataType implements DataType {
    @Override
    @JsonIgnore
    public String getId() throws UnsupportedOperationException {
        return getName() + "_" + Joiner.on(".").join(getVersion());
    }

    @Override
    @JsonIgnore
    public String getName() throws UnsupportedOperationException {
        return canonicalName(getClass().getSimpleName(), "DataType", "DT");
    }

    @Override
    @JsonIgnore
    public List<Integer> getVersion() throws UnsupportedOperationException {
        return Lists.newArrayList(1, 0, 0);
    }

    @Override
    @JsonIgnore
    public String getShortDescription() {
        return this.getClass().getSimpleName();
    }

    @Override
    @JsonIgnore
    public String getDescription() {
        return this.getClass().getName();
    }
}
