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

import java.util.Map;
import java.util.Objects;

/**
 * Created by mohan.pandian on 17/02/15.
 */
public class ServiceIDL {
    private Version version;
    private boolean explicit;
    private Service service;
    private Map<String, Parameter> parameters;
    private Map<String, EndPoint> endPoints;
    private Map<Integer, String> exceptions;

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }

    public Map<String, EndPoint> getEndPoints() {
        return endPoints;
    }

    public void setEndPoints(Map<String, EndPoint> endPoints) {
        this.endPoints = endPoints;
    }

    public Map<Integer, String> getExceptions() { return exceptions; }

    public void setExceptions(Map<Integer, String> exceptions) { this.exceptions = exceptions; }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof ServiceIDL)) {
            return false;
        }
        if (this == object) {
            return true;
        }

        ServiceIDL serviceIDL = (ServiceIDL) object;
        if (!Objects.equals(service, serviceIDL.getService())) {
            return false;
        }
        if (!Objects.equals(parameters, serviceIDL.getParameters())) {
            return false;
        }
        if (!Objects.equals(endPoints, serviceIDL.getEndPoints())) {
            return false;
        }
        if (!Objects.equals(exceptions, serviceIDL.getExceptions())) {
            return false;
        }

        return true;
    }

}
