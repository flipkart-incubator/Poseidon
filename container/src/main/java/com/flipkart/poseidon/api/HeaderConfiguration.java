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

package com.flipkart.poseidon.api;

import java.util.function.Supplier;

/**
 * Created by shrey.garg on 06/04/16.
 */
public class HeaderConfiguration {
    private String name;
    private Supplier<String> defaultValue;

    public HeaderConfiguration(String name) {
        this(name, null);
    }

    public HeaderConfiguration(String name, Supplier<String> defaultValue) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Header name can't be empty");
        }
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue == null ? null : defaultValue.get();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HeaderConfiguration)) {
            return false;
        }
        if (this == o) {
            return true;
        }

        HeaderConfiguration that = (HeaderConfiguration) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
