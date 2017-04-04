package com.flipkart.poseidon.serviceclients.mapper;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by prasad.krishna on 25/03/17.
 */
public enum Type {

    STRING("string", "java.lang.String"),
    NUMBER("number","java.lang.Double"),
    INTEGER("integer", "java.lang.Integer"),
    BOOLEAN("boolean", "java.lang.Boolean"),
    OBJECT("object", "java.lang.Object"),
    ARRAY("array","java.util.List"),
    MAP("map", "java.util.Map"),
    NULL("null","java.lang.Object"),
    ANY("any","java.lang.Object");

    private String jsonValue;
    private String packageName;

    private Type(String jsonValue, String packageName) {
        this.jsonValue = jsonValue;
        this.packageName = packageName;
    }

    @JsonValue
    public String getJsonValue() {
        return this.jsonValue;
    }

    public String getPackageName() {
        return packageName;
    }
}
