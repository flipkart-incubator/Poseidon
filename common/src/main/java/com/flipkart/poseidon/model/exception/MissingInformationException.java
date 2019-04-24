package com.flipkart.poseidon.model.exception;

/**
 * Created by shrey.garg on 01/11/16.
 */
public class MissingInformationException extends Exception {
    public MissingInformationException() {
    }

    public MissingInformationException(String message) {
        super(message);
    }
}
