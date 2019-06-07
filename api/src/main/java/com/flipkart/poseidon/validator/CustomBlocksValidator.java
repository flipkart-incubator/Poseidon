package com.flipkart.poseidon.validator;

import flipkart.lego.api.entities.DataSource;

import java.util.List;

/**
 * Created by shrey.garg on 2019-06-07.
 */
public interface CustomBlocksValidator {
    List<String> validateDatasource(Class<? extends DataSource<?>> dsClass);
}
