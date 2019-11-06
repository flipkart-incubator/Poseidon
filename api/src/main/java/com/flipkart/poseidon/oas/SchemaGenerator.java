/*
 * Copyright 2019 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon.oas;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.flipkart.poseidon.api.APIManager;
import com.flipkart.poseidon.helper.ClassPathHelper;
import com.flipkart.poseidon.legoset.PoseidonLegoSet;
import com.flipkart.poseidon.model.oas.Response;
import com.flipkart.poseidon.model.oas.Responses;
import com.flipkart.poseidon.pojos.EndpointPOJO;
import com.flipkart.poseidon.pojos.ParamPOJO;
import com.flipkart.poseidon.pojos.ParamsPOJO;
import com.flipkart.poseidon.pojos.TaskPOJO;
import com.flipkart.poseidon.utils.ApiHelper;
import com.flipkart.poseidon.validator.BlocksValidator;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.TypeToken;
import flipkart.lego.api.entities.DataSource;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.flipkart.poseidon.helpers.ObjectMapperHelper.getMapper;

/**
 * Created by shrey.garg on 2019-01-01.
 */
public class SchemaGenerator {
    private static final Map<String, JavaType> modelTypes = new HashMap<>();
    private static final Set<String> globalModelSet = new HashSet<>();
    private static final Map<String, Class<? extends DataSource<?>>> datasources = new HashMap<>();
    private static final Set<String> noBuilderClasses = new HashSet<>();

    private static final List<Class<? extends Annotation>> nonNullAnnotations = Arrays.asList(
            NotNull.class,
            Nonnull.class
    );

    private static final Map<String, OpenAPI> versionedOpenAPIs = new HashMap<>();
    private static String serviceName = null;

    private static OpenAPI resolveVersionedAPI(String version) {
        final String resolvedVersion = Optional.ofNullable(version).orElse("1.0");
        return versionedOpenAPIs.computeIfAbsent(resolvedVersion, v -> {
            OpenAPI openAPI = new OpenAPI();
            Info info = new Info();
            info.title(Optional.ofNullable(serviceName).orElse("Service") + "Resource");
            info.version(v);
            openAPI.info(info);
            return openAPI;
        });
    }

