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

import org.eclipse.jetty.servlet.FilterMapping;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.util.*;

/**
 * Created by shrey.garg on 21/05/16.
 */
public class JettyFilterConfiguration {
    private Filter filter;
    private List<String> mappings;
    private Map<String, String> initParameters = new HashMap<>();
    private EnumSet<DispatcherType> dispatcherTypes;

    public JettyFilterConfiguration(Filter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Filter configurations cannot be empty");
        }

        this.filter = filter;
        this.mappings = Collections.singletonList("/*");
        this.dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
    }

    public Filter getFilter() {
        return filter;
    }

    public List<String> getMappings() {
        return mappings;
    }

    public Map<String, String> getInitParameters() {
        return initParameters;
    }

    public EnumSet<DispatcherType> getDispatcherTypes() {
        return dispatcherTypes;
    }

    public void setMappings(List<String> mappings) {
        if (mappings == null) {
            throw new IllegalArgumentException("Filter configurations cannot be empty");
        }
        this.mappings = mappings;
    }

    public void setDispatcherTypes(EnumSet<DispatcherType> dispatcherTypes) {
        if (dispatcherTypes == null) {
            throw new IllegalArgumentException("Filter configurations cannot be empty");
        }
        this.dispatcherTypes = dispatcherTypes;
    }

    public void setInitParameters(Map<String, String> initParameters) {
        if (initParameters == null) {
            throw new IllegalArgumentException("Filter configurations cannot be empty");
        }

        this.initParameters = initParameters;
    }
}
