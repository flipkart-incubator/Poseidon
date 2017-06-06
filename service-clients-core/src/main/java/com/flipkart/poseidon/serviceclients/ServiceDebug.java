package com.flipkart.poseidon.serviceclients;

/**
 * Created by shrey.garg on 28/05/17.
 */
public class ServiceDebug {
    private final ServiceExecuteProperties properties;
    private final FutureTaskResultToDomainObjectPromiseWrapper responsePromise;

    public ServiceDebug(ServiceExecuteProperties properties, FutureTaskResultToDomainObjectPromiseWrapper responsePromise) {
        this.properties = properties;
        this.responsePromise = responsePromise;
    }

    public ServiceExecuteProperties getProperties() {
        return properties;
    }

    public FutureTaskResultToDomainObjectPromiseWrapper getResponsePromise() {
        return responsePromise;
    }
}
