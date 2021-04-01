package com.bsycorp.kees.storage;

import com.bsycorp.kees.models.Parameter;
import java.util.HashMap;
import java.util.Map;

public class InMemoryStorageProvider implements StorageProvider {

    private Map<String, String> store = new HashMap<>();
    private int storeHighwaterMark = 0;

    @Override
    public void put(String storagePrefix, Parameter key, String value, Boolean ignorePutFailure) {
        String fullKey = key.getStorageFullPath(storagePrefix);
        store.put(fullKey, value);
        if (store.size() > storeHighwaterMark) storeHighwaterMark = store.size();
    }

    @Override
    public void delete(String storagePrefix, Parameter key, String expectedValue) {
        String fullKey = key.getStorageFullPath(storagePrefix);
        store.remove(fullKey);
    }

    @Override
    public String getValueByKey(String storagePrefix, Parameter key) {
        String fullKey = key.getStorageFullPath(storagePrefix);
        return store.get(fullKey);
    }

    @Override
    public String getKeyByParameterAndValue(String storagePrefix, Parameter parameter, String value) {
        for (Map.Entry<String, String> entry : store.entrySet()) {
            if (entry.getKey().startsWith(parameter.getStorageFullPath(storagePrefix)) && entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean exists(String storagePrefix, Parameter key) {
        String fullKey = key.getStorageFullPath(storagePrefix);
        return store.containsKey(fullKey);
    }

    public Map<String, String> getStore() {
        return store;
    }

    public int getStoreHighwaterMark() {
        return storeHighwaterMark;
    }
}
