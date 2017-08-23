package com.flipkart.poseidon.validator;

import com.flipkart.poseidon.pojos.EndpointPOJO;

import java.util.List;

/**
 * Created by shrey.garg on 27/07/17.
 */
public interface CustomValidator {
    List<String> validate(EndpointPOJO pojo);
}
