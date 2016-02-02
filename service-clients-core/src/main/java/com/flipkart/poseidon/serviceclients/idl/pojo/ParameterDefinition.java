package com.flipkart.poseidon.serviceclients.idl.pojo;

/**
 * Created by shrey.garg on 02/02/16.
 */
public class ParameterDefinition {
    private String name;
    private String key;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterDefinition)) return false;

        ParameterDefinition that = (ParameterDefinition) o;

        if (!name.equals(that.name)) return false;
        return key != null ? key.equals(that.key) : that.key == null;

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
