package com.flipkart.poseidon.api;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by shrey.garg on 16/02/16.
 */
public interface ExceptionMapper {
    boolean map(Throwable e, HttpServletResponse response);
}
