package com.bsycorp.kees.models;

import com.bsycorp.kees.Utils;

import java.io.IOException;

public abstract class Parameter {

    private String parameterName;
    private String fullAnnotationName;
    private String fieldName;

    public static Parameter construct(String annotationName, String annotationValue) throws IOException {
        if (annotationName.startsWith("secret." + Utils.getAnnotationDomain())) {
            return new SecretParameter(annotationName, annotationValue);
        } else if (annotationName.startsWith("resource." + Utils.getAnnotationDomain())) {
            return new ResourceParameter(annotationName, annotationValue);
        } else if (annotationName.startsWith("custom." + Utils.getAnnotationDomain())) {
            return new CustomParameter(annotationName, annotationValue);
        } else {
            //unsupported, skip
            return null;
        }
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getParameterNameWithField() {
        return fieldName == null ? parameterName : parameterName + "_" + fieldName;
    }

    public String getFullAnnotationName() {
        return fullAnnotationName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void overrideFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setName(String fullAnnotationName) {
        this.parameterName = fullAnnotationName.substring(fullAnnotationName.indexOf("/") + 1);
        //remove field from parameter name if it exists
        this.fieldName = extractFieldName(fullAnnotationName);
        this.parameterName = extractBareParameterName(parameterName);
        this.fullAnnotationName = fullAnnotationName;
    }

    public abstract String getStorageSuffix();

    public abstract String getStorageFullPath(String storagePrefix);

    public static String extractBareParameterName(String parameterName){
        return parameterName.matches(".*_[a-z]*$") ? parameterName.substring(0, parameterName.lastIndexOf("_")) : parameterName;
    }

    public static String extractFieldName(String parameterName){
        return parameterName.matches(".*_[a-z]*$") ? parameterName.substring(parameterName.lastIndexOf("_")+1) : null;
    }

}
