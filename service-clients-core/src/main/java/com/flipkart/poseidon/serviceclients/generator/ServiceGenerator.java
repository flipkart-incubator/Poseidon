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

import com.fasterxml.jackson.core.type.TypeReference;
import com.flipkart.poseidon.serviceclients.AbstractServiceClient;
import com.flipkart.poseidon.serviceclients.FutureTaskResultToDomainObjectPromiseWrapper;
import com.flipkart.poseidon.serviceclients.idl.pojo.EndPoint;
import com.flipkart.poseidon.serviceclients.idl.pojo.Parameter;
import com.flipkart.poseidon.serviceclients.idl.pojo.ServiceIDL;
import com.google.common.base.Joiner;
import com.sun.codemodel.*;
import flipkart.lego.api.entities.ServiceClient;
import org.apache.commons.lang.Validate;

import javax.validation.constraints.NotNull;
import java.beans.Introspector;
import java.io.File;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.flipkart.poseidon.serviceclients.CallableNameHelper.canonicalName;

/**
 * Created by mohan.pandian on 20/02/15.
 *
 * Uses sun's codemodel to generate service clients java interfaces and default implementations.
 * This is where the core business logic of service client generator lies
 */
public class ServiceGenerator {
    private static final ServiceGenerator SERVICE_GENERATOR = new ServiceGenerator();
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile("\\{parameters\\.(.*?)\\}");

    private ServiceGenerator() {}

    public static ServiceGenerator getInstance() {
        return SERVICE_GENERATOR;
    }

    private String getInterfaceName(ServiceIDL serviceIdl) {
        return serviceIdl.getService().getName() + "Client";
    }

    private String getFullInterfaceName(ServiceIDL serviceIdl) {
        return serviceIdl.getService().getPackageName() + "." + getInterfaceName(serviceIdl);
    }

    private String getImplName(ServiceIDL serviceIdl) {
        return serviceIdl.getService().getName() + "ClientImpl";
    }

    private String getFullImplName(ServiceIDL serviceIdl) {
        return serviceIdl.getService().getPackageName() + "." + getImplName(serviceIdl);
    }

    private JType getJType(JCodeModel jCodeModel, String name) {
        if (name == null || name.length() == 0) {
            name = "void";
        }
        try {
            return jCodeModel.parseType(name);
        } catch(ClassNotFoundException e) {
            return jCodeModel.directClass(name);
        }
    }

    private String getVariableName(String className) {
        return Introspector.decapitalize(className.replace("[", "").replace("]", ""));
    }

    private Map<String, String> getAllHeaders(ServiceIDL serviceIdl, EndPoint endPoint) {
        Map<String, String> headersMap = new HashMap<>();
        if (serviceIdl.getService().getHeaders() != null)
            headersMap.putAll(serviceIdl.getService().getHeaders());
        if (endPoint.getHeaders() != null)
            headersMap.putAll(endPoint.getHeaders());
        return headersMap;
    }

    private void generateMethodParam(ServiceIDL serviceIdl, JCodeModel jCodeModel, JMethod method, JDocComment methodComment, String paramName) {
        Parameter parameter = serviceIdl.getParameters().get(paramName);
        JType jType = getJType(jCodeModel, parameter.getType());
        if (!parameter.getOptional()) {
            method.param(jType.boxify(), paramName).annotate(NotNull.class);
        } else {
            method.param(jType.boxify(), paramName);
        }
        JCommentPart paramComment = methodComment.addParam(paramName);
        if (parameter.getDescription() != null) {
            for (String description : parameter.getDescription()) {
                paramComment.append(description);
            }
        }
    }

    private void addClassComments(ServiceIDL serviceIdl, JDefinedClass jDefinedClass) {
        JDocComment docComment = jDefinedClass.javadoc();
        docComment.addAll(Arrays.asList(serviceIdl.getService().getDescription()));
    }

