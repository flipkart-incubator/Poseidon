package com.flipkart.poseidon.validator;

import flipkart.lego.api.entities.DataSource;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by shrey.garg on 2019-06-08.
 */
public class DatasourceValidator {
    public static List<String> validate(Class<? extends DataSource<?>> dsClass) {
        final ArrayList<String> errors = new ArrayList<>();
        int injectableConstructorCount = 0;
        final Constructor<?>[] declaredConstructors = dsClass.getDeclaredConstructors();
        for (Constructor<?> constructor : declaredConstructors) {
            if (constructor.getParameterCount() == 2) {
                continue;
            }

            if (!constructor.isAnnotationPresent(Inject.class)) {
                errors.add("Injectable constructor" + constructor.toGenericString() + "not annotated with @Inject");
                continue;
            }

            injectableConstructorCount++;
        }

        if (injectableConstructorCount > 1) {
            errors.add("More than 1 injectable constructor defined");
        }

        return errors;
    }
}
