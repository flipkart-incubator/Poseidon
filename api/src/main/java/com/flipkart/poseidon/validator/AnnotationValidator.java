package com.flipkart.poseidon.validator;

import com.flipkart.poseidon.model.annotations.Description;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import flipkart.lego.api.entities.DataSource;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by shrey.garg on 15/07/16.
 */
public class AnnotationValidator {
    private static final List<Class<? extends Annotation>> annotationsToValidate = Arrays.asList(
            Name.class,
            Version.class,
            Description.class
    );

    public static List<String> validateDataSource(Class<DataSource> dataSource) {
        List<String> errors = new ArrayList<>();

        Predicate<Class<? extends Annotation>> isAnnotationNotPresent = a -> !dataSource.isAnnotationPresent(a);
        Consumer<Class<? extends Annotation>> addToErrors = a -> errors.add(a.getSimpleName() + " annotation is missing.");

        annotationsToValidate.stream().filter(isAnnotationNotPresent).forEach(addToErrors);

        return errors;
    }
}
