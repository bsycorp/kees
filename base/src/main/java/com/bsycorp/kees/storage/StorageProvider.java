package com.bsycorp.kees.storage;

import com.bsycorp.kees.models.Parameter;

public interface StorageProvider {

    //put() takes a value in encoded form and write to storage
    void put(String storagePrefix, Parameter key, String value, Boolean ignorePutFailure);

    void delete(String storagePrefix, Parameter key, String expectedValue);

    //get() returns the stored value for the given key, to support binary values all stored values are b64 encoded first
    String getValueByKey(String storagePrefix, Parameter key);

    //get() returns the stored key for the given value, to support leases where the key is what we are looking for
    String getKeyByParameterAndValue(String storagePrefix, Parameter parameter, String value);

    //get() returns the stored value for the given key, to support binary values all stored values are b64 encoded first
    boolean exists(String storagePrefix, Parameter key);

}
