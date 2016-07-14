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

import com.flipkart.poseidon.serviceclients.executor.CommandFailedException;
import com.flipkart.poseidon.serviceclients.executor.GitCommandExecutor;
import com.flipkart.poseidon.serviceclients.idl.pojo.ServiceIDL;
import com.flipkart.poseidon.serviceclients.idl.pojo.Version;
import com.flipkart.poseidon.serviceclients.idl.reader.IDLReader;
import com.sun.codemodel.JCodeModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

/**
 * Created by mohan.pandian on 20/02/15.
 *
 * Generates pojo and service client interface & implementation using
 * PojoGenerator  and ServiceGenerator respectively.
 */
public class Generator {
    private final static String IDL_BASE_PATH = ".src.main.resources.idl.";
    private final static String POJO_FOLDER_NAME = "pojos";
    private final static String SERVICE_FOLDER_NAME = "service";
    private final static String DESTINATION_JAVA_FOLDER = ".target.generated-sources.";
    private final static String PACKAGE_NAME = "com.flipkart.poseidon.serviceclients."; // module name and major version will be appended to this
    private final static JCodeModel jCodeModel = new JCodeModel();
    private final static Logger logger = LoggerFactory.getLogger(Generator.class);

    private static String moduleParentPath;
    private static String modulePath;
    private static String moduleName;
    private static Version version;
    private static String packageName;
    private static String[] pojoOrdering;

    public static void main(String[] args) throws Exception {
        if (!validateInput(args)) {
            printUsage();
            System.exit(-1);
        }
        logger.info("Using module parent path: {}", moduleParentPath);
        logger.info("Using module path: {}", modulePath);
        logger.info("Using module name: {}", moduleName);
        logger.info("Using version: {}", version);
        if (pojoOrdering != null && pojoOrdering.length > 0) {
            logger.info("Using pojo ordering: {}", Arrays.toString(pojoOrdering));
        }

        ensurePaths();
        generate();
    }

    private static boolean validateInput(String[] args) {
        if (args.length < 2) {
            return false;
        }
        int lastIndex = args[0].lastIndexOf(File.separatorChar);
        if (lastIndex <= 0) {
            logger.error("Invalid module path");
            return false;
        }
        modulePath = args[0];
        moduleName = modulePath.substring(lastIndex + 1);
        if (!moduleName.matches("^[a-zA-Z_]*$")) {
            logger.error("Invalid module name. Should contain only alphabets and underscore");
            return false;
        }
        moduleParentPath = modulePath.substring(0, lastIndex);

        determineVersion(args[1]);
        String majorVersion = "v" + version.getMajor();
        packageName = PACKAGE_NAME + moduleName + "." + majorVersion;

        if (args.length > 2) {
            pojoOrdering = args[2].split(",");
        }
        return true;
    }

    private static void determineVersion(String localPomVersion) {
        setVersion(localPomVersion);
//        updateVersion();
    }

    private static void setVersion(String moduleVersion) {
        String[] versions = moduleVersion.replace("-SNAPSHOT", "").split("\\.");
        version = new Version();
        int number;
        for (int i = 0; i < versions.length; i++) {
            try {
                number = Integer.parseInt(versions[i]);
            } catch (NumberFormatException e) {
                number = 0;
            }
            if (i == 0) {
                version.setMajor(number);
            } else {
                if (i == 1) {
                    version.setMinor(number);
                } else {
                    if (i == 2) {
                        version.setPatch(number);
                    }
                }
            }
        }
    }

