package com.flipkart.poseidon.validator;

import com.flipkart.poseidon.datasources.RequestAttribute;
import flipkart.lego.api.entities.DataSource;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;

import static com.flipkart.poseidon.validator.ValidatorUtils.braced;

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
            final Parameter[] parameters = constructor.getParameters();
            final Set<String> datasourceRequestAttributes = new HashSet<>();
            for (Parameter parameter : parameters) {
                final RequestAttribute requestAttribute = parameter.getAnnotation(RequestAttribute.class);
                if (requestAttribute != null) {
                    final String attribute = Optional.of(requestAttribute.value()).filter(StringUtils::isNotEmpty).orElse(parameter.getName());
                    final boolean added = datasourceRequestAttributes.add(attribute);
                    if (!added) {
                        errors.add("RequestAttribute: " + braced(attribute) + " is used twice");
                    }
                }
            }
        }

        if (injectableConstructorCount > 1) {
            errors.add("More than 1 injectable constructor defined");
        }

        return errors;
    }
}
