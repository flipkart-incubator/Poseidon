package com.flipkart.poseidon.mappers;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by shrey.garg on 16/02/16.
 */
public interface ExceptionMapper {
    boolean map(Throwable e, HttpServletResponse response);
}
