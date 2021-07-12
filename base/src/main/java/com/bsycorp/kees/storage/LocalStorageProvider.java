package com.bsycorp.kees.storage;

import com.bsycorp.kees.data.DeterministicDataProvider;
import com.bsycorp.kees.models.Parameter;
import com.bsycorp.kees.models.ResourceParameter;
import com.bsycorp.kees.models.SecretParameter;
import com.bsycorp.kees.models.SecretTypeEnum;
import java.util.Collections;
import java.util.List;

public class LocalStorageProvider implements StorageProvider {

    private DeterministicDataProvider dataProvider = new DeterministicDataProvider();

    @Override
    public void put(String storagePrefix, Parameter key, String value, Boolean ignorePutFailure) {
        //noop
    }

    @Override
    public void delete(String storagePrefix, Parameter key, String expectedValue) {
        //noop
    }

    @Override
    public String getValueByKey(String storagePrefix, Parameter parameter) {
        if (parameter instanceof SecretParameter) {
            SecretParameter secretParameter = (SecretParameter) parameter;
            if (secretParameter.getLocalValue() != null) {
                //local values are already encoded so return
                return secretParameter.getLocalValue();

            } else if (secretParameter.getType() == SecretTypeEnum.GPG && secretParameter.getFieldName().equals("password")) {
                return dataProvider.generatePairedBase64Encoded(secretParameter.getType(), secretParameter.getParameterName(), secretParameter.getSize(), secretParameter.getUserId())[2];
            } else if ((secretParameter.getType() == SecretTypeEnum.RSA || secretParameter.getType() == SecretTypeEnum.GPG)
                    && secretParameter.getFieldName().equals("public")) {
                return dataProvider.generatePairedBase64Encoded(secretParameter.getType(), secretParameter.getParameterName(), secretParameter.getSize(), secretParameter.getUserId())[0];
            } else if ((secretParameter.getType() == SecretTypeEnum.RSA || secretParameter.getType() == SecretTypeEnum.GPG)
                    && secretParameter.getFieldName().equals("private")) {
                return dataProvider.generatePairedBase64Encoded(secretParameter.getType(), secretParameter.getParameterName(), secretParameter.getSize(), secretParameter.getUserId())[1];
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
    public String getKeyByParameterAndValue(String storagePrefix, Parameter parameter, String value) {
        byte[] bytes = dataProvider.generateRaw(SecretTypeEnum.RANDOM, storagePrefix + "/" + value, 3*8);
        int index = Byte.toUnsignedInt(bytes[0]) + Byte.toUnsignedInt(bytes[1]) + Byte.toUnsignedInt(bytes[2]);
        //generate 3 byte of deterministic entropy from path
        return parameter.getStorageFullPath(storagePrefix) + "." + index;
    }

    @Override
    public List<String> getKeysByParameter(String storagePrefix, Parameter parameter) {
        return Collections.emptyList();
    }

    @Override
    public boolean exists(String storagePrefix, Parameter key) {
        return false;
    }

}
