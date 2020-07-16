package com.flipkart.poseidon.serviceclients.generator;

import com.fasterxml.jackson.annotation.JsonValue;
import com.flipkart.lyrics.processor.types.TypeAdditionalHandler;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

/**
 * Created by prasad.krishna on 18/08/17.
 */
public class JsonValueAdditionalFieldHandler extends TypeAdditionalHandler {

    @Override
    public void process(TypeSpec.Builder builder, String s, Object o) {
        if (o instanceof Boolean && ((Boolean) o).booleanValue()) {
            MethodSpec.Builder toString = MethodSpec
                    .methodBuilder("toString")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addAnnotation(Override.class)
                    .addAnnotation(JsonValue.class)
                    .addStatement("return this.value");
            builder.addMethod(toString.build());
        }
    }
}
