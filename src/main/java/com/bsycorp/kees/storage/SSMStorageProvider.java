package com.bsycorp.kees.storage;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.AWSSimpleSystemsManagementException;
import com.amazonaws.services.simplesystemsmanagement.model.DescribeParametersRequest;
import com.amazonaws.services.simplesystemsmanagement.model.DescribeParametersResult;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType;
import com.amazonaws.services.simplesystemsmanagement.model.ParametersFilter;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest;
import com.bsycorp.kees.Utils;
import com.bsycorp.kees.models.Parameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSMStorageProvider implements StorageProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SSMStorageProvider.class);

    private AWSSimpleSystemsManagement client = Utils.getSSMClient();

    @Override
    public void put(String storagePrefix, Parameter key, String value) {
        String ssmPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Setting SSM value for key: {}", ssmPath);

        try {
            client.putParameter(new PutParameterRequest().withType(ParameterType.SecureString).withName(ssmPath).withValue(value));
            //success!
            LOG.info("Set SSM parameter and value for key: {}", ssmPath);

        } catch (AWSSimpleSystemsManagementException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when setting parameter for key: " + ssmPath, e);
        }

    }

    @Override
    public String get(String storagePrefix, Parameter key) {
        String ssmPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Looking up SSM value for key: {}", ssmPath);

        try {
            GetParameterResult ssmResult = client.getParameter(new GetParameterRequest().withWithDecryption(true).withName(ssmPath));
            if (!ParameterType.SecureString.toString().equals(ssmResult.getParameter().getType())){
                LOG.error("Found SSM parameter with invalid type, type should always be {}! Found: {}",
                        ParameterType.SecureString.toString(),
                        ssmResult.getParameter().getType()
                );
                return null;
            }
            return ssmResult.getParameter().getValue();

        } catch (ParameterNotFoundException e) {
            LOG.warn("Couldn't find parameter for key: {}", ssmPath);
            return null;

        } catch (AWSSimpleSystemsManagementException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when looking up parameter with key: " + ssmPath, e);
            return null;
        }
    }

    @Override
    public boolean exists(String storagePrefix, Parameter key) {
        String ssmPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Checking SSM value exists for key: {}", ssmPath);

        try {
            DescribeParametersResult ssmResult = client.describeParameters(new DescribeParametersRequest().withFilters(new ParametersFilter().withKey("Name").withValues(ssmPath)));
            return ssmResult.getParameters().size() > 0;

        } catch (ParameterNotFoundException e) {
            LOG.warn("Couldn't find parameter for key: {}", ssmPath);
            return false;

        } catch (AWSSimpleSystemsManagementException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when looking up parameter with key: " + ssmPath, e);
            return false;
        }
    }
}
