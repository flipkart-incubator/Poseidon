package com.flipkart.poseidon.model.oas;

/**
 * Created by shrey.garg on 2019-01-08.
 */
public @interface Response {
    int status();
    Class<?> responseClass();
}
