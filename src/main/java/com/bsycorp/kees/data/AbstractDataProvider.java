package com.bsycorp.kees.data;

import com.bsycorp.kees.models.SecretTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public abstract class AbstractDataProvider implements DataProvider {

    private static BaseConverter base62Converter = new BaseConverter("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
    private static Logger LOG = LoggerFactory.getLogger(AbstractDataProvider.class);

    @Override
    public Object[] generatePairedRaw(SecretTypeEnum type, String annotationName, int size) {
        SecureRandom random = getRandomFromKey(annotationName);

        if (type == SecretTypeEnum.RSA) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(size, random);
                KeyPair keyPair = kpg.generateKeyPair();
                //Note for future nick*, getEncoded() is actually DER encoding.. so its still a 'raw' value..
                return new Object[] {
                        keyPair.getPublic().getEncoded(), keyPair.getPrivate().getEncoded()
                };
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Error generating RSA key", e);
                throw new RuntimeException("Error generating RSA key");
            }

        } else {
            throw new RuntimeException("Unsupported secret type: " + type);
        }
    }

    @Override
    public byte[] generateRaw(SecretTypeEnum type, String annotationName, int size) {
        SecureRandom random = getRandomFromKey(annotationName);

        if (type == SecretTypeEnum.PASSWORD) {
            byte[] bytes = new byte[size];
            random.nextBytes(bytes);
            return base62Converter.encode(bytes).substring(0, size / 8).getBytes();

        } else if (type == SecretTypeEnum.RANDOM) {
            byte[] bytes = new byte[size / 8];
            random.nextBytes(bytes);
            return bytes;

        } else {
            throw new RuntimeException("Unsupported secret type: " + type);
        }
    }

    protected abstract SecureRandom getRandomFromKey(String annotationName);

    @Override
    public String generateBase64Encoded(SecretTypeEnum type, String annotationName, int size) {
        return Base64.getEncoder().encodeToString(generateRaw(type, annotationName, size));
    }

    @Override
    public String[] generatePairedBase64Encoded(SecretTypeEnum type, String annotationName, int size) {
        Object[] results = generatePairedRaw(type, annotationName, size);
        return new String[]{
                Base64.getEncoder().encodeToString((byte[]) results[0]),
                Base64.getEncoder().encodeToString((byte[]) results[1])
        };
    }
}
