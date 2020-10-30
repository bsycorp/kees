package com.bsycorp.kees.storage;

import com.bsycorp.kees.Utils;
import com.bsycorp.kees.models.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DescribeParametersRequest;
import software.amazon.awssdk.services.ssm.model.DescribeParametersResponse;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.ParametersFilter;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import software.amazon.awssdk.services.ssm.model.SsmException;

public class SSMStorageProvider implements StorageProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SSMStorageProvider.class);

    private SsmClient client = Utils.getSSMClient();

    @Override
    public void put(String storagePrefix, Parameter key, String value) {
        String ssmPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Setting SSM value for key: {}", ssmPath);

        try {
            client.putParameter(PutParameterRequest.builder().type(ParameterType.SECURE_STRING).name(ssmPath).value(value).build());
            //success!
            LOG.info("Set SSM parameter and value for key: {}", ssmPath);

        } catch (SsmException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when setting parameter for key: " + ssmPath, e);
        }

    }

    @Override
    public String get(String storagePrefix, Parameter key) {
        String ssmPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Looking up SSM value for key: {}", ssmPath);

        try {
            GetParameterResponse ssmResult = client.getParameter(GetParameterRequest.builder().withDecryption(true).name(ssmPath).build());
            if (ParameterType.SECURE_STRING != ssmResult.parameter().type()){
                LOG.error("Found SSM parameter with invalid type, type should always be {}! Found: {}",
                        ParameterType.SECURE_STRING.toString(),
                        ssmResult.parameter().type()
                );
                return null;
            }
            return ssmResult.parameter().value();

        } catch (ParameterNotFoundException e) {
            LOG.warn("Couldn't find parameter for key: {}", ssmPath);
            return null;

        } catch (SsmException e) {
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
            DescribeParametersResponse ssmResult = client.describeParameters(
                    DescribeParametersRequest.builder().filters(
                            ParametersFilter.builder().key("Name").values(ssmPath).build()
                    ).build()
            );
            return ssmResult.parameters().size() > 0;

        } catch (ParameterNotFoundException e) {
            LOG.warn("Couldn't find parameter for key: {}", ssmPath);
            return false;

        } catch (SsmException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when looking up parameter with key: " + ssmPath, e);
            return false;
        }
    }
}