    private void addAnnotations(JCodeModel jCodeModel, JDefinedClass jDefinedClass) {
        JAnnotationUse annotationUse = jDefinedClass.annotate(jCodeModel.ref("javax.annotation.Generated"));
        annotationUse.param("value", getClass().getName());
        annotationUse.param("date", new Date().toString());
        annotationUse.param("comments", "EDIT THIS IF YOU ARE ****");
    }

    private void addExtends(JCodeModel jCodeModel, JDefinedClass jDefinedClass) {
        jDefinedClass._extends(jCodeModel.ref(ServiceClient.class));
    }

    private void addInterfaceFields(ServiceIDL serviceIdl, JCodeModel jCodeModel, JDefinedClass jDefinedClass) {
        int fieldModifier = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;
        JFieldVar versionField = jDefinedClass.field(fieldModifier, jCodeModel.ref(List.class).narrow(Integer.class), "VERSION");
        JInvocation invocation = jCodeModel.ref(Arrays.class).staticInvoke("asList");
        invocation.arg(JExpr.lit(serviceIdl.getVersion().getMajor()));
        invocation.arg(JExpr.lit(serviceIdl.getVersion().getMinor()));
        invocation.arg(JExpr.lit(serviceIdl.getVersion().getPatch()));
        versionField.init(invocation);
    }

    private void addExtendsImplements(ServiceIDL serviceIdl, JCodeModel jCodeModel, JDefinedClass jDefinedClass) {
        jDefinedClass._extends(jCodeModel.ref(AbstractServiceClient.class));
        jDefinedClass._implements(jCodeModel.ref(getInterfaceName(serviceIdl)));
    }

    private void addConstructor(ServiceIDL serviceIdl, JDefinedClass jDefinedClass) {
        JFieldRef arg2;
        JMethod constructor = jDefinedClass.constructor(JMod.PRIVATE);
        JBlock block = constructor.body();
        if (serviceIdl.getExceptions() != null && serviceIdl.getExceptions().size() > 0 ) {
            for (Integer serviceResponseCode : serviceIdl.getExceptions().keySet()) {
                arg2 = JExpr.ref(JExpr.ref(serviceIdl.getExceptions().get(serviceResponseCode) + "Exception"), "class");
                block.invoke(JExpr.ref("exceptions"), "put").arg(serviceResponseCode.toString()).arg(arg2);
            }
        }
        // add default exception
        arg2 = JExpr.ref(JExpr.ref(serviceIdl.getService().getName() + "Exception"), "class");
        block.invoke(JExpr.ref("exceptions"), "put").arg("default").arg(arg2);
    }

    private void addMethods(ServiceIDL serviceIdl, JCodeModel jCodeModel, JDefinedClass jDefinedClass, boolean isImpl) {
        for(Map.Entry<String, EndPoint> entry: serviceIdl.getEndPoints().entrySet()) {
            String methodName = entry.getKey();
            EndPoint endPoint = entry.getValue();

            JType methodReturnType = getJType(jCodeModel, endPoint.getResponseObject());
            JType methodFullReturnType = jCodeModel.ref(FutureTaskResultToDomainObjectPromiseWrapper.class).narrow(methodReturnType);
            JMethod method = jDefinedClass.method(JMod.PUBLIC, methodFullReturnType, methodName);
            if (isImpl)
                method.annotate(jCodeModel.ref("Override"));
            JDocComment methodComment = method.javadoc();
            methodComment.addAll(Arrays.asList(endPoint.getDescription()));

            String[] parameters = endPoint.getParameters();
            if (parameters == null)
                parameters = new String[]{};
            for (String paramName : parameters) {
                generateMethodParam(serviceIdl, jCodeModel, method, methodComment, paramName);
            }
            Map<String, String> headersMap = getAllHeaders(serviceIdl, endPoint);
            for (Map.Entry<String, String> headerMapEntry: headersMap.entrySet()) {
                String value = headerMapEntry.getValue();
                Matcher matcher = PARAMETERS_PATTERN.matcher(value);
                if (matcher.find()) {
                    String paramName = matcher.group(1);
                    generateMethodParam(serviceIdl, jCodeModel, method, methodComment, paramName);
                }
            }
            if (endPoint.getRequestObject() != null) {
                String paramName = getVariableName(endPoint.getRequestObject());
                method.param(getJType(jCodeModel, endPoint.getRequestObject()), paramName);
                methodComment.addParam(paramName);
            }
            if (endPoint.getResponseObject() != null && !endPoint.getResponseObject().isEmpty()) {
                JCommentPart returnComment = methodComment.addReturn();
                returnComment.append(methodFullReturnType);
            }
            method._throws(jCodeModel.directClass("Exception"));
            if (isImpl) {
                addMethodBody(serviceIdl, jCodeModel, endPoint, method.body());
            }
        }
    }

