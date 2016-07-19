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

package com.flipkart.poseidon.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ParamPOJO {

    private String name;
    private String internalName;
    private DataType datatype;
    /*
     * Used only for optional query params and headers (including multivalue)
     * Please provide constants in corresponding datatype.
     * Ex:
     *   "name": "myFlag",
     *   "datatype": "Boolean",
     *   "defaultValue": true
     */
    private Object defaultValue;
    private boolean multivalue;
    private String separator;
    private boolean file;
    private boolean body = false;
    private String javatype;
    private boolean header = false;
    private boolean pathparam = false;
    private int position = 0;
    @JsonIgnore
    private int greedyPosition = 0;

    public String getName() {
        return name;
    }

    public String getInternalName() {
        return internalName;
    }

    public DataType getDatatype() {
        return datatype;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean getMultivalue() {
        return multivalue;
    }

    public boolean isFile() {
        return file;
    }

    public boolean isBody() {
        return body;
    }

    public String getJavatype() {
        return javatype;
    }

    public boolean isHeader() {
        return header;
    }

    public boolean isPathparam() {return pathparam; }

    public int getPosition() {return  position; }

    public int getGreedyPosition() {
        return greedyPosition;
    }

    public void setGreedyPosition(int greedyPosition) {
        this.greedyPosition = greedyPosition;
    }

    public String getSeparator() {
        return separator;
    }

    public static enum DataType {
        STRING, INTEGER, NUMBER, BOOLEAN;

        @JsonCreator
        public static DataType getValueFor(String value) {
            return valueOf(value.toUpperCase());
        }
    }
}
