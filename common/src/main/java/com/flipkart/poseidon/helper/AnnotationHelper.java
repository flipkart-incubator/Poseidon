package com.flipkart.poseidon.helper;

import com.flipkart.poseidon.model.annotations.Version;

/**
 * Created by shrey.garg on 20/05/16.
 */
public class AnnotationHelper {
    public static String constructVersion(Version value) {
        return String.format("%s.%s.%s", value.major(), value.minor(), value.patch());
    }
}
