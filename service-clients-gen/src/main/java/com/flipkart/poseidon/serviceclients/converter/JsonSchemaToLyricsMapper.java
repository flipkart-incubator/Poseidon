package com.flipkart.poseidon.serviceclients.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.lyrics.model.*;
import com.flipkart.poseidon.serviceclients.generator.JsonValidator;
import com.flipkart.poseidon.serviceclients.mapper.ClassDesc;
import com.flipkart.poseidon.serviceclients.mapper.FieldDesc;
import com.flipkart.poseidon.serviceclients.mapper.ServiceClientPojoMapper;
import com.flipkart.poseidon.serviceclients.mapper.Type;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.util.*;

/**
 * Created by prasad.krishna on 25/03/17.
 */

public class JsonSchemaToLyricsMapper {

    private static final JsonSchemaToLyricsMapper INSTANCE = new JsonSchemaToLyricsMapper();

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static ServiceClientPojoMapper jsonSchemaModel;
    private static String packageName;
    private static String className;

    private JsonSchemaToLyricsMapper() {

    }

    public static JsonSchemaToLyricsMapper getInstance() {
        return INSTANCE;
    }

    public List<ClassDesc> convert(String filePath, String packageName, String className) throws Exception {
        this.packageName = packageName;
        this.className = className;
        JsonValidator.getInstance().validatePojo(filePath);
        ServiceClientPojoMapper jsonSchemaModel = objectMapper.readValue(new File(filePath), ServiceClientPojoMapper.class);
        this.jsonSchemaModel = jsonSchemaModel;
        return generateLyricsPojo();
    }

    private List<ClassDesc> generateLyricsPojo() {
        switch (jsonSchemaModel.getType()) {
            case OBJECT:
                return generateClass();
            case STRING:
                return generateEnum(className, jsonSchemaModel.getEnumeration());
            default:
                return null;
        }
    }

