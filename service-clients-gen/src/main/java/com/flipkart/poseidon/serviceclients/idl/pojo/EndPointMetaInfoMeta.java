package com.flipkart.poseidon.serviceclients.idl.pojo;

import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * Created by jagannadha.raju on 14/09/18.
 */
public class EndPointMetaInfoMeta {
    private boolean dynamicCommandName;

    public boolean isDynamicCommandName() {
        return dynamicCommandName;
    }

    public void setDynamicCommandName(boolean dynamicCommandName) {
        this.dynamicCommandName = dynamicCommandName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof EndPointMetaInfoMeta)) return false;

        EndPointMetaInfoMeta that = (EndPointMetaInfoMeta) o;

        return new EqualsBuilder()
                .append(dynamicCommandName, that.dynamicCommandName)
                .isEquals();
    }
}