    private void addMethodBody(ServiceIDL serviceIdl, JCodeModel jCodeModel, EndPoint endPoint, JBlock block) {
        String baseUri = serviceIdl.getService().getBaseUri();
        String endPointUri = endPoint.getUri();
        String uri = (baseUri + endPointUri).replaceAll("//", "/");
        Set<String> argsList = new LinkedHashSet<>();
        Set<String> argsListQueryParams = new LinkedHashSet<>();

        Matcher matcher = PARAMETERS_PATTERN.matcher(uri);
        while (matcher.find()) {
            uri = matcher.replaceFirst("%s");
            argsList.add(matcher.group(1));
            matcher.reset(uri);
        }
        if (argsList.isEmpty()) {
            block.decl(jCodeModel.ref("String"), "uri", JExpr.lit(uri));
        } else {
            JInvocation invocation = JExpr.ref("String").invoke("format").arg(uri);
            for (String arg : argsList) {
                Parameter parameter = serviceIdl.getParameters().get(arg);
                if (parameter.getType().equals("String")) {
                    invocation.arg(JExpr.invoke("encodeUrl").arg(JExpr.ref(arg)));
                } else if (parameter.getType().endsWith("[]")) {
                    JExpression joinerExpression = jCodeModel.ref(Joiner.class).staticInvoke("on").arg(JExpr.lit(',')).invoke("join").arg(JExpr.ref(arg));
                    invocation.arg(JExpr.invoke("encodeUrl").arg(joinerExpression));
                } else {
                    invocation.arg(JExpr.ref(arg));
                }
            }
            block.decl(jCodeModel.ref("String"), "uri", invocation);
        }
        if (endPoint.getParameters() != null) {
            for (String paramName : endPoint.getParameters()) {
                Parameter parameter = serviceIdl.getParameters().get(paramName);
                if (!parameter.getOptional()) {
                    if (parameter.getType().equalsIgnoreCase("string") || parameter.getType().endsWith("[]")) {
                        block.add(jCodeModel.ref(Validate.class).staticInvoke("notEmpty").arg(JExpr.ref(paramName)).arg(paramName + " can not be null/empty"));
                    } else {
                        block.add(jCodeModel.ref(Validate.class).staticInvoke("notNull").arg(JExpr.ref(paramName)).arg(paramName + " can not be null"));
                    }
                }

                if (argsList.contains(paramName))
                    continue;

                argsListQueryParams.add(paramName);
            }
        }

        if (!argsListQueryParams.isEmpty()) {
            JInvocation invocation = jCodeModel.ref(Arrays.class).staticInvoke("asList");
            for (String arg : argsListQueryParams) {
                Parameter parameter = serviceIdl.getParameters().get(arg);
                String paramName = Optional.ofNullable(parameter.getName()).orElse(arg);
                if (!parameter.getOptional()) {
                    if (parameter.getType().equals("String")) {
                        invocation.arg(JExpr.lit(paramName + "=").plus(JExpr.invoke("encodeUrl").arg(JExpr.ref(arg))));
                    } else if (parameter.getType().endsWith("[]")) {
                        JExpression joinerExpression = jCodeModel.ref(Joiner.class).staticInvoke("on").arg(JExpr.lit(',')).invoke("join").arg(JExpr.ref(arg));
                        invocation.arg(JExpr.lit(paramName + "=").plus(JExpr.invoke("encodeUrl").arg(joinerExpression)));
                    } else {
                        invocation.arg(JExpr.lit(paramName + "=" ).plus(JExpr.ref(arg)));
                    }
                } else {
                    invocation.arg(JExpr.invoke("getOptURI").arg(paramName).arg(JExpr.ref(arg)));
                }
            }
            block.assign(JExpr.ref("uri"), JExpr.ref("uri").plus(JExpr.invoke("getQueryURI").arg(invocation)));
        }

        Map<String, String> headersMap = getAllHeaders(serviceIdl, endPoint);
        if (headersMap.size() > 0) {
            JClass mapClass = jCodeModel.ref(Map.class).narrow(jCodeModel.ref("String"), jCodeModel.ref("String"));
            JClass hashMapClass = jCodeModel.ref(HashMap.class).narrow(jCodeModel.ref("String"), jCodeModel.ref("String"));
            block.decl(mapClass, "headersMap", JExpr._new(hashMapClass));
            for (Map.Entry<String, String> headerMapEntry : headersMap.entrySet()) {
                String key = headerMapEntry.getKey();
                String value = headerMapEntry.getValue();
                JInvocation invocation = block.invoke(JExpr.ref("headersMap"), "put").arg(key);
                matcher = PARAMETERS_PATTERN.matcher(value);
                boolean matchFound = false;
                if (matcher.find()) {
                    String paramName = matcher.group(1);
                    invocation.arg(jCodeModel.ref("String").staticInvoke("valueOf").arg(JExpr.ref(paramName)));
                    matchFound = true;
                }
                if (!matchFound)
                    invocation.arg(value);
            }
        }

        if (endPoint.getResponseObject() != null && !endPoint.getResponseObject().isEmpty()) {
            // If responseObject contains generic types, use TypeReference. Else use Class of the responseObject.
            // http://wiki.fasterxml.com/JacksonDataBinding
            // For uniformity, TypeReference or Class is then converted to a JavaType to deserialize service response.
            // For generic types, creating an anonymous inner class for every service call would have overhead which is
            // compensated by the type safety it ensures at compilation time as well as easy code generation
            JInvocation invocation = JExpr.invoke("execute");
            JInvocation nestedInvocation = JExpr.invoke("getJavaType");
            if (!endPoint.getResponseObject().contains("<")) {
                JFieldRef ref = JExpr.ref(JExpr.ref(endPoint.getResponseObject()), "class");
                nestedInvocation.arg(ref);
            } else {
                JClass typeReferenceClass = jCodeModel.ref(TypeReference.class).narrow(getJType(jCodeModel, endPoint.getResponseObject()));
                nestedInvocation.arg(JExpr._new(jCodeModel.anonymousClass(typeReferenceClass)));
            }
            invocation.arg(nestedInvocation).arg(JExpr.ref("uri")).arg(endPoint.getHttpMethod());

            if (headersMap.size() > 0) {
                invocation.arg(JExpr.ref("headersMap"));
            } else {
                invocation.arg(JExpr._null());
            }

            if (endPoint.getRequestObject() != null && !endPoint.getRequestObject().isEmpty()) {
                String requestObjectName = getVariableName(endPoint.getRequestObject());
                invocation.arg(JExpr.ref(requestObjectName));
            } else {
                invocation.arg(JExpr._null());
            }

            if (endPoint.getCommandName() != null && !endPoint.getCommandName().isEmpty()) {
                invocation = invocation.arg(endPoint.getCommandName());
            } else {
                invocation.arg(JExpr._null());
            }

            if (endPoint.isRequestCachingEnabled()) {
                invocation = invocation.arg(JExpr.lit(true));
            }
            block._return(invocation);
        }
    }

