package com.flipkart.poseidon.serviceclients;

import java.util.List;

/**
 * Created by kiran.japannavar on 16/08/16.
 */
public class TestAbstractServiceClient extends AbstractServiceClient {
    @Override
    protected String getCommandName() {
        return null;
    }

    @Override
    public String getShortDescription() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getName() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public List<Integer> getVersion() {
        return null;
    }
}
