package com.flipkart.poseidon.serviceclients.batch;

import java.util.List;

/**
 * Created by venkata.lakshmi on 16/04/15.
 */
public interface ResponseMerger<T> {

    public T mergeResponse(List<T> responses);
}
