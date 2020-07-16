package com.flipkart.poseidon.serviceclients.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Created by prasad.krishna on 24/03/17.
 */
public class ServiceClientPojo {

    private Type type;
    private Object additionalProperties;
    private String description;
    private Map<String, FieldDescriptor> properties;
    @JsonProperty("enum")
    private List<String> enumeration;
    private List<String> javaEnumNames;
    @JsonProperty("extends")
    private FieldDescriptor extendedClass;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Object getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Object additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, FieldDescriptor> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, FieldDescriptor> properties) {
        this.properties = properties;
    }

    public List<String> getEnumeration() {
        return enumeration;
    }

    public void setEnumeration(List<String> enumeration) {
        this.enumeration = enumeration;
    }

    public FieldDescriptor getExtendedClass() {
        return extendedClass;
    }

    public void setExtendedClass(FieldDescriptor extendedClass) {
        this.extendedClass = extendedClass;
    }

    public List<String> getJavaEnumNames() {
        return javaEnumNames;
    }

    public void setJavaEnumNames(List<String> javaEnumNames) {
        this.javaEnumNames = javaEnumNames;
    }
}
