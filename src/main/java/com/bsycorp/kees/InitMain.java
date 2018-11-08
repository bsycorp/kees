package com.bsycorp.kees;

import com.bsycorp.kees.models.Parameter;
import com.bsycorp.kees.models.ResourceParameter;
import com.bsycorp.kees.models.SecretParameter;
import com.bsycorp.kees.storage.DynamoDBStorageProvider;
import com.bsycorp.kees.storage.LocalStorageProvider;
import com.bsycorp.kees.storage.StorageProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.bsycorp.kees.Utils.getAnnotationDomain;

public class InitMain {

    private static Logger LOG = LoggerFactory.getLogger(InitMain.class);

    private StorageProvider storageProvider;
    private AnnotationParser annotationParser = new AnnotationParser();

    public static void main(String... argv) throws Exception {
        InitMain main = new InitMain();
        try {
            main.run();
        } catch (Exception e) {
            LOG.error("Error in execution", e);
            throw e;
        }
    }

    public void run() throws Exception {

        String envLabel = Utils.getEnvironment().get("ENV_LABEL");
        if (envLabel == null) {
            throw new Exception("ENV_LABEL is required");
        }

        String storagePrefix = getStoragePrefix();
        String tableName = Utils.getTableName(envLabel);

        LOG.info("Starting init process, with configuration: \n\t annotations: {}\n\t local-mode: {}\n\t storage-prefix: {}\n\t secrets-output: {}\n\t resources-output: {} table-name: {}",
                getAnnotationsFile(),
                isLocalMode(),
                storagePrefix,
                getSecretsFile(),
                getResourcesFile(),
                tableName
        );

        //check if in local mode, setup providers accordingly
        if (isLocalMode()) {
            storageProvider = new LocalStorageProvider();
        } else {
            if (tableName == null) {
                throw new Exception("No table name annotation specified");
            }
            storageProvider = new DynamoDBStorageProvider(tableName);
        }

        //parser annotations from podinfo
        Map<String, String> retrievedSecrets = new HashMap<>();
        Map<String, String> retrievedResources = new HashMap<>();

        boolean hadError = false;
        List<Parameter> parameters = annotationParser.parseFlatFile(getAnnotationsFile());
        LOG.info("Found {} parameters to lookup", parameters.size());
        for (Parameter parameter : parameters) {
            try {
                String fileKey = parameter.getParameterNameWithField();
                if (parameter instanceof SecretParameter) {
                    //secret params are encoded at rest, so get retrieves an encodedValue
                    String encodedValue = storageProvider.get(storagePrefix, parameter);
                    if (encodedValue == null) {
                        LOG.info("Couldn't find value for parameter: {}, failing..", fileKey);
                        throw new RuntimeException("Couldn't find value for parameter:" + parameter.getFullAnnotationName());
                    }
                    retrievedSecrets.put(fileKey, encodedValue);

                } else if (parameter instanceof ResourceParameter) {
                    //resource params are encoded at rest, so retrieves an encoded value
                    String encodedValue = storageProvider.get(storagePrefix, parameter);
                    if (encodedValue == null) {
                        LOG.info("Couldn't find value for parameter: {}, failing..", fileKey);
                        throw new RuntimeException("Couldn't find value for parameter:" + parameter.getFullAnnotationName());
                    }
                    retrievedResources.put(fileKey, encodedValue);

                } else {
                    throw new RuntimeException("Unsupported parameter type");
                }
            } catch (Exception e) {
                hadError = true;
                LOG.error("Error getting value for annotation: " + parameter.getFullAnnotationName(), e);
            }
        }

        if (hadError) {
            //had error so don't write and just fail
            throw new RuntimeException("Didn't process all annotations correctly, failing..");
        }

        //store values for writing to file
        LOG.info("Writing out all retrieved secrets, found {}, to {}", retrievedSecrets.size(), getSecretsFile());
        StringBuilder secretsOutput = new StringBuilder();
        retrievedSecrets.entrySet().stream().forEach(e -> secretsOutput.append(e.getKey() + "=" + e.getValue() + "\n"));
        FileUtils.writeStringToFile(getSecretsFile(), secretsOutput.toString(), "UTF-8");
        LOG.info("Stored retrieved secrets. Done.");

        LOG.info("Writing out all retrieved resources, found {}, to {}", retrievedResources.size(), getResourcesFile());
        StringBuilder resourcesOutput = new StringBuilder();
        retrievedResources.entrySet().stream().forEach(e -> resourcesOutput.append(e.getKey() + "=" + e.getValue() + "\n"));
        retrievedResources.entrySet().stream().forEach(e -> {
            try {
                FileUtils.writeStringToFile(getResourcesFileForKey(e.getKey()), new String(Base64.getDecoder().decode(e.getValue())), "UTF-8");
            } catch (IOException e1) {
                LOG.error("Unable to write resource to file");
            }
        });
        FileUtils.writeStringToFile(getResourcesFile(), resourcesOutput.toString(), "UTF-8");
        LOG.info("Stored retrieved resources. Done.");
    }

    String getStoragePrefix() throws IOException {
        String storagePrefix;

        Properties properties = new Properties();
        properties.load(new FileInputStream(getAnnotationsFile()));
        //if local mode exists and its true
        String annotationKey = "init." + Utils.getAnnotationDomain() + "/storage-prefix";
        if (properties.containsKey(annotationKey)) {
            storagePrefix = (String) properties.get(annotationKey);
        } else {
            storagePrefix = "/";
        }
        //remote quotes as storage prefix might have them, and we dont want them
        storagePrefix = storagePrefix.replace("\"", "").replace("'", "");

        return storagePrefix;
    }

    File getAnnotationsFile() {
        File annotationFile;

        if (Utils.getEnvironment().get("ANNOTATIONS_FILE") == null) {
            annotationFile = new File("/podinfo/annotations");
        } else {
            annotationFile = new File(Utils.getEnvironment().get("ANNOTATIONS_FILE"));
        }

        return annotationFile;
    }

    File getSecretsFile() {
        File secretsFile;

        if (Utils.getEnvironment().get("SECRETS_FILE") == null) {
            secretsFile = new File("/bsycorp-init/secrets.properties");
        } else {
            secretsFile = new File(Utils.getEnvironment().get("SECRETS_FILE"));
        }

        return secretsFile;
    }

    File getResourcesFile() {
        File resourcesFile;

        if (Utils.getEnvironment().get("RESOURCES_FILE") == null) {
            resourcesFile = new File("/bsycorp-init/resources.properties");
        } else {
            resourcesFile = new File(Utils.getEnvironment().get("RESOURCES_FILE"));
        }

        return resourcesFile;
    }

    File getResourcesFileForKey(String key) {
        File resourcesFile;

        if (Utils.getEnvironment().get("RESOURCES_FILE_PATH") != null) {
            resourcesFile = new File(Utils.getEnvironment().get("RESOURCES_FILE_PATH") + "/" + key);
        } else {
            resourcesFile = new File("/bsycorp-init/" + key);
        }

        return resourcesFile;
    }

    boolean isLocalMode() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(getAnnotationsFile()));
        //if local mode exists and its true
        return properties.containsKey("init." + getAnnotationDomain() + "/local-mode") && ((String) properties.get("init." + getAnnotationDomain() + "/local-mode")).contains("true");
    }
}
