package com.flipkart.poseidon.logback.access;

import ch.qos.logback.access.jetty.RequestLogImpl;
import org.eclipse.jetty.util.component.LifeCycle;

/**
 * Created by shrey.garg on 22/02/17.
 */
public class RequestLogImplFix extends RequestLogImpl implements LifeCycle {
}
