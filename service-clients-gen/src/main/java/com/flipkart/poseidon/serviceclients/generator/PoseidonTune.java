package com.flipkart.poseidon.serviceclients.generator;

import com.flipkart.lyrics.annotators.AnnotatorStyle;
import com.flipkart.lyrics.annotators.JacksonStyle;
import com.flipkart.lyrics.config.DefaultTune;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by prasad.krishna on 05/04/17.
 */
public class PoseidonTune extends DefaultTune {

    private final List<AnnotatorStyle> annotatorStyles = Collections.singletonList(new JacksonStyle());

    @Override
    public List<AnnotatorStyle> getAnnotatorStyles() {
        return annotatorStyles;
    }
}
