package com.flipkart.poseidon.api;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by shrey.garg on 16/02/16.
 */
public interface ExceptionMapper {
    boolean map(Throwable e, HttpServletResponse response) throws IOException;
}
