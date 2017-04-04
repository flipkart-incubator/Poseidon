package com.flipkart.poseidon.serviceclients.mapper;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by prasad.krishna on 31/03/17.
 */
public enum FormatType {

    DATE_TIME("date-time", "java.util.Date"),
    DATE("date", "String"),
    TIME("time", "String"),
    UTC("utc-millisec", "java.lang.Long"),
    REGEX("regex", "java.util.regex.Pattern"),
    COLOR("color","String"),
    STYLE("style", "String"),
    PHONE("phone","String"),
    URI("uri","java.net.URI"),
    EMAIL("email","String"),
    IP("ip-address","String"),
    IPV6("ipv6", "String"),
    HOST("host-name","String"),
    UUID("uuid","java.util.UUID");


    private String value;
    private String packageName;

    private FormatType(String value, String packageName) {
        this.value = value;
        this.packageName = packageName;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getPackageName() {
        return packageName;
    }
}
