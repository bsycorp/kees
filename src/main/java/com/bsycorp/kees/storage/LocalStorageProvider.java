package com.bsycorp.kees.storage;

import com.bsycorp.kees.data.DeterministicDataProvider;
import com.bsycorp.kees.models.Parameter;
import com.bsycorp.kees.models.ResourceParameter;
import com.bsycorp.kees.models.SecretParameter;
import com.bsycorp.kees.models.SecretTypeEnum;

public class LocalStorageProvider implements StorageProvider {

    private DeterministicDataProvider dataProvider = new DeterministicDataProvider();

    @Override
    public void put(String storagePrefix, Parameter key, String value) {
        //noop
    }

    @Override
    public String get(String storagePrefix, Parameter parameter) {
        if (parameter instanceof SecretParameter) {
            SecretParameter secretParameter = (SecretParameter) parameter;
            if (secretParameter.getLocalValue() != null) {
                //local values are already encoded so return
                return secretParameter.getLocalValue();

            } else if (secretParameter.getType() == SecretTypeEnum.RSA && secretParameter.getFieldName().equals("public")) {
                return dataProvider.generatePairedBase64Encoded(secretParameter.getType(), secretParameter.getParameterName(), secretParameter.getSize())[0];

            } else if (secretParameter.getType() == SecretTypeEnum.RSA && secretParameter.getFieldName().equals("private")) {
                return dataProvider.generatePairedBase64Encoded(secretParameter.getType(), secretParameter.getParameterName(), secretParameter.getSize())[1];

            } else {
                return dataProvider.generateBase64Encoded(secretParameter.getType(), secretParameter.getParameterName(), secretParameter.getSize());

            }

        } else if (parameter instanceof ResourceParameter) {
            ResourceParameter resourceParameter = (ResourceParameter) parameter;
            if (resourceParameter.getLocalValue() != null) {
                return resourceParameter.getLocalValue();
            } else {
                throw new RuntimeException("Can't generate value for resource parameter locally");
            }

        } else {
            throw new RuntimeException("Unsupported parameter type: " + parameter.getClass());
        }
    }

    @Override
    public boolean exists(String storagePrefix, Parameter key) {
        return false;
    }

}
