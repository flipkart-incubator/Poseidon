package com.flipkart.poseidon.serviceclients.batch;

import java.util.List;

/**
 * Created by venkata.lakshmi on 23/04/15.
 */
public interface RequestSplitter<T> {

    public List<T> split(T data);
}
