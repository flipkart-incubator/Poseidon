package com.flipkart.poseidon.serviceclients.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by prasad.krishna on 24/03/17.
 */
public class FieldDesc {

    private Type type;
    @JsonProperty("enum")
    private List<String> enumeration;
    private List<String> javaEnumNames;
    private String javaType;
    private String description;
    private boolean usePrimitives;
    private boolean required;
    private FieldDesc items;
    private FormatType format;
    private boolean optional;
    @JsonProperty("default")
    private Object defaultValue;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<String> getEnumeration() {
        return enumeration;
    }

    public void setEnumeration(List<String> enumeration) {
        this.enumeration = enumeration;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isUsePrimitives() {
        return usePrimitives;
    }

    public void setUsePrimitives(boolean usePrimitives) {
        this.usePrimitives = usePrimitives;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public FieldDesc getItems() {
        return items;
    }

    public void setItems(FieldDesc items) {
        this.items = items;
    }

    public FormatType getFormat() {
        return format;
    }

    public void setFormat(FormatType format) {
        this.format = format;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<String> getJavaEnumNames() {
        return javaEnumNames;
    }

    public void setJavaEnumNames(List<String> javaEnumNames) {
        this.javaEnumNames = javaEnumNames;
    }
}