    private List<ClassDesc> generateClass() {
        com.flipkart.lyrics.model.Type type = com.flipkart.lyrics.model.Type.CLASS;
        List<ClassDesc> classDescList = new ArrayList<>();
        Map<String, FieldModel> fields = new HashMap<>();
        Object additionalProperties = jsonSchemaModel.getAdditionalProperties();
        if (additionalProperties != null && !(additionalProperties instanceof Boolean)) {
            FieldDesc additionalPropertiesObj = objectMapper.convertValue(additionalProperties, FieldDesc.class);
            if (additionalPropertiesObj.getType() != null && !additionalPropertiesObj.getType().equals(Type.OBJECT)) {
                FieldType fieldType = FieldType.OBJECT;
                String objectType = Type.MAP.getPackageName();
                String firstParamType = Type.STRING.getPackageName();
                String secondParamType = additionalPropertiesObj.getType().getPackageName();
                VariableModel variableModel = new VariableModel(objectType, new VariableModel[]{new VariableModel(firstParamType), new VariableModel(secondParamType)});
                FieldModel model = getFieldModel(fieldType, variableModel, new Modifier[0]);
                fields.put("additionalProperties", model);
            } else {
                ClassDesc classDesc = getPropertyClass();
                classDescList.add(classDesc);
                FieldType fieldType = FieldType.OBJECT;
                String objectType = Type.MAP.getPackageName();
                String firstParamType = Type.STRING.getPackageName();
                String secondParamType = getAcutalClassName(classDesc.getClassName());
                VariableModel variableModel = new VariableModel(objectType, new VariableModel[]{new VariableModel(firstParamType), new VariableModel(secondParamType)});
                FieldModel model = getFieldModel(fieldType, variableModel, new Modifier[0]);
                fields.put(classDesc.getClassName(), model);
            }
        }
        if (jsonSchemaModel.getProperties() != null && !jsonSchemaModel.getProperties().isEmpty()) {
            for (Map.Entry<String, FieldDesc> entry : jsonSchemaModel.getProperties().entrySet()) {
                if (entry.getValue().getEnumeration() != null) {
                    List<ClassDesc> enumModels = generateEnum(entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1), entry.getValue().getEnumeration());
                    classDescList.addAll(enumModels);
                    FieldDesc fieldDesc = new FieldDesc();
                    fieldDesc.setJavaType(entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1));
                    fields.put(entry.getKey(), fieldDescToFieldModel(fieldDesc));
                } else {
                    fields.put(entry.getKey(), fieldDescToFieldModel(entry.getValue()));
                }
            }
        }
        FieldDesc extendedClass = jsonSchemaModel.getExtendedClass();
        VariableModel extendsModel = null;
        if (extendedClass != null) {
            if (extendedClass.getType().equals(Type.ARRAY) && extendedClass.getItems() != null) {
                extendsModel = new VariableModel(Type.ARRAY.getPackageName(), new VariableModel[]{getVariableModel(extendedClass.getItems().getJavaType())});
            } else  if (extendedClass.getType().equals(Type.OBJECT)){
                extendsModel = getVariableModel(extendedClass.getJavaType());
            }
        }

        classDescList.add(new ClassDesc(className, getTypeModel(type, extendsModel, fields, null)));
        return classDescList;
    }

    private ClassDesc getPropertyClass() {
        Map<String, FieldModel> fields = new HashMap<>();
        FieldType fieldType = FieldType.OBJECT;
        String objectType = Type.MAP.getPackageName();
        String firstParamType = Type.STRING.getPackageName();
        String secondParamType = Type.OBJECT.getPackageName();
        VariableModel variableModel = new VariableModel(objectType, new VariableModel[]{new VariableModel(firstParamType), new VariableModel(secondParamType)});
        FieldModel model = getFieldModel(fieldType, variableModel, new Modifier[0]);
        fields.put("additionalProperties", model);
        return new ClassDesc(className + "Property",  getTypeModel(com.flipkart.lyrics.model.Type.CLASS, null, fields, null));
    }

    private String getAcutalClassName(String className) {
        if (className.contains(".")) {
            return className;
        }
        return packageName + "." + className;
    }

    private FieldModel fieldDescToFieldModel(FieldDesc fieldDesc) {
        FieldType fieldType = getFieldType(fieldDesc.getType());
        VariableModel variableModel = new VariableModel();
        boolean array = false;
        if (fieldDesc.getType()!= null &&
                fieldDesc.getType().equals(Type.ARRAY)) {
            if (fieldDesc.getItems() != null) {
                variableModel = new VariableModel(Type.ARRAY.getPackageName(), new VariableModel[]{getVariableModel(fieldDesc.getItems().getJavaType())});
            } else {
                String javaType = fieldDesc.getJavaType();
                if (javaType.length() > 2 && javaType.charAt(javaType.length() - 2) == '[' &&
                        javaType.charAt(javaType.length() - 1) == ']') {
                    javaType = javaType.substring(0, javaType.length() - 2);
                    array = true;
                }
                fieldType = FieldType.OBJECT;
                variableModel = new VariableModel(getAcutalClassName(javaType));
            }
        } else if (fieldDesc.getType() == null || fieldDesc.getType().equals(Type.OBJECT)) {
            variableModel = getVariableModel(fieldDesc.getJavaType());
        }
        if (fieldDesc.getFormat() != null){
            fieldType = FieldType.OBJECT;
            variableModel = getVariableModel(fieldDesc.getFormat().getPackageName());
        }
        boolean primitive = false;
        if (fieldDesc.isUsePrimitives()){
            primitive = true;
        }
        InitializerModel initializerModel = null;
        if (fieldDesc.getDefaultValue() != null) {
            initializerModel = new InitializerModel(fieldDesc.getDefaultValue());
        }
        return new FieldModel(null , fieldType, variableModel, primitive,  new Modifier[0] , false, null, !fieldDesc.isOptional(), false, false, array, initializerModel, false);
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

    private List<ClassDesc> generateEnum(String className, List<String> enums) {
        List<ClassDesc> classDescList = new ArrayList<>();
        com.flipkart.lyrics.model.Type type = com.flipkart.lyrics.model.Type.ENUM;
        List<String> values = null;
        if (enums != null) {
            values = new ArrayList<>();
            for (String enumValue : enums) {
                values.add(enumValue);
            }
        }
        classDescList.add(new ClassDesc(className, getTypeModel(type, null, null, values)));
        return classDescList;
    }

    private FieldModel getFieldModel(FieldType fieldType, VariableModel variableModel, Modifier[] modifiers) {
        return new FieldModel(null, fieldType, variableModel, false, modifiers , false, null, false, false, false, false, null, false);
    }

    private TypeModel getTypeModel(com.flipkart.lyrics.model.Type type, VariableModel extendsModel, Map<String, FieldModel> fields, List<String> values) {
        return new TypeModel(type, new Modifier[0], extendsModel, new HashSet(), new ArrayList(), new ArrayList(), fields, new LinkedHashMap(), null, values, new LinkedHashMap(), new ArrayList<>(), null,null,null,null);
    }

    private VariableModel getVariableModel(String javaType) {
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
                VariableModel variableModel = new VariableModel(getAcutalClassName(javaType.substring(iterator, iterator + punctIndex).trim()));
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
