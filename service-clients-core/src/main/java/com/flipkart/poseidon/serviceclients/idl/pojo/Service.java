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
import java.util.Map;
import java.util.Objects;

/**
 * Created by mohan.pandian on 17/02/15.
 */
public class Service {
    private String name;
    private String packageName;
    private String baseUri;
    private Map<String, String> headers;
    private String[] description;
    private String commandName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String[] getDescription() {
        return description;
    }

    public void setDescription(String[] description) {
        this.description = description;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Service)) {
            return false;
        }
        if (this == object) {
            return true;
        }

        Service service = (Service) object;
        if (!Objects.equals(baseUri, service.getBaseUri())) {
            return false;
        }
        if (!Objects.equals(headers, service.getHeaders())) {
            return false;
        }
        if (!Arrays.equals(description, service.getDescription())) {
            return false;
        }
        if (!Objects.equals(commandName, service.getCommandName())) {
            return false;
        }

        return true;
    }
}
