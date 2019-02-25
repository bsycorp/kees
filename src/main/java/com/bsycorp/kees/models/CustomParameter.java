package com.bsycorp.kees.models;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public class CustomParameter extends Parameter {

    private String storageKey;
    private String fixedValue;
    private Properties rawInput;

    public CustomParameter(String annotationName, String annotationValue) throws IOException {
        setName(annotationName);

        //parse comma separate input by changing comma to newline, then parsing like a properties file
        rawInput = new Properties();
        rawInput.load(new ByteArrayInputStream(annotationValue.replace(",", "\n").getBytes()));

        setStorageKey(rawInput.getProperty("storageKey"));
        setFixedValue(rawInput.getProperty("fixedValue"));
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getFixedValue() {
        return fixedValue;
    }

    public void setFixedValue(String fixedValue) {
        this.fixedValue = fixedValue;
    }

    @Override
    public String getStorageSuffix() {
        return getStorageKey() != null ? getStorageKey() : getParameterName();
    }

    @Override
    public String getStorageFullPath(String storagePrefix) {
        if(storagePrefix.endsWith("/")) storagePrefix = storagePrefix.substring(0, storagePrefix.length() - 1);
        return String.format("%s/%s", storagePrefix, getStorageSuffix());
    }
}
