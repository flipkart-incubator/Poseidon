package com.flipkart.poseidon.serviceclients.generator;

import com.flipkart.lyrics.annotators.AnnotatorStyle;
import com.flipkart.lyrics.annotators.JacksonStyle;
import com.flipkart.lyrics.config.DefaultTune;
import com.flipkart.lyrics.processor.fields.FieldAdditionalHandler;
import com.flipkart.lyrics.processor.types.TypeAdditionalHandler;

import java.util.*;

/**
 * Created by prasad.krishna on 05/04/17.
 */
public class PoseidonTune extends DefaultTune {

    private final List<AnnotatorStyle> annotatorStyles = Collections.singletonList(new JacksonStyle());

    private final Map<String, TypeAdditionalHandler> typeAdditionalHandlerMap = new HashMap<>();

    {
        typeAdditionalHandlerMap.put("jsonValue", new JsonValueAdditionalFieldHandler());
    }

    @Override
    public List<AnnotatorStyle> getAnnotatorStyles() {
        return annotatorStyles;
    }

    @Override
    public Map<String, TypeAdditionalHandler> getTypeAdditionalPropertiesHandler() {
        return typeAdditionalHandlerMap;
    }
}
