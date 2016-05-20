package com.flipkart.poseidon.api;

import org.eclipse.jetty.servlet.FilterMapping;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shrey.garg on 21/05/16.
 */
public class JettyFilterConfiguration {
    private Filter filter;
    private List<String> mappings;
    private Map<String, String> initParameters = new HashMap<>();
    private EnumSet<DispatcherType> dispatcherTypes;

    public JettyFilterConfiguration(Filter filter, List<String> mappings, EnumSet<DispatcherType> dispatcherTypes) {
        if (filter == null || mappings == null || dispatcherTypes == null) {
            throw new IllegalArgumentException("Filter configurations cannot be empty");
        }

        this.filter = filter;
        this.mappings = mappings;
        this.dispatcherTypes = dispatcherTypes;
    }

    public Filter getFilter() {
        return filter;
    }

    public List<String> getMappings() {
        return mappings;
    }

    public Map<String, String> getInitParameters() {
        return initParameters;
    }

    public void setInitParameters(Map<String, String> initParameters) {
        if (initParameters == null) {
            throw new IllegalArgumentException("Filter configurations cannot be empty");
        }

        this.initParameters = initParameters;
    }

    public EnumSet<DispatcherType> getDispatcherTypes() {
        return dispatcherTypes;
    }
}