    private static void updateVersion() {
        String idlBasePath = IDL_BASE_PATH.replace('.', File.separatorChar);
        File folder = new File(modulePath + idlBasePath + SERVICE_FOLDER_NAME);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null && files.length > 0) {
                Versioner.getInstance().updateVersion(moduleParentPath, moduleName, files[0].getPath(), version);
            }
        }
    }

    private static void printUsage() {
        logger.error("Module path and version are required. Pojo files ordering is optional");
        logger.error("IntelliJ IDEA Ex: $MODULE_DIR$ 1.0.0-SNAPSHOT pojo1.json,pojo2.json");
        logger.error("Maven Ex: ${project.basedir} ${project.version} pojo1.json,pojo2.json");
    }

    private static void ensurePaths() {
        File javaFolder = new File(modulePath + DESTINATION_JAVA_FOLDER.replace('.', File.separatorChar));
        if (!javaFolder.exists() && !javaFolder.mkdirs()) {
            logger.warn("Couldn't create destination java folder");
        }
    }

    private static void generate() throws Exception {
        String idlBasePath = IDL_BASE_PATH.replace('.', File.separatorChar);
        File pojoFolder = new File(modulePath + idlBasePath + POJO_FOLDER_NAME);
        generatePojo(pojoFolder);

        File serviceFolder = new File(modulePath + idlBasePath + SERVICE_FOLDER_NAME);
        generateService(serviceFolder);
    }

    private static void generatePojo(File pojoFolder) throws Exception {
        if (!pojoFolder.exists()) {
            return;
        }

        File[] files;
        if (pojoOrdering != null && pojoOrdering.length > 0) {
            files = new File[pojoOrdering.length];
            int i = 0;
            for (String fileName : pojoOrdering) {
                File file = new File(pojoFolder.getPath() + File.separator + fileName);
                if (file.isDirectory()) {
                    throw new IllegalArgumentException("Pojo ordering can't contain a directory");
                }
                files[i++] = file;
            }
        } else {
            files = pojoFolder.listFiles();
        }
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                generatePojo(file);
                continue;
            }
            logger.info("Generating from " + file.getName());
            String className = file.getName().replaceFirst("[.][^.]+$", ""); // Remove extension
            generatePojo(className, file.getPath());
        }
    }

    private static void generateService(File serviceFolder) throws Exception {
        if (!serviceFolder.exists()) {
            return;
        }

        File[] files = serviceFolder.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            logger.info("Generating from " + file.getName());
            String className = file.getName().replaceFirst("[.][^.]+$", ""); // Remove extension
            generateService(className, file.getPath());
        }
    }

    private static void generatePojo(String className, String filePath) throws Exception {
        JsonValidator.getInstance().validatePojo(filePath);

        String pojoJson = FileUtils.readFileToString(new File(filePath));
        //javaType should contain FQN of referenced (to-be-generated) class. But as the package is determined
        //at runtime (containing version), we inject the package name here
        pojoJson = pojoJson.replaceAll("\"javaType\"\\s*:\\s*\"(?!.*\\.)", "\"javaType\": \"" + packageName + ".");
        String destinationFolder = modulePath + DESTINATION_JAVA_FOLDER.replace('.', File.separatorChar);

        PojoGenerator.getInstance().generate(pojoJson, jCodeModel, destinationFolder, className, packageName);
    }

    private static void generateService(String className, String filePath) throws Exception {
        JsonValidator.getInstance().validateService(filePath);

        ServiceIDL serviceIdl = IDLReader.getIDL(filePath);
        if (!className.endsWith("Service")) {
            className += "Service";
        }
        serviceIdl.getService().setName(className);
        serviceIdl.getService().setPackageName(packageName);
        serviceIdl.setVersion(version);
        serviceIdl.setExplicit(true);
        String destinationFolder = modulePath + DESTINATION_JAVA_FOLDER.replace('.', File.separatorChar);

        ServiceGenerator.getInstance().generateInterface(serviceIdl, jCodeModel, destinationFolder);
        ServiceGenerator.getInstance().generateImpl(serviceIdl, jCodeModel, destinationFolder);

        // create a generic exception class for a service
        String exceptionClassName = packageName + "." + serviceIdl.getService().getName() + "Exception";
        ExceptionGenerator.getInstance().addExceptionClass(destinationFolder, exceptionClassName);

        // add exception classes for particular response types
        if (serviceIdl.getExceptions() != null && serviceIdl.getExceptions().size() > 0) {
            for (int response : serviceIdl.getExceptions().keySet()) {
                exceptionClassName = packageName + "." + serviceIdl.getExceptions().get(response) + "Exception";
                ExceptionGenerator.getInstance().addExceptionClass(destinationFolder, exceptionClassName);
            }
        }
    }
}
