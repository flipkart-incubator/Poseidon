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

import com.google.common.base.Joiner;
import flipkart.lego.api.entities.DataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ListDataType<T> extends ArrayList<T> implements DataType {

    public ListDataType(Collection<? extends T> collection) {
        super(collection);
    }

    @Override
    public String getShortDescription() {
        return "A ArrayList based DataType, used just to honor the datasource contract.";
    }

    @Override
    public String getDescription() {
        return getShortDescription();
    }
}