    private void addSimpleMethod(JCodeModel jCodeModel, JDefinedClass jDefinedClass,
                                 String methodName, String returnStr) {
        addSimpleMethod(jCodeModel, jDefinedClass, methodName, returnStr, null);
    }

    private void addSimpleMethod(JCodeModel jCodeModel, JDefinedClass jDefinedClass,
                                    String methodName, String returnStr, String exception) {
        JType methodReturnType = jCodeModel.ref("String");
        JMethod method = jDefinedClass.method(JMod.PUBLIC, methodReturnType, methodName);
        method.annotate(jCodeModel.ref("Override"));
        method.javadoc().addReturn().append(methodReturnType);
        if (exception != null)
            method._throws(jCodeModel.directClass(exception));
        method.body()._return(JExpr.lit(returnStr));
    }

    private void addImplMethods(ServiceIDL serviceIdl, JCodeModel jCodeModel, JDefinedClass jDefinedClass) {
        String commandName = serviceIdl.getService().getCommandName();
        if(commandName == null || commandName.isEmpty()) {
            commandName = Introspector.decapitalize(serviceIdl.getService().getName() + "HttpRequest");
        }
        addSimpleMethod(jCodeModel, jDefinedClass, "getCommandName", commandName);

        String serviceName = canonicalName(getInterfaceName(serviceIdl), "ServiceClient", "SC");
        addSimpleMethod(jCodeModel, jDefinedClass, "getName", serviceName, "UnsupportedOperationException");

        JType methodReturnType = jCodeModel.ref(List.class).narrow(Integer.class);
        JMethod method = jDefinedClass.method(JMod.PUBLIC, methodReturnType, "getVersion");
        method.annotate(jCodeModel.ref("Override"));
        method.javadoc().addReturn().append(methodReturnType);
        method._throws(jCodeModel.directClass("UnsupportedOperationException"));
        method.body()._return(JExpr.ref("VERSION"));

        String[] description = serviceIdl.getService().getDescription();
        String shortDescription = description.length > 0 ? description[0] : getInterfaceName(serviceIdl);
        addSimpleMethod(jCodeModel, jDefinedClass, "getShortDescription", shortDescription);

        String fullDescription = description.length > 0 ? String.join(" ", description) : getInterfaceName(serviceIdl);
        addSimpleMethod(jCodeModel, jDefinedClass, "getDescription", fullDescription);
    }

