package com.bsycorp.kees.storage;

import com.bsycorp.kees.models.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryStorageProvider implements StorageProvider {

    private Map<String, String> store = new HashMap<>();
    private int storeHighwaterMark = 0;
    private int storeGetCounter = 0;

    @Override
    public void put(String storagePrefix, Parameter key, String value, Boolean ignorePutFailure) {
        String fullKey = key.getStorageFullPath(storagePrefix);
        if (store.containsKey(fullKey) && !ignorePutFailure) {
            if (!ignorePutFailure) {
                throw new RuntimeException("Failed to put key and not ignoring failures");
            } else {
                return;
            }
        }
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
        storeGetCounter += 1;
        String fullKey = key.getStorageFullPath(storagePrefix);
        return store.get(fullKey);
    }

    @Override
    public String getKeyByParameterAndValue(String storagePrefix, Parameter parameter, String value) {
        storeGetCounter += 1;
        for (Map.Entry<String, String> entry : store.entrySet()) {
            if (entry.getKey().startsWith(parameter.getStorageFullPath(storagePrefix)) && entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public List<String> getKeysByParameter(String storagePrefix, Parameter parameter) {
        storeGetCounter += 1;
        return store.entrySet().stream()
                .filter(e -> e.getKey().startsWith(parameter.getStorageFullPath(storagePrefix)))
                .map(e -> e.getKey())
                .collect(Collectors.toList());
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

    public int getStoreGetCounter() {
        return storeGetCounter;
    }
}
