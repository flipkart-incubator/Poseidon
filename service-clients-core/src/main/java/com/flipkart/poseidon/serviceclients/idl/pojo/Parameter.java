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

package com.flipkart.poseidon.serviceclients.idl.pojo;

import java.util.Arrays;
import java.util.Objects;

/**
 * Created by mohan.pandian on 17/02/15.
 */
public class Parameter {
    private String type;
    private Boolean optional = false;
    private String[] description;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public String[] getDescription() {
        return description;
    }

    public void setDescription(String[] description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Parameter)) {
            return false;
        }
        if (this == object) {
            return true;
        }

        Parameter parameter = (Parameter) object;
        if (!Objects.equals(type, parameter.getType())) {
            return false;
        }
        if (!Objects.equals(optional, parameter.getOptional())) {
            return false;
        }
        if (!Arrays.equals(description, parameter.getDescription())) {
            return false;
        }
        return true;
    }
}