    public void generateInterface(ServiceIDL serviceIdl, JCodeModel jCodeModel, String destinationFolder) throws Exception {
        JDefinedClass serviceInterface = jCodeModel._class(getFullInterfaceName(serviceIdl), ClassType.INTERFACE);

        addClassComments(serviceIdl, serviceInterface);

        addAnnotations(jCodeModel, serviceInterface);

        addExtends(jCodeModel, serviceInterface);

        addInterfaceFields(serviceIdl, jCodeModel, serviceInterface);

        addMethods(serviceIdl, jCodeModel, serviceInterface, false);

        jCodeModel.build(new File(destinationFolder), (PrintStream) null);
    }

    public void generateImpl(ServiceIDL serviceIdl, JCodeModel jCodeModel, String destinationFolder) throws Exception {
        JDefinedClass serviceImpl = jCodeModel._class(getFullImplName(serviceIdl), ClassType.CLASS);

        addClassComments(serviceIdl, serviceImpl);

        addAnnotations(jCodeModel, serviceImpl);

        addExtendsImplements(serviceIdl, jCodeModel, serviceImpl);

        addConstructor(serviceIdl, serviceImpl);

        addMethods(serviceIdl, jCodeModel, serviceImpl, true);

        addImplMethods(serviceIdl, jCodeModel, serviceImpl);

        jCodeModel.build(new File(destinationFolder), (PrintStream) null);
    }
}
