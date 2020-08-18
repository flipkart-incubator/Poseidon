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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.poseidon.handlers.http.multipart.FileFormField;
import com.flipkart.poseidon.model.annotations.Description;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import com.flipkart.poseidon.serviceclients.AbstractServiceClient;
import com.flipkart.poseidon.handlers.http.multipart.FormField;
import com.flipkart.poseidon.serviceclients.FutureTaskResultToDomainObjectPromiseWrapper;
import com.flipkart.poseidon.serviceclients.ServiceExecutePropertiesBuilder;
import com.flipkart.poseidon.serviceclients.idl.pojo.EndPoint;
import com.flipkart.poseidon.serviceclients.idl.pojo.Parameter;
import com.flipkart.poseidon.serviceclients.idl.pojo.ServiceIDL;
import com.google.common.base.Joiner;
import com.sun.codemodel.*;
import flipkart.lego.api.entities.ServiceClient;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;

import javax.validation.constraints.NotNull;
import java.beans.Introspector;
import java.io.File;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.flipkart.poseidon.helper.CallableNameHelper.canonicalName;

/**
 * Created by mohan.pandian on 20/02/15.
 *
 * Uses sun's codemodel to generate service clients java interfaces and default implementations.
 * This is where the core business logic of service client generator lies
 */
