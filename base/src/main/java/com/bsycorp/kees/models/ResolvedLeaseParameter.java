package com.bsycorp.kees.models;

import java.io.IOException;

public class ResolvedLeaseParameter extends LeaseParameter {

    private String leaseValue;

    public ResolvedLeaseParameter(LeaseParameter parameter, String leaseValue) throws IOException {
        super(parameter);
        setLeaseValue(leaseValue);
    }

    public void setLeaseValue(String leaseValue) {
        this.leaseValue = leaseValue;
    }

    @Override
    public String getStorageFullPath(String storagePrefix) {
        if (storagePrefix.endsWith("/")) storagePrefix = storagePrefix.substring(0, storagePrefix.length() - 1);
        return String.format("%s/leases/%s.%s", storagePrefix, getStorageSuffix(), leaseValue);
    }
}
