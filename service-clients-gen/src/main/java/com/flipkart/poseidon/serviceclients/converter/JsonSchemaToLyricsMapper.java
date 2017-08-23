package com.flipkart.poseidon.serviceclients.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.lyrics.model.*;
import com.flipkart.poseidon.serviceclients.generator.JsonValidator;
import com.flipkart.poseidon.serviceclients.mapper.ClassDescriptor;
import com.flipkart.poseidon.serviceclients.mapper.FieldDescriptor;
import com.flipkart.poseidon.serviceclients.mapper.ServiceClientPojo;
import com.flipkart.poseidon.serviceclients.mapper.Type;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by prasad.krishna on 25/03/17.
 */

public class JsonSchemaToLyricsMapper {

    private static final JsonSchemaToLyricsMapper INSTANCE = new JsonSchemaToLyricsMapper();

    private static ObjectMapper objectMapper = new ObjectMapper();

    private JsonSchemaToLyricsMapper() {

    }

    public static JsonSchemaToLyricsMapper getInstance() {
        return INSTANCE;
    }

    public List<ClassDescriptor> convert(String filePath, String packageName, String className) throws Exception {
        JsonValidator.getInstance().validatePojo(filePath);
        ServiceClientPojo jsonSchemaModel = objectMapper.readValue(new File(filePath), ServiceClientPojo.class);
        return generateLyricsPojo(packageName, className, jsonSchemaModel);
    }

    private List<ClassDescriptor> generateLyricsPojo(String packageName, String className, ServiceClientPojo jsonSchemaModel) {
        if (jsonSchemaModel.getEnumeration() == null) {
            return generateClass(packageName, className, jsonSchemaModel);
        }
        return generateEnum(className, jsonSchemaModel.getEnumeration(), jsonSchemaModel.getJavaEnumNames());
    }

