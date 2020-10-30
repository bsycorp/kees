package com.bsycorp.kees.storage;

import com.bsycorp.kees.models.Parameter;

import java.util.HashMap;
import java.util.Map;

public class InMemoryStorageProvider implements StorageProvider {

    private Map<String, String> store = new HashMap<>();

    @Override
    public void put(String storagePrefix, Parameter key, String value) {
        String fullKey = key.getStorageFullPath(storagePrefix);
        store.put(fullKey, value);
    }

    @Override
    public String get(String storagePrefix, Parameter key) {
        String fullKey = key.getStorageFullPath(storagePrefix);
        return store.get(fullKey);
    }

    @Override
    public boolean exists(String storagePrefix, Parameter key) {
        String fullKey = key.getStorageFullPath(storagePrefix);
        return store.containsKey(fullKey);
    }

    public Map<String, String> getStore() {
        return store;
    }
}
