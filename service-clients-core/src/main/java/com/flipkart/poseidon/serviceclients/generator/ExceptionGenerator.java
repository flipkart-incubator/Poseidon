/*
 * Copyright 2015 Flipkart Internet, pvt ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.poseidon.serviceclients.generator;

import com.flipkart.poseidon.serviceclients.ServiceClientException;
import com.sun.codemodel.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

/**
 * Created by venkata.lakshmi on 26/03/15.
 */
public class ExceptionGenerator {

    private static ExceptionGenerator INSTANCE = new ExceptionGenerator();

    private ExceptionGenerator() {}

    public static ExceptionGenerator getInstance() {
        return INSTANCE;
    }

    private void addAnnotations(JCodeModel jCodeModel, JDefinedClass jDefinedClass) {
        JAnnotationUse annotationUse = jDefinedClass.annotate(jCodeModel.ref("javax.annotation.Generated"));
        annotationUse.param("value", getClass().getName());
        annotationUse.param("date", new Date().toString());
        annotationUse.param("comments", "EDIT THIS IF YOU ARE ****");
    }

    private void addConstructor(JDefinedClass jDefinedClass) {
        JMethod constructor = jDefinedClass.constructor(JMod.PUBLIC);
        String messageVar = "message";
        constructor.param(String.class, messageVar);
        JBlock block = constructor.body();
        JInvocation superCall = block.invoke("super");
        superCall.arg(JExpr.ref(messageVar));
    }

    private void addConstructorThrowable(JDefinedClass jDefinedClass) {
        JMethod constructor = jDefinedClass.constructor(JMod.PUBLIC);
        String messageVar = "message";
        constructor.param(String.class, messageVar);
        String throwableVar = "e";
        constructor.param(Throwable.class, throwableVar);

        JBlock block = constructor.body();
        JInvocation superCall = block.invoke("super");
        superCall.arg(JExpr.ref(messageVar));
        superCall.arg(JExpr.ref(throwableVar));
    }

    public void addExceptionClass(String destinationFolder, String className) throws JClassAlreadyExistsException, IOException {
        JCodeModel model = new JCodeModel();
        JDefinedClass definedClass = model._class(className, ClassType.CLASS);
        definedClass._extends(model.ref(ServiceClientException.class));

        addAnnotations(model, definedClass);
        addConstructor(definedClass);
        addConstructorThrowable(definedClass);

        model.build(new File(destinationFolder), (PrintStream) null);
    }
}
