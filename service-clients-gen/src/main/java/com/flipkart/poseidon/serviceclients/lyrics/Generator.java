package com.flipkart.poseidon.serviceclients.lyrics;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.lyrics.Song;
import com.flipkart.lyrics.config.DefaultTune;
import com.flipkart.lyrics.config.Tune;
import com.flipkart.lyrics.model.TypeModel;
import com.flipkart.poseidon.serviceclients.idl.pojo.Version;
import com.sun.codemodel.JCodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by prasad.krishna on 14/03/17.
 */
public class Generator {


    private final static String IDL_BASE_PATH = ".src.main.resources.idl.";
    private final static String POJO_FOLDER_NAME = "pojos";
    private final static String SERVICE_FOLDER_NAME = "service";
    private final static String DESTINATION_JAVA_FOLDER = ".target.generated-sources.";
    private final static String PACKAGE_NAME = "com.flipkart.poseidon.serviceclients."; // module name and major version will be appended to this
    private final static Logger logger = LoggerFactory.getLogger(Generator.class);

    private static ObjectMapper objectMapper;
    private static String moduleParentPath;
    private static String modulePath;
    private static String moduleName;
    private static Version version;
    private static String packageName;
    private static String[] pojoOrdering;
    private static Song serviceClientSong;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    public static void main(String args[]) throws Exception {
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

    private static void generate() throws Exception {
        Tune serviceClientTune = new DefaultTune();
        serviceClientSong = new Song(serviceClientTune);
        String idlBasePath = IDL_BASE_PATH.replace('.', File.separatorChar);
        File pojoFolder = new File(modulePath + idlBasePath + POJO_FOLDER_NAME);
        generatePojo(pojoFolder);
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

    private static void generatePojo(String className, String filePath) throws IOException {
        String destinationFolder = modulePath + DESTINATION_JAVA_FOLDER.replace('.', File.separatorChar);
        TypeModel typeModel = objectMapper.readValue(new File(filePath), TypeModel.class);
        serviceClientSong.createType(className, packageName, typeModel, new File(destinationFolder));
    }

    private static void ensurePaths() {
        File javaFolder = new File(modulePath + DESTINATION_JAVA_FOLDER.replace('.', File.separatorChar));
        if (!javaFolder.exists() && !javaFolder.mkdirs()) {
            logger.warn("Couldn't create destination java folder");
        }
    }

    private static void printUsage() {
        logger.error("Module path and version are required. Pojo files ordering is optional");
        logger.error("IntelliJ IDEA Ex: $MODULE_DIR$ 1.0.0-SNAPSHOT pojo1.json,pojo2.json");
        logger.error("Maven Ex: ${project.basedir} ${project.version} pojo1.json,pojo2.json");
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

}
