package com.flipkart.poseidon.handlers.http.multipart;

import org.apache.http.entity.ContentType;

/**
 * Created by chaitanya.naik on 2020-08-18.
 */
public class FileFormField extends FormField {

    private String fileName;

    public FileFormField(String name, ContentType contentType, byte[] data, String fileName) {
        super(name, contentType, data);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
