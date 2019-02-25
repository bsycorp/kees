package com.bsycorp.kees.data;

import com.bsycorp.kees.gpg.GPGKeyGenerator;
import com.bsycorp.kees.models.SecretTypeEnum;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDataProvider implements DataProvider {

    private static BaseConverter base62Converter = new BaseConverter("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
    private static Logger LOG = LoggerFactory.getLogger(AbstractDataProvider.class);

    @Override
    public Object[] generatePairedRaw(SecretTypeEnum type, String annotationName, int size, String userId) {
        SecureRandom random = getRandomFromKey(annotationName);

        if (type == SecretTypeEnum.RSA) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(size, random);
                KeyPair keyPair = kpg.generateKeyPair();
                //Note for future nick*, getEncoded() is actually DER encoding.. so its still a 'raw' value..
                return new Object[]{
                        keyPair.getPublic().getEncoded(), keyPair.getPrivate().getEncoded()
                };
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Error generating RSA key", e);
                throw new RuntimeException("Error generating RSA key");
            }
        } else if (type == SecretTypeEnum.GPG) {
            // generate password
            byte[] password = generateRaw(SecretTypeEnum.PASSWORD, annotationName, 128);

            // generate gpg key
            GPGKeyGenerator.GPGKeyPair gpgKeyPair = new GPGKeyGenerator().generateKeyPair(
                    userId, new String(password), size, random);
            return new Object[]{gpgKeyPair.getPublicKey(), gpgKeyPair.getPrivateKey(), userId.getBytes(), password};
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
    public String[] generatePairedBase64Encoded(SecretTypeEnum type, String annotationName, int size, String userId) {
        Object[] results = generatePairedRaw(type, annotationName, size, userId);
        return Arrays.stream(results).map(o -> Base64.getEncoder().encodeToString((byte[]) o)).toArray(String[]::new);
    }
}