    private List<ClassDescriptor> generateClass(String packageName, String className, ServiceClientPojo jsonSchemaModel) {
        com.flipkart.lyrics.model.Type type = com.flipkart.lyrics.model.Type.CLASS;
        List<ClassDescriptor> classDescriptorList = new ArrayList<>();
        Map<String, FieldModel> fields = new HashMap<>();
        Object additionalProperties = jsonSchemaModel.getAdditionalProperties();
        if (additionalProperties != null && !(additionalProperties instanceof Boolean)) {
            FieldDescriptor additionalPropertiesObj = objectMapper.convertValue(additionalProperties, FieldDescriptor.class);
            if (additionalPropertiesObj.getType() != null && !additionalPropertiesObj.getType().equals(Type.OBJECT)) {
                FieldType fieldType = FieldType.OBJECT;
                String objectType = Type.MAP.getPackageName();
                String firstParamType = Type.STRING.getPackageName();
                String secondParamType = additionalPropertiesObj.getType().getPackageName();
                VariableModel variableModel = new VariableModel(objectType, new VariableModel[]{new VariableModel(firstParamType), new VariableModel(secondParamType)});
                FieldModel model = getFieldModel(fieldType, variableModel, new Modifier[0]);
                fields.put("additionalProperties", model);
            } else {
                ClassDescriptor classDescriptor = getPropertyClass(className);
                classDescriptorList.add(classDescriptor);
                FieldType fieldType = FieldType.OBJECT;
                String objectType = Type.MAP.getPackageName();
                String firstParamType = Type.STRING.getPackageName();
                String secondParamType = getAcutalClassName(packageName, classDescriptor.getClassName());
                VariableModel variableModel = new VariableModel(objectType, new VariableModel[]{new VariableModel(firstParamType), new VariableModel(secondParamType)});
                FieldModel model = getFieldModel(fieldType, variableModel, new Modifier[0]);
                fields.put(classDescriptor.getClassName(), model);
            }
        }
        if (jsonSchemaModel.getProperties() != null && !jsonSchemaModel.getProperties().isEmpty()) {
            for (Map.Entry<String, FieldDescriptor> entry : jsonSchemaModel.getProperties().entrySet()) {
                if (entry.getValue().getEnumeration() != null) {
                    List<ClassDescriptor> enumModels = generateEnum(entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1), entry.getValue().getEnumeration(), entry.getValue().getJavaEnumNames());
                    classDescriptorList.addAll(enumModels);
                    FieldDescriptor fieldDescriptor = new FieldDescriptor();
                    fieldDescriptor.setJavaType(entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1));
                    fields.put(entry.getKey(), fieldDescToFieldModel(packageName, fieldDescriptor));
                } else {
                    fields.put(entry.getKey(), fieldDescToFieldModel(packageName, entry.getValue()));
                }
            }
        }
        FieldDescriptor extendedClass = jsonSchemaModel.getExtendedClass();
        VariableModel extendsModel = null;
        if (extendedClass != null) {
            if (extendedClass.getType().equals(Type.ARRAY) && extendedClass.getItems() != null) {
                extendsModel = new VariableModel(Type.ARRAY.getPackageName(), new VariableModel[]{getVariableModel(packageName, extendedClass.getItems().getJavaType())});
            } else  if (extendedClass.getType().equals(Type.OBJECT)){
                extendsModel = getVariableModel(packageName, extendedClass.getJavaType());
            }
        }

        classDescriptorList.add(new ClassDescriptor(className, getTypeModel(type, extendsModel, fields, new LinkedHashMap<>(), new ArrayList<>(), null)));
        return classDescriptorList;
    }

    private ClassDescriptor getPropertyClass(String className) {
        Map<String, FieldModel> fields = new HashMap<>();
        FieldType fieldType = FieldType.OBJECT;
        String objectType = Type.MAP.getPackageName();
        String firstParamType = Type.STRING.getPackageName();
        String secondParamType = Type.OBJECT.getPackageName();
        VariableModel variableModel = new VariableModel(objectType, new VariableModel[]{new VariableModel(firstParamType), new VariableModel(secondParamType)});
        FieldModel model = getFieldModel(fieldType, variableModel, new Modifier[0]);
        fields.put("additionalProperties", model);
        return new ClassDescriptor(className + "Property",  getTypeModel(com.flipkart.lyrics.model.Type.CLASS, null, fields, new LinkedHashMap<>(), new ArrayList<>(), null));
    }

    private String getAcutalClassName(String packageName, String className) {
        if (className.contains(".")) {
            return className;
        }
        return packageName + "." + className;
    }

    private FieldModel fieldDescToFieldModel(String packageName, FieldDescriptor fieldDescriptor) {
        FieldType fieldType = getFieldType(fieldDescriptor.getType());
        VariableModel variableModel = new VariableModel();
        boolean array = false;
        if (fieldDescriptor.getType()!= null &&
                fieldDescriptor.getType().equals(Type.ARRAY)) {
            if (fieldDescriptor.getItems() != null) {
                variableModel = new VariableModel(Type.ARRAY.getPackageName(), new VariableModel[]{getVariableModel(packageName, fieldDescriptor.getItems().getJavaType())});
            } else {
                String javaType = fieldDescriptor.getJavaType();
                if (javaType.length() > 2 && javaType.charAt(javaType.length() - 2) == '[' &&
                        javaType.charAt(javaType.length() - 1) == ']') {
                    javaType = javaType.substring(0, javaType.length() - 2);
                    array = true;
                }
                fieldType = FieldType.OBJECT;
                variableModel = new VariableModel(getAcutalClassName(packageName, javaType));
            }
        } else if (fieldDescriptor.getType() == null || fieldDescriptor.getType().equals(Type.OBJECT)) {
            variableModel = getVariableModel(packageName, fieldDescriptor.getJavaType());
        }
        if (fieldDescriptor.getFormat() != null){
            fieldType = FieldType.OBJECT;
            variableModel = getVariableModel(packageName, fieldDescriptor.getFormat().getPackageName());
        }
        boolean primitive = false;
        if (fieldDescriptor.isUsePrimitives()){
            primitive = true;
        }
        InitializerModel initializerModel = null;
        if (fieldDescriptor.getDefaultValue() != null) {
            initializerModel = new InitializerModel(fieldDescriptor.getDefaultValue());
        }
        return new FieldModel(null , fieldType, variableModel, primitive,  new Modifier[0] , false, null, !fieldDescriptor.isOptional(), false, false, array, initializerModel, false);
    }

    private FieldType getFieldType(Type type) {
        if (type == null) {
            return FieldType.OBJECT;
        }
        switch (type) {
            case NUMBER:
                return FieldType.DOUBLE;
            case INTEGER:
                return FieldType.INTEGER;
            case STRING:
                return FieldType.STRING;
            case BOOLEAN:
                return FieldType.BOOLEAN;
            default:
                return FieldType.OBJECT;
        }
    }

    private List<ClassDescriptor> generateEnum(String className, List<String> enums, List<String > javaEnumNames) {
        List<ClassDescriptor> classDescriptorList = new ArrayList<>();
        com.flipkart.lyrics.model.Type type = com.flipkart.lyrics.model.Type.ENUM_WITH_FIELDS;
        Map<String, Object[]> valuesWithFields = new LinkedHashMap<>();
        Map<String, FieldModel> fields = new LinkedHashMap<>();
        List<String> fieldOrder = new ArrayList<>();
        Map<String, Object> additionalFields = new HashMap<>();
        if (enums != null) {
            if (javaEnumNames == null) {
                javaEnumNames = enums.stream().map(String::toUpperCase).collect(Collectors.toList());
            }
            for (int i = 0; i< enums.size(); i++) {
                valuesWithFields.put(javaEnumNames.get(i), new Object[]{enums.get(i)});
            }
            fields.put("value", getFieldModel(FieldType.STRING, new VariableModel(), new Modifier[0]));
            fieldOrder.add("value");
            additionalFields.put("jsonValue", true);
        }
        classDescriptorList.add(new ClassDescriptor(className, getTypeModel(type, null, fields, valuesWithFields, fieldOrder, additionalFields)));
        return classDescriptorList;
    }

    private FieldModel getFieldModel(FieldType fieldType, VariableModel variableModel, Modifier[] modifiers) {
        return new FieldModel(null, fieldType, variableModel, false, modifiers , false, null, false, false, false, false, null, false);
    }

    private TypeModel getTypeModel(com.flipkart.lyrics.model.Type type, VariableModel extendsModel, Map<String, FieldModel> fields, Map<String, Object[]> valuesWithFields, List<String> fieldOrder, Map<String, Object> additionalProperties) {
        TypeModel typeModel = new TypeModel(type, new Modifier[0], extendsModel, new HashSet(), new ArrayList(), new ArrayList(), fields, new LinkedHashMap(), null, null, valuesWithFields, fieldOrder, null,null,null,null);
        if (additionalProperties != null) {
            additionalProperties.forEach((key, value) -> typeModel.addAdditionalProperties(key, value));
        }
        return typeModel;
    }

    private VariableModel getVariableModel(String packageName, String javaType) {
        javaType = javaType.trim();
        Stack<Object> genericStack = new Stack<>();
        int len = javaType.length();
        int iterator = 0;
        while (iterator < len) {
            if (javaType.charAt(iterator) == '<') {
                genericStack.push("<");
                iterator ++;
            } else if (javaType.charAt(iterator) == '>'){
                List<VariableModel> variableModels = new ArrayList<>();
                while (!genericStack.empty() && genericStack.peek() instanceof VariableModel) {
                    variableModels.add((VariableModel) genericStack.pop());
                }
                if (genericStack.peek() instanceof String && genericStack.peek().equals("<")) {
                    genericStack.pop();
                    String type = ((VariableModel) genericStack.pop()).getType();
                    Collections.reverse(variableModels);
                    VariableModel[] variableModelArray;
                    if (variableModels.isEmpty()) {
                        variableModelArray = new VariableModel[0];
                    } else {
                        variableModelArray = variableModels.toArray(new VariableModel[variableModels.size()]);
                    }
                    VariableModel variableModel = new VariableModel(type, variableModelArray);
                    genericStack.push(variableModel);
                }
                iterator ++;
            } else if (javaType.charAt(iterator) == ',' || javaType.charAt(iterator) == ' ') {
                iterator ++;
            } else {
                String tobeProcessedString = javaType.substring(iterator, len);
                int punctIndex = getMin(tobeProcessedString.indexOf(","),
                        tobeProcessedString.indexOf("<"), tobeProcessedString.indexOf(">"), tobeProcessedString.length());
                VariableModel variableModel = new VariableModel(getAcutalClassName(packageName, javaType.substring(iterator, iterator + punctIndex).trim()));
                genericStack.push(variableModel);
                iterator = punctIndex + iterator;
            }
        }
        return (VariableModel) genericStack.pop();
    }

    private int getMin(int... values) {
        if (values.length == 0) {
            return -1;
        }
        int min = values[0];
        for (int value : values) {
            min = (value < min && value != -1) || min == -1? value : min;
        }
        return min;
    }

}