    public static void main(String[] args) throws IOException, NoSuchMethodException {
        if (args.length < 3) {
            throw new UnsupportedOperationException("Correct Usage: <api-dir> <schema-target> <datasource-packages-csv>");
        }

        final List<String> validConfigs = new ArrayList<>();
        List<EndpointPOJO> pojos = new ArrayList<>();

        final Path dir = Paths.get(args[0]);
        final Path genDir = Paths.get(args[1]);
        final Path modelsDir = genDir.resolve("models");

        if (Files.exists(modelsDir)) {
            Files.walk(modelsDir)
                    .sorted(Comparator.reverseOrder())
                    .peek(System.out::println)
                    .forEach(deleteIfExists);
        }

        if (args.length >= 4) {
            noBuilderClasses.addAll(Arrays.asList(args[3].split(",")));
        }

        if (args.length == 5) {
            serviceName = args[4];
        }

        try {
            APIManager.scanAndAdd(dir, validConfigs);
            for (String config : validConfigs) {
                pojos.add(getMapper().readValue(config, EndpointPOJO.class));
            }
            pojos = pojos.stream().sorted(Comparator.comparing(EndpointPOJO::getName)).collect(Collectors.toList());
        } catch (Exception e) {
            throw new UnsupportedOperationException("Error while reading EndpointPOJO", e);
        }

        final String[] datasourcePackages = args[2].split(",");
        BlocksValidator.main(datasourcePackages);

        fetchDataSources(datasourcePackages);

        final Map<String, Map<String, Map<PathItem.HttpMethod, Operation>>> paths = new LinkedHashMap<>();
        for (EndpointPOJO endpointPOJO : pojos) {
            final Operation operation = new Operation();
            final ParamsPOJO paramsPOJO = endpointPOJO.getParams();
            if (paramsPOJO != null) {
                if (paramsPOJO.getRequired() != null) {
                    for (ParamPOJO paramPOJO : paramsPOJO.getRequired()) {
                        handleParams(operation, paramPOJO, modelsDir, true);
                    }
                }

                if (paramsPOJO.getOptional() != null) {
                    for (ParamPOJO paramPOJO : paramsPOJO.getOptional()) {
                        handleParams(operation, paramPOJO, modelsDir, false);
                    }
                }
            }

            ApiResponses responses = new ApiResponses();
            if (endpointPOJO.getResponse() instanceof String) {
                String responseString = (String) endpointPOJO.getResponse();
                responseString = responseString.substring(3, responseString.length() - 2);
                if (responseString.startsWith("$")) {
                    responseString = responseString.substring(1);
                }

                final TaskPOJO taskPOJO = endpointPOJO.getTasks().get(responseString);
                final Class<? extends DataSource<?>> dsClass = datasources.get(taskPOJO.getName());
                final Type resolvedType = TypeToken.of(dsClass)
                        .resolveType(dsClass.getMethod("call").getGenericReturnType())
                        .getType();
                resolveAPIResponse("200", resolvedType, responses, modelsDir);
                handleOtherResponses(dsClass, responses, modelsDir);
            }

            operation.responses(responses);
            operation.operationId(endpointPOJO.getName());
            paths.computeIfAbsent(endpointPOJO.getVersion(), v -> new LinkedHashMap<>())
                    .computeIfAbsent(endpointPOJO.getUrl(), s -> new LinkedHashMap<>()).put(getHttpMethod(endpointPOJO.getHttpMethod()), operation);
        }

        paths.forEach((version, pathsMap) -> {
            final OpenAPI openAPI = resolveVersionedAPI(version);
            pathsMap.forEach((k, v) -> {
                final PathItem pathItem = new PathItem();
                pathItem.addExtension("x-old-path", k);
                v.forEach(pathItem::operation);
                openAPI.path(k, pathItem);
            });
        });

        for (OpenAPI openAPI : versionedOpenAPIs.values()) {
            final byte[] apiBytes = Json.mapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(openAPI);

            final Path apiDir = genDir.resolve("apis/v" + openAPI.getInfo().getVersion().charAt(0));
            final Path apiFile = apiDir.resolve(Optional.ofNullable(serviceName).orElse("Service") + "Resource.json");
            Files.createDirectories(apiFile.getParent());
            Files.write(apiFile, apiBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        modelTypes.forEach((k, v) -> {
            try {
                final Path modelFile = modelsDir.resolve(k + ".json");
                Files.createDirectories(modelFile.getParent());
                Files.write(modelFile, Json.mapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(generateSchema(v, modelsDir)), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Error while create model files", e);
            }
        });
    }

    private static void handleParams(Operation operation, ParamPOJO paramPOJO, Path modelsDir, boolean isRequired) {
        if (paramPOJO.isFile()) {
            return;
        }

        if (paramPOJO.isBody()) {
            final RequestBody requestBody = new RequestBody();
            requestBody.required(isRequired);
            final Content content = new Content();
            final MediaType mediaType = new MediaType();
            mediaType.schema(createSchema(paramPOJO, modelsDir));
            content.addMediaType("application/json", mediaType);
            requestBody.content(content);
            operation.requestBody(requestBody);
            return;
        }

        operation.addParametersItem(createParameter(paramPOJO, modelsDir).required(isRequired));
    }

    private static void resolveAPIResponse(String status, Type resolvedType, ApiResponses responses, Path modelsDir) {
        final Schema<?> responseSchema = processType(resolvedType, modelsDir);
        final ApiResponse response = new ApiResponse();
        final Content content = new Content();
        final MediaType mediaType = new MediaType();
        mediaType.schema(responseSchema);
        content.addMediaType("application/json", mediaType);
        response.content(content);
        response.description("Some random thing");
        responses.addApiResponse(status, response);
    }

    private static void handleOtherResponses(Class<? extends DataSource<?>> dsClass, ApiResponses responses, Path modelsDir) {
        final Responses responsesAnnotation = dsClass.getAnnotation(Responses.class);
        if (responsesAnnotation == null) {
            return;
        }

        final Response[] responseArray = responsesAnnotation.value();
        for (Response response : responseArray) {
            resolveAPIResponse(String.valueOf(response.status()), response.responseClass(), responses, modelsDir);
        }
    }

    private static Schema<?> generateSchema(JavaType javaType, Path modelsDir) {
        if (javaType.isCollectionLikeType()) {
            if (javaType.getContentType() instanceof SimpleType) {
                final Class<?> rawClass = javaType.getContentType().getRawClass();
                return new ArraySchema().items(createReference(rawClass, null, new LinkedHashMap<>()));
            } else {
                return generateSchema(javaType, modelsDir);
            }
        } else if (javaType.isMapLikeType()) {
            // TODO: 2019-07-20 add here when Map types have to be supported as request bodies
            return null;
        } else {
            final Class<?> rawClass = javaType.getRawClass();
            return processClass(rawClass, modelsDir);
        }
    }

    private static Schema<?> processClass(Class<?> clazz, Path modelsDir) {
        final Map<String, Class<?>> referencedClasses = new HashMap<>();
        final int globalSetSize = globalModelSet.size();

        if (clazz.isEnum()) {
            final Enum<?>[] enumConstants = ((Class<? extends Enum<?>>) clazz).getEnumConstants();
            StringSchema schema = new StringSchema();
            Arrays.stream(enumConstants).forEach(e -> schema.addEnumItem(e.name()));
            return schema;
        }

        ComposedSchema schema = new ComposedSchema();
        schema.type("object");

        if (!noBuilderClasses.contains(clazz.getName())) {
            schema.addExtension("x-create-builder", true);
        }

        final Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            schema.allOf(Collections.singletonList(createReference(superclass, clazz, referencedClasses)));
        }

        handleJsonSubTypes(clazz, schema, referencedClasses);

        final Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            final String alias = fetchAlias(field);
            final Schema<?> fieldSchema = processField(clazz, field.getType(), field.getGenericType(), referencedClasses);
            if (alias == null) {
                schema.addProperties(field.getName(), fieldSchema);
            } else {
                fieldSchema.addExtension("x-internal-name", field.getName());
                schema.addProperties(alias, fieldSchema);
            }

            if (isRequiredProperty(field)) {
                schema.addRequiredItem(field.getName());
            }
        }

        if (globalSetSize < globalModelSet.size()) {
            referencedClasses.forEach((k, v) -> {
                try {
                    final Path modelFile = modelsDir.resolve(k + ".json");
                    Files.createDirectories(modelFile.getParent());
                    final Schema<?> referencedSchema = processClass(v, modelsDir);
                    Files.write(modelFile, Json.mapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(referencedSchema), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Error while create model files", e);
                }
            });
        }

        return schema;
    }

    private static void handleJsonSubTypes(Class<?> clazz, ComposedSchema schema, Map<String, Class<?>> referencedClasses) {
        final JsonTypeInfo typeInfo = clazz.getAnnotation(JsonTypeInfo.class);
        final JsonSubTypes subTypes = clazz.getAnnotation(JsonSubTypes.class);

        if (typeInfo != null && subTypes != null) {
            final Discriminator discriminator = new Discriminator().propertyName(typeInfo.property().equals("") ? typeInfo.use().getDefaultPropertyName() : typeInfo.property());

            for (JsonSubTypes.Type type : subTypes.value()) {
                final Schema<?> reference = createReference(type.value(), clazz, referencedClasses);
                schema.addOneOfItem(reference);
                if (StringUtils.isNotEmpty(type.name()) || typeInfo.use() == JsonTypeInfo.Id.CLASS) {
                    // TODO: 2019-06-24 fix this once mappings are correctly handled elsewhere
//                    discriminator.mapping(type.name(), reference.get$ref());
                    discriminator.mapping(typeInfo.use() == JsonTypeInfo.Id.CLASS ? type.value().getName() : type.name(), "#/components/schemas/" + type.value().getSimpleName());
                }
            }

            schema.discriminator(discriminator);
        }
    }

    private static boolean isRequiredProperty(Field field) {
        for (Class<? extends Annotation> nonNullAnnotation : nonNullAnnotations) {
            if (field.isAnnotationPresent(nonNullAnnotation)) {
                return true;
            }
        }

        return false;
    }

    private static String fetchAlias(Field field) {
        final String jsonProperty;
        if (field.isAnnotationPresent(JsonProperty.class) && StringUtils.isNotBlank((jsonProperty = field.getAnnotation(JsonProperty.class).value()))) {
            return jsonProperty;
        }

        return null;
    }

    private static Schema<?> processType(Type type, Path modelsDir) {
        final Map<String, Class<?>> referencedClasses = new HashMap<>();
        final int globalSetSize = globalModelSet.size();

        final Schema<?> schema;
        if (!(type instanceof ParameterizedType)) {
            if (Map.class.isAssignableFrom((Class<?>) type) || List.class.isAssignableFrom((Class<?>) type)) {
                schema = processType(type, null, referencedClasses);
            } else {
                schema = createReference((Class<?>) type, null, referencedClasses);
            }
        } else {
            schema = processType(type, null, referencedClasses);
        }

        if (globalSetSize < globalModelSet.size()) {
            referencedClasses.forEach((k, v) -> {
                try {
                    final Path modelFile = modelsDir.resolve(k + ".json");
                    Files.createDirectories(modelFile.getParent());
                    final Schema<?> referencedSchema = processClass(v, modelsDir);
                    Files.write(modelFile, Json.mapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(referencedSchema), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Error while create model files", e);
                }
            });
        }

        return schema;
    }

    private static Schema<?> processField(Class<?> baseClass, Class<?> clazz, Type type, Map<String, Class<?>> referencedClasses) {
        if (clazz == Integer.class) {
            return new IntegerSchema();
        } else if (clazz == Integer.TYPE) {
            return new IntegerSchema().extensions(Collections.singletonMap("x-primitive", true));
        } else if (clazz == Long.class || clazz == Long.TYPE) {
            return new IntegerSchema().format("int64");
        } else if (clazz == Float.class || clazz == Float.TYPE) {
            return new NumberSchema();
        } else if (clazz == Double.class || clazz == Double.TYPE) {
            return new NumberSchema().format("num64");
        } else if (clazz == Boolean.class || clazz == Boolean.TYPE) {
            return new BooleanSchema();
        } else if (clazz == Byte.class || clazz == Byte.TYPE) {
            return new StringSchema().format("byte");
        } else if (clazz == String.class) {
            return new StringSchema();
        } else if (Map.class.isAssignableFrom(clazz) || List.class.isAssignableFrom(clazz)) {
            return processType(type, baseClass, referencedClasses);
        } else if (clazz == Object.class) {
            return new ObjectSchema().extensions(Collections.singletonMap("x-no-contract", true));
        } else if (clazz.isEnum()) {
            return createReference(clazz, baseClass, referencedClasses);
        } else if (clazz.isArray()) {
            return new ArraySchema().items(processField(baseClass, clazz.getComponentType(), clazz.getComponentType(), referencedClasses));
        } else {
            return createReference(clazz, baseClass, referencedClasses);
        }
    }

    private static Schema<?> createReference(Class<?> clazz, Class<?> baseClass, Map<String, Class<?>> referencedClasses) {
        referencedClasses.put(clazz.getName().replace('.', '/'), clazz);
        globalModelSet.add(clazz.getName());
        return new Schema<>().$ref(baseClass != null ? resolveModelContextPath(clazz.getName(), baseClass) : resolvePath(clazz.getName()));
    }

    private static Schema<?> processType(Type type, Class<?> baseClass, Map<String, Class<?>> referencedClasses) {
        if (type instanceof ParameterizedType) {
            final Class<?> rawType = (Class<?>) ((ParameterizedType) type).getRawType();
            if (Map.class.isAssignableFrom(rawType)) {
                return new ObjectSchema().additionalProperties(processType(((ParameterizedType) type).getActualTypeArguments()[1], baseClass, referencedClasses));
            } else if (List.class.isAssignableFrom(rawType)) {
                return new ArraySchema().items(processType(((ParameterizedType) type).getActualTypeArguments()[0], baseClass, referencedClasses));
            } else {
                return processField(baseClass, rawType, type, referencedClasses);
            }
        } else {
            final Class<?> clazz = (Class<?>) type;
            if (Map.class.isAssignableFrom(clazz)) {
                return new ObjectSchema().additionalProperties(processType(Object.class, baseClass, referencedClasses));
            } else if (List.class.isAssignableFrom(clazz)) {
                return new ArraySchema().items(processType(Object.class, baseClass, referencedClasses));
            } else {
                return processField(baseClass, clazz, type, referencedClasses);
            }
        }
    }

    private static Parameter createParameter(ParamPOJO paramPOJO, Path modelsDir) {
        final Parameter parameter = new Parameter();
        if (paramPOJO.getInternalName() != null) {
            parameter.addExtension("x-internal-name", paramPOJO.getInternalName());
        }

        parameter.name(paramPOJO.getName())
                .in(getParamType(paramPOJO))
                .schema(createSchema(paramPOJO, modelsDir));

        return parameter;
    }

    private static String getParamType(ParamPOJO paramPOJO) {
        if (paramPOJO.isPathparam()) {
            return "path";
        } else if (paramPOJO.isHeader()) {
            return "header";
        } else {
            return "query";
        }
    }

    private static Schema<?> createSchema(ParamPOJO paramPOJO, Path modelsDir) {
        final Schema<?> schema;
        if (paramPOJO.getDatatype() == null) {
            JavaType javaType = ApiHelper.constructJavaType(paramPOJO);
            if (javaType.isContainerType()) {
                return generateSchema(javaType, modelsDir);
            } else {
                return createSchemaPOJO(paramPOJO);
            }
        }

        switch (paramPOJO.getDatatype()) {
            case STRING:
                schema = new StringSchema();
                break;
            case BOOLEAN:
                schema = new BooleanSchema();
                break;
            case INTEGER:
                schema = new IntegerSchema();
                break;
            case LONG:
                schema = new IntegerSchema();
                schema.format("int64");
                break;
            case NUMBER:
                schema = new NumberSchema();
                break;
            case ENUM:
                schema = createSchemaPOJO(paramPOJO);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return schema;
    }

    private static Schema<?> createSchemaPOJO(ParamPOJO paramPOJO) {
        final JavaType javaType = ApiHelper.constructJavaType(paramPOJO);
        final String ref = resolvePath(javaType.getRawClass().getName());
        modelTypes.put(javaType.getRawClass().getName().replace('.', '/'), javaType);
        globalModelSet.add(javaType.getRawClass().getName());

        Schema<?> schema = new Schema<>();
        schema.$ref(ref);
        return schema;
    }

    private static String resolvePath(String path) {
        String resolvedPath = path.replace('.', '/');
        resolvedPath = "../../models/" + resolvedPath;
        return resolvedPath + ".json";
    }

    private static String resolveModelContextPath(String path, Class<?> baseClass) {
        String baseClassPath = baseClass.getName();
        final int minLength = baseClassPath.length() < path.length() ? baseClassPath.length() : path.length();
        int cutOff = 0;
        for (int i = 0; i < minLength; i++) {
            if (baseClassPath.charAt(i) != path.charAt(i)) {
                cutOff = i;
            }
        }
        String processedPath = path.substring(cutOff);
        final int parts = baseClass.getName().split("\\.").length - 1;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts; i++) {
            builder.append("../");
        }
        builder.append(path.replace('.', '/'));
        builder.append(".json");
        return builder.toString();
    }

    private static PathItem.HttpMethod getHttpMethod(HttpMethod httpMethod) {
        switch (httpMethod) {
            case GET:
                return PathItem.HttpMethod.GET;
            case POST:
                return PathItem.HttpMethod.POST;
            case PUT:
                return PathItem.HttpMethod.PUT;
            case DELETE:
                return PathItem.HttpMethod.DELETE;
            case PATCH:
                return PathItem.HttpMethod.PATCH;
            case HEAD:
                return PathItem.HttpMethod.HEAD;
            case OPTIONS:
                return PathItem.HttpMethod.OPTIONS;
            case TRACE:
                return PathItem.HttpMethod.TRACE;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static void fetchDataSources(String[] args) {
        try {
            final Set<ClassPath.ClassInfo> classInfos = ClassPathHelper.getPackageClasses(Thread.currentThread().getContextClassLoader(), Arrays.asList(args));
            System.out.println("Classes in ClassLoader: " + classInfos.size());
            for (ClassPath.ClassInfo classInfo : classInfos) {
                Class clazz = Class.forName(classInfo.getName());
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }

                if (DataSource.class.isAssignableFrom(clazz)) {
                    datasources.put(PoseidonLegoSet.getBlockId(clazz).get(), clazz);
                }
            }
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }

    }

    private static final Consumer<Path> deleteIfExists = p -> {
        try {
            Files.deleteIfExists(p);
        } catch (Exception e) {
            System.out.println("Error while deleting model dir: " + e.getMessage());
        }
    };
}