public class ServiceGenerator {
    private static final ServiceGenerator SERVICE_GENERATOR = new ServiceGenerator();
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile("\\{parameters\\.(.*?)\\}");
    private static final String REQUEST_OBJECT_VAR_NAME = "requestObject";
    private static final String REQUEST_OBJECT_LOOP_VAR_NAME = "requestObject1";
    private static final String META_INFO_PARAMETER_NAME = "_metaInfo";
    private static final String META_INFO_COMMAND_NAME_VAR_NAME = "_metaInfoCommandName";

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
        if(Parameter.Type.FILE.equals(parameter.getFormFieldType())) {
            //Additional parameter of file name also will be needed to be processed by http MultipartEntity
            JVar var = method.param(jCodeModel.ref("String"), paramName + "FileName");
            if(!parameter.getOptional()) {
                var.annotate(NotNull.class);
            }
        }
        JCommentPart paramComment = methodComment.addParam(paramName);
        if (parameter.getDescription() != null) {
            for (String description : parameter.getDescription()) {
                paramComment.append(description);
            }
        }
    }

    public void generateMetaInfo(ServiceIDL serviceIdl) throws Exception{
        for (Map.Entry<String, EndPoint> entry : serviceIdl.getEndPoints().entrySet()) {
            if (entry.getValue().isIncludeMetaInfo()) {
                addMetaInfoAsParameter(serviceIdl);
                break;
            }
        }
    }

    private void addMetaInfoAsParameter(ServiceIDL serviceIDL) throws Exception{
        for (String parameterName : serviceIDL.getParameters().keySet()) {
            if (META_INFO_PARAMETER_NAME.equals(parameterName)) {
                throw new Exception("_metaInfo as parameter name is restricted for internal usage of poseidon.");
            }
        }

        Parameter metaInfoParameter = new Parameter();
        metaInfoParameter.setType("java.util.Map<String,Object>");
        metaInfoParameter.setDescription(new String[] { "Map which has all meta info for a method overrides" });

        serviceIDL.getParameters().put(META_INFO_PARAMETER_NAME, metaInfoParameter);
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

    private void addNameAnnotations(JDefinedClass jDefinedClass, String name) {
        JAnnotationUse nameAnnotation = jDefinedClass.annotate(Name.class);
        nameAnnotation.param("value", name);
    }

    private void addDescriptionAnnotations(JDefinedClass jDefinedClass, String shortDescription, String verboseDescription) {
        JAnnotationUse descriptionAnnotation = jDefinedClass.annotate(Description.class);
        descriptionAnnotation.param("value", shortDescription);
        descriptionAnnotation.param("verbose", verboseDescription);
    }

    private void addVersionAnnotations(JDefinedClass jDefinedClass, int major, int minor, int patch) {
        JAnnotationUse versionAnnotation = jDefinedClass.annotate(Version.class);
        versionAnnotation.param("major", major);
        versionAnnotation.param("minor", minor);
        versionAnnotation.param("patch", patch);
    }

    private void addExtends(JCodeModel jCodeModel, JDefinedClass jDefinedClass) {
        jDefinedClass._extends(jCodeModel.ref(ServiceClient.class));
    }

    private void addInterfaceFields(ServiceIDL serviceIdl, JCodeModel jCodeModel, JDefinedClass jDefinedClass) {
        if (serviceIdl.getService().getObjectMapperClass() != null) {
            int fieldModifier = JMod.PUBLIC | JMod.FINAL;
            JClass objectMapper = jCodeModel.ref(serviceIdl.getService().getObjectMapperClass());
            jDefinedClass.field(fieldModifier, objectMapper, "customObjectMapper", JExpr._new(objectMapper));
        }
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

            if (endPoint.isIncludeMetaInfo()) {
                generateMethodParam(serviceIdl, jCodeModel, method, methodComment, META_INFO_PARAMETER_NAME);
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
                method.param(getJType(jCodeModel, endPoint.getRequestObject()), REQUEST_OBJECT_VAR_NAME);
                methodComment.addParam(REQUEST_OBJECT_VAR_NAME);
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

        if (endPoint.getRequestSplitterClass() != null) {
            JClass requestSplitter = jCodeModel.ref(endPoint.getRequestSplitterClass());
            block.decl(requestSplitter, "requestSplitter", JExpr._new(requestSplitter));

            JClass responseMerger = jCodeModel.ref(endPoint.getResponseMergerClass());
            block.decl(responseMerger, "responseMerger", JExpr._new(responseMerger));

            JClass wrapper = jCodeModel.ref(FutureTaskResultToDomainObjectPromiseWrapper.class);
            block.decl(wrapper, "wrapper", JExpr._new(wrapper).arg(JExpr.ref("responseMerger")));
            if (endPoint.getRequestParamWithLimit() != null) {
                Matcher matcher = PARAMETERS_PATTERN.matcher(endPoint.getRequestParamWithLimit());
                if (matcher.matches()) {
                    Parameter parameter = serviceIdl.getParameters().get(matcher.group(1));
                    JClass listClass = jCodeModel.ref(parameter.getType());
                    String listClassVar = matcher.group(1) + "List";
                    String listElement = listClassVar + "Element";
                    block.decl(jCodeModel.ref(List.class).narrow(listClass), listClassVar, JExpr.ref("requestSplitter").invoke("split").arg(JExpr.ref(matcher.group(1))));
                    JForEach forEach = new JForEach(listClass, listElement, JExpr.ref(listClassVar));
                    JInvocation invocation = createRequest(serviceIdl, jCodeModel, endPoint, forEach.body(),matcher.group(1), listElement);
                    forEach.body().add(JExpr.ref("wrapper").invoke("addFutureForTask").arg(invocation.invoke("getFutureList")));
                    block.add(forEach);
                    block._return(JExpr.ref("wrapper"));
                    return;
                }
            } else if (endPoint.getRequestSplitterClass() != null) {
                JInvocation invocation = createRequest(serviceIdl, jCodeModel, endPoint, block, null, null);
                JType returnType = jCodeModel.ref(List.class).narrow(getJType(jCodeModel, endPoint.getRequestObject()));
                block.decl(returnType, "requestObjects", JExpr.ref("requestSplitter").invoke("split").arg(JExpr.ref(REQUEST_OBJECT_VAR_NAME)));
                JForEach forEach = new JForEach(getJType(jCodeModel, endPoint.getRequestObject()), REQUEST_OBJECT_LOOP_VAR_NAME, JExpr.ref("requestObjects"));
                forEach.body().add(JExpr.ref("wrapper").invoke("addFutureForTask").arg(invocation.invoke("getFutureList")));
                block.add(forEach);
                block._return(JExpr.ref("wrapper"));
                return;
            }
        }
        JInvocation invocation = createRequest(serviceIdl, jCodeModel, endPoint, block, null, null);
        block._return(invocation);
    }

    private JInvocation createRequest(ServiceIDL serviceIdl, JCodeModel jCodeModel, EndPoint endPoint, JBlock block,String requestParamWithLimit, String listElementVarName) {
        String baseUri = serviceIdl.getService().getBaseUri();
        String endPointUri = endPoint.getUri();
        String uri = (baseUri + endPointUri).replaceAll("//", "/");
        Set<String> argsList = new LinkedHashSet<>();
        Set<String> argsListQueryParams = new LinkedHashSet<>();
        Set<String> formFields = new LinkedHashSet<>();

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
                if (endPoint.getRequestParamWithLimit() != null && requestParamWithLimit.equals(arg)) {
                    arg = listElementVarName;
                }
                if (parameter.getType().equals("String")) {
                    invocation.arg(JExpr.invoke("encodePathParam").arg(JExpr.ref(arg)));
                } else if (parameter.getType().endsWith("[]")) {
                    JExpression joinerExpression = jCodeModel.ref(Joiner.class).staticInvoke("on").arg(JExpr.lit(',')).invoke("join").arg(JExpr.ref(arg));
                    invocation.arg(JExpr.invoke("encodePathParam").arg(joinerExpression));
                } else if (parameter.getType().startsWith("java.util.List")) {
                    invocation.arg(jCodeModel.ref(StringUtils.class).staticInvoke("join").arg(JExpr.ref(arg)).arg(","));
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
                    if (parameter.getType().equalsIgnoreCase("string")) {
                        block.add(jCodeModel.ref(Validate.class).staticInvoke("notEmpty").arg(JExpr.ref(paramName)).arg(paramName + " can not be null/empty"));
                    } else if (parameter.getType().endsWith("[]")) {
                        JInvocation isNotEmpty = jCodeModel.ref(ArrayUtils.class).staticInvoke("isNotEmpty").arg(JExpr.ref(paramName));
                        block.add(jCodeModel.ref(Validate.class).staticInvoke("isTrue").arg(isNotEmpty).arg(paramName + " can not be null/empty"));
                    } else {
                        block.add(jCodeModel.ref(Validate.class).staticInvoke("notNull").arg(JExpr.ref(paramName)).arg(paramName + " can not be null"));
                    }
                }

                if (argsList.contains(paramName))
                    continue;
                if(parameter.getFormFieldType() != null) {
                    formFields.add(paramName);
                } else {
                    argsListQueryParams.add(paramName);
                }
            }
        }

        if (endPoint.isIncludeMetaInfo() && endPoint.getMetaInfo() != null) {
            if (endPoint.getMetaInfo().isDynamicCommandName()) {
                JInvocation invocation = jCodeModel.ref("String").staticInvoke("valueOf").arg(JExpr.ref(META_INFO_PARAMETER_NAME).invoke("get").arg("commandName"));
                block.decl(jCodeModel.ref("String"), META_INFO_COMMAND_NAME_VAR_NAME, invocation);
                block.add(jCodeModel.ref(Validate.class).staticInvoke("notNull").arg(JExpr.ref(META_INFO_COMMAND_NAME_VAR_NAME)).arg(META_INFO_COMMAND_NAME_VAR_NAME + " can not be null"));
            }
        }

        if (!formFields.isEmpty()) {
            JInvocation invocation = jCodeModel.ref(Arrays.class).staticInvoke("asList");
            for (String paramName : formFields) {
                Parameter parameter = serviceIdl.getParameters().get(paramName);
                JExpression contentType = jCodeModel.ref(ContentType.class).staticInvoke("parse").arg(parameter.getFormFieldContentType());
                if(parameter.getFormFieldType().equals(Parameter.Type.TEXT)) {
                    JClass formFieldClass = jCodeModel.ref(FormField.class);
                    invocation.arg(JExpr._new(formFieldClass).arg(paramName).arg(contentType).arg(JExpr.ref(paramName)));
                } else if(parameter.getFormFieldType().equals(Parameter.Type.FILE)) {
                    JClass fileFormFieldClass = jCodeModel.ref(FileFormField.class);
                    invocation.arg(JExpr._new(fileFormFieldClass).arg(paramName).arg(contentType).arg(JExpr.ref(paramName)).arg(JExpr.ref(paramName + "FileName")));
                }
            }
            JClass formFieldListClass = jCodeModel.ref(List.class).narrow(FormField.class);
            block.decl(formFieldListClass, "formFields", invocation);
            //TODO consider multivalue etc.
        }

        if (!argsListQueryParams.isEmpty()) {
            JInvocation invocation = jCodeModel.ref(Arrays.class).staticInvoke("asList");
            for (String arg : argsListQueryParams) {
                Parameter parameter = serviceIdl.getParameters().get(arg);
                String argRef = arg;
                if (endPoint.getRequestParamWithLimit() != null && requestParamWithLimit.equals(arg)) {
                    argRef = listElementVarName;
                }
                String paramName = Optional.ofNullable(parameter.getName()).orElse(arg);
                if (!parameter.getOptional()) {
                    if (parameter.isMultiValue()) {
                        invocation.arg(JExpr.invoke("getMultiValueParamURI").arg(paramName).arg(JExpr.ref(argRef)));
                    } else if (parameter.getType().equals("String")) {
                        invocation.arg(JExpr.lit(paramName + "=").plus(JExpr.invoke("encodeUrl").arg(JExpr.ref(argRef))));
                    } else if (parameter.getType().endsWith("[]")) {
                        JExpression joinerExpression = jCodeModel.ref(Joiner.class).staticInvoke("on").arg(JExpr.lit(',')).invoke("join").arg(JExpr.ref(argRef));
                        invocation.arg(JExpr.lit(paramName + "=").plus(JExpr.invoke("encodeUrl").arg(joinerExpression)));
                    } else if (parameter.getType().startsWith("java.util.List")) {
                        JExpression joinerExpression = jCodeModel.ref(StringUtils.class).staticInvoke("join").arg(JExpr.ref(argRef)).arg(",");
                        invocation.arg(JExpr.lit(paramName + "=").plus(JExpr.invoke("encodeUrl").arg(joinerExpression)));
                    } else {
                        invocation.arg(JExpr.lit(paramName + "=" ).plus(JExpr.ref(argRef)));
                    }
                } else {
                    if (parameter.isMultiValue()) {
                        invocation.arg(JExpr.invoke("getMultiValueParamURI").arg(paramName).arg(JExpr.ref(argRef)));
                    } else {
                        invocation.arg(JExpr.invoke("getOptURI").arg(paramName).arg(JExpr.ref(argRef)));
                    }
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
                JInvocation invocation = JExpr.invoke(JExpr.ref("headersMap"), "put").arg(key);
                matcher = PARAMETERS_PATTERN.matcher(value);
                if (matcher.find()) {
                    String paramName = matcher.group(1);
                    invocation.arg(jCodeModel.ref("String").staticInvoke("valueOf").arg(JExpr.ref(paramName)));
                    Parameter parameter = serviceIdl.getParameters().get(paramName);
                    if (parameter.getOptional()) {
                        block._if(JExpr.ref(paramName).ne(JExpr._null()))._then().add(invocation);
                    } else {
                        block.add(invocation);
                    }
                } else {
                    invocation.arg(value);
                    block.add(invocation);
                }
            }
        }

        if (endPoint.getResponseObject() != null && !endPoint.getResponseObject().isEmpty()) {
            // If responseObject contains generic types, use TypeReference. Else use Class of the responseObject.
            // http://wiki.fasterxml.com/JacksonDataBinding
            // For uniformity, TypeReference or Class is then converted to a JavaType to deserialize service response.
            // For generic types, creating an anonymous inner class for every service call would have overhead which is
            // compensated by the type safety it ensures at compilation time as well as easy code generation
            JInvocation invocation = JExpr.invoke("execute");
            JType definedClass = jCodeModel._ref(ServiceExecutePropertiesBuilder.class);
            JInvocation nestedInvocation = JExpr.invoke("getJavaType");
            if (!endPoint.getResponseObject().contains("<")) {
                if (endPoint.getResponseObject().contains(".")) {
                    JInvocation classDecl = jCodeModel.ref(Class.class).staticInvoke("forName").arg(endPoint.getResponseObject());
                    nestedInvocation.arg(classDecl);
                } else {
                    JFieldRef ref = JExpr.ref(JExpr.ref(endPoint.getResponseObject()), "class");
                    nestedInvocation.arg(ref);
                }
            } else {
                JClass typeReferenceClass = jCodeModel.ref(TypeReference.class).narrow(getJType(jCodeModel, endPoint.getResponseObject()));
                nestedInvocation.arg(JExpr._new(jCodeModel.anonymousClass(typeReferenceClass)));
            }
            JInvocation builderInvocation = JExpr.invoke(JExpr._new(definedClass), "setJavaType").arg(nestedInvocation);
            if (endPoint.getErrorResponseObject() != null && !endPoint.getErrorResponseObject().isEmpty()) {
                JInvocation nestedErrorInvocation = JExpr.invoke("getErrorType");
                if (!endPoint.getErrorResponseObject().contains("<")) {
                    if (endPoint.getErrorResponseObject().contains(".")) {
                        JInvocation classDecl = jCodeModel.ref(Class.class).staticInvoke("forName").arg(endPoint.getErrorResponseObject());
                        nestedErrorInvocation.arg(classDecl);
                    } else {
                        JFieldRef ref = JExpr.ref(JExpr.ref(endPoint.getErrorResponseObject()), "class");
                        nestedErrorInvocation.arg(ref);
                    }
                } else {
                    JClass typeReferenceClass = jCodeModel.ref(TypeReference.class).narrow(getJType(jCodeModel, endPoint.getErrorResponseObject()));
                    nestedErrorInvocation.arg(JExpr._new(jCodeModel.anonymousClass(typeReferenceClass)));
                }
                builderInvocation = builderInvocation.invoke("setErrorType").arg(nestedErrorInvocation);
            }
            builderInvocation = builderInvocation.invoke("setUri").arg(JExpr.ref("uri"));
            builderInvocation = builderInvocation.invoke("setHttpMethod").arg(endPoint.getHttpMethod());

            if (headersMap.size() > 0) {
                builderInvocation = builderInvocation.invoke("setHeadersMap").arg(JExpr.ref("headersMap"));
            }

            if (endPoint.getRequestObject() != null && !endPoint.getRequestObject().isEmpty()) {
                String requestObjectName;
                if (endPoint.getRequestSplitterClass() != null && endPoint.getRequestParamWithLimit() == null) {
                    requestObjectName = REQUEST_OBJECT_LOOP_VAR_NAME;
                } else {
                    requestObjectName = REQUEST_OBJECT_VAR_NAME;
                }
                builderInvocation = builderInvocation.invoke("setRequestObject").arg(JExpr.ref(requestObjectName));
            }

            if (endPoint.isIncludeMetaInfo() && endPoint.getMetaInfo() != null && endPoint.getMetaInfo().isDynamicCommandName()) {
                builderInvocation = builderInvocation.invoke("setCommandName").arg(JExpr.ref(META_INFO_COMMAND_NAME_VAR_NAME));
            } else if (endPoint.getCommandName() != null && !endPoint.getCommandName().isEmpty()) {
                builderInvocation = builderInvocation.invoke("setCommandName").arg(endPoint.getCommandName());
            }

            if (endPoint.isRequestCachingEnabled()) {
                builderInvocation = builderInvocation.invoke("setRequestCachingEnabled").arg(JExpr.lit(true));
            }

            if(formFields.size() > 0) {
                builderInvocation = builderInvocation.invoke("setFormFields").arg(JExpr.ref("formFields"));
            }

            builderInvocation = builderInvocation.invoke("build");
            return invocation.arg(builderInvocation);
        }
        return null;
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

        if (serviceIdl.getService().getObjectMapperClass() != null) {
            JType methodReturnType = jCodeModel.ref(ObjectMapper.class);
            JMethod method = jDefinedClass.method(JMod.PROTECTED, methodReturnType, "getObjectMapper");
            method.annotate(jCodeModel.ref("Override"));
            method.javadoc().addReturn().append(methodReturnType);
            method.body()._return(JExpr.ref("customObjectMapper").invoke("getObjectMapper"));
        }
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

        addMetaAnnotations(serviceIdl, jCodeModel, serviceImpl);

        jCodeModel.build(new File(destinationFolder), (PrintStream) null);
    }

    private void addMetaAnnotations(ServiceIDL serviceIdl, JCodeModel jCodeModel, JDefinedClass serviceImpl) {
        String serviceName = canonicalName(getInterfaceName(serviceIdl), "ServiceClient", "SC");
        addNameAnnotations(serviceImpl, serviceName);

        addVersionAnnotations(serviceImpl, serviceIdl.getVersion().getMajor(), serviceIdl.getVersion().getMinor(), serviceIdl.getVersion().getPatch());

        String[] description = serviceIdl.getService().getDescription();
        String shortDescription = description.length > 0 ? description[0] : getInterfaceName(serviceIdl);
        String fullDescription = description.length > 0 ? String.join(" ", description) : getInterfaceName(serviceIdl);

        addDescriptionAnnotations(serviceImpl, shortDescription, fullDescription);
    }
}
