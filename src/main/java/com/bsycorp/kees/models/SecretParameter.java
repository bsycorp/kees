package com.bsycorp.kees.models;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

public class SecretParameter extends Parameter {

    private SecretKindEnum kind;
    private SecretTypeEnum type;
    private int size;
    private String storageKey;
    private String localValue;
    private Properties rawInput;
    private String userId;

    public SecretParameter(String annotationName, String annotationValue) throws IOException {
        setName(annotationName);

        //parse comma separate input by changing comma to newline, then parsing like a properties file
        rawInput = new Properties();
        rawInput.load(new ByteArrayInputStream(annotationValue.replace(",", "\n").getBytes()));

        //have all properties so set into parameters, setter should throw if invalid
        setKind(rawInput, SecretKindEnum.valueOf(rawInput.getProperty("kind")));
        setType(rawInput, SecretTypeEnum.valueOf(rawInput.getProperty("type")));
        if (rawInput.getProperty("size") != null) {
            setSize(rawInput, Integer.parseInt(rawInput.getProperty("size")));
        } else if (getKind() == SecretKindEnum.DYNAMIC) {
            throw new RuntimeException("Size is required for dynamic secrets");
        }
        setStorageKey(rawInput, rawInput.getProperty("storageKey"));
        setLocalValue(rawInput, rawInput.getProperty("localModeValue"));
        if (type == SecretTypeEnum.GPG) {
            setUserId(rawInput.getProperty("userId"));
            if (userId != null  && !Pattern.compile(".+<.+>").matcher(userId).matches()) {
                throw new RuntimeException("userId must be of the form \"userId<email>\"");
            }
        }
    }

    public SecretKindEnum getKind() {
        return kind;
    }

    public void setKind(Properties properties, SecretKindEnum kind) {
        this.kind = kind;
    }

    public SecretTypeEnum getType() {
        return type;
    }

    public void setType(Properties properties, SecretTypeEnum type) {
        this.type = type;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public int getSize() {
        return size;
    }

    public void setSize(Properties properties, int size) {
        if ("RSA".equals(properties.get("type")) && !(size == 2048 || size == 4096)) {
            throw new RuntimeException("Invalid size for RSA type: " + size);
        } else if (!(size > 0 && size <= 8192)) {
            throw new RuntimeException("Invalid size: " + size);
        }
        this.size = size;
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
            this.localValue = localValue;
    }

    @Override
    public String getStorageSuffix() {
        return getStorageKey() != null ? getStorageKey() : getParameterNameWithField();
    }

    @Override
    public String getStorageFullPath(String storagePrefix) {
        if (storagePrefix.endsWith("/")) storagePrefix = storagePrefix.substring(0, storagePrefix.length() - 1);
        return String.format("%s/%s", storagePrefix, getStorageSuffix());
    }
}
