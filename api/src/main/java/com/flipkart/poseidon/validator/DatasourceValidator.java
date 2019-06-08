package com.flipkart.poseidon.validator;

import flipkart.lego.api.entities.DataSource;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    private <T> List<Constructor<T>> findInjectableConstructors(Class<T> klass) {
        final Constructor<T>[] declaredConstructors = (Constructor<T>[]) klass.getDeclaredConstructors();
        final List<Constructor<T>> injectableConstructors = Arrays.stream(declaredConstructors).filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if (injectableConstructors.size() > 1) {
            throw new UnsupportedOperationException(klass.getName() + " has more than one injectable constructor");
        }
        return injectableConstructors;
    }
}
