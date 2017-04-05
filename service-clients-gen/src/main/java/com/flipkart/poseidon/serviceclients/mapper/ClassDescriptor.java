package com.flipkart.poseidon.serviceclients.mapper;

import com.flipkart.lyrics.model.TypeModel;

/**
 * Created by prasad.krishna on 30/03/17.
 */
public class ClassDescriptor {
    private String className;
    private TypeModel typeModel;

    public ClassDescriptor(String className, TypeModel typeModel) {
        this.className = className;
        this.typeModel = typeModel;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }


    public TypeModel getTypeModel() {
        return typeModel;
    }

    public void setTypeModel(TypeModel typeModel) {
        this.typeModel = typeModel;
    }
}

