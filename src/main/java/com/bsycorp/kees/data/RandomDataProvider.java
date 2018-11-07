package com.bsycorp.kees.data;

import com.bsycorp.kees.models.SecretTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;

public class RandomDataProvider extends AbstractDataProvider {

    private static Logger LOG = LoggerFactory.getLogger(RandomDataProvider.class);
    private static SecureRandom secureRandom = new SecureRandom();

    @Override
    public Object[] generatePairedRaw(SecretTypeEnum type, String annotationName, int size) {
        LOG.info("Generating random {} data for key '{}' of size: {}", type, annotationName, size);
        return super.generatePairedRaw(type, annotationName, size);
    }

    @Override
    public byte[] generateRaw(SecretTypeEnum type, String annotationName, int size) {
        LOG.info("Generating random {} data for key '{}' of size: {}", type, annotationName, size);
        return super.generateRaw(type, annotationName, size);
    }

    protected SecureRandom getRandomFromKey(String key) {
        return secureRandom;
    }
}
