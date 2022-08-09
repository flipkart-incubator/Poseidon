package com.flipkart.poseidon.serviceclients;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by shrey.garg on 05/06/17.
 */
public class ServiceContextState {
    Map<String, Object> ctxt;
    boolean debug;
    Map<String, List<ServiceDebug>> serviceResponses;
    Map<String, Queue<String>> collectedHeaders;
    Map<String, FanoutContext> fanoutContext;
}
