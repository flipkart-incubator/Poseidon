package com.flipkart.poseidon.handlers.http.multipart;

import org.apache.http.entity.ContentType;

/**
 * Created by chaitanya.naik on 2020-08-17.
 */
public class FormField {
    private String name;
    private ContentType contentType;
    private byte[] data;

    public FormField(String name, ContentType contentType, byte[] data) {
        this.name = name;
        this.contentType = contentType;
        this.data = data;
    }

    public FormField(String name, ContentType contentType, String data) {
        this(name, contentType, data.getBytes());
    }

    public String getName() {
        return name;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }
}
