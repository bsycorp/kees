package com.bsycorp.kees.models;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public class ResourceParameter extends Parameter {

    private String storageKey;
    private String localValue;
    private Properties rawInput;

    public ResourceParameter(String annotationName, String annotationValue) throws IOException {
        setName(annotationName);

        //parse comma separate input by changing comma to newline, then parsing like a properties file
        rawInput = new Properties();
        rawInput.load(new ByteArrayInputStream(annotationValue.replace(",", "\n").getBytes()));

        //have all properties so set into parameters, setter should throw if invalid
        setStorageKey(rawInput, rawInput.getProperty("storageKey"));
        setLocalValue(rawInput, rawInput.getProperty("localModeValue"));
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(Properties properties, String storageKey) {
        this.storageKey = storageKey;
    }

    public String getLocalValue() {
        return localValue;
    }

    public void setLocalValue(Properties properties, String localValue) {
        if(localValue != null){
            this.localValue = localValue;
        }
    }

    @Override
    public String getStorageSuffix() {
        return getStorageKey() != null ? getStorageKey() : getParameterName();
    }

    @Override
    public String getStorageFullPath(String storagePrefix) {
        if(storagePrefix.endsWith("/")) storagePrefix = storagePrefix.substring(0, storagePrefix.length()-1);
        return String.format("%s/%s", storagePrefix, getStorageSuffix());
    }
}
