package com.bsycorp.kees.models;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public class LeaseParameter extends Parameter {

    private LeaseKindEnum kind = LeaseKindEnum.INDEX;
    private String storageKeyPrefix;
    private int rangeStart;
    private int rangeEnd;
    private Properties rawInput;

    public LeaseParameter(LeaseParameter leaseParameter) {
        setKind(leaseParameter.getKind());
        setName(leaseParameter.getFullAnnotationName());
        this.storageKeyPrefix = leaseParameter.getStorageKeyPrefix();
        this.rangeStart = leaseParameter.getRangeStart();
        this.rangeEnd = leaseParameter.getRangeEnd();
    }

    public LeaseParameter(String annotationName, String annotationValue) throws IOException {
        setName(annotationName);

        //parse comma separate input by changing comma to newline, then parsing like a properties file
        rawInput = new Properties();
        rawInput.load(new ByteArrayInputStream(annotationValue.replace(",", "\n").getBytes()));

        //have all properties so set into parameters, setter should throw if invalid
        setKind(LeaseKindEnum.valueOf(rawInput.getProperty("kind")));
        setStorageKey(rawInput.getProperty("storageKeyPrefix"));
        setRangeStart(Integer.parseInt(rawInput.getProperty("rangeStart")));
        setRangeEnd(Integer.parseInt(rawInput.getProperty("rangeEnd")));
    }

    public LeaseKindEnum getKind() {
        return kind;
    }

    public void setKind(LeaseKindEnum kind) {
        this.kind = kind;
    }

    public String getStorageKeyPrefix() {
        return storageKeyPrefix;
    }

    public void setStorageKey(String storageKey) {
        this.storageKeyPrefix = storageKey;
    }

    public int getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(int rangeStart) {
        this.rangeStart = rangeStart;
    }

    public int getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(int rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    @Override
    public String getStorageSuffix() {
        return getStorageKeyPrefix() != null ? getStorageKeyPrefix() : getParameterName();
    }

    @Override
    public String getStorageFullPath(String storagePrefix) {
        if (storagePrefix.endsWith("/")) storagePrefix = storagePrefix.substring(0, storagePrefix.length() - 1);
        return String.format("%s/leases/%s", storagePrefix, getStorageSuffix());
    }
}
