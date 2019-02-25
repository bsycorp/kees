package com.bsycorp.kees.data;

import com.bsycorp.kees.gpg.GPGKeyGenerator;
import com.bsycorp.kees.models.Parameter;
import com.bsycorp.kees.models.SecretTypeEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeterministicDataProvider extends AbstractDataProvider {

    private static Logger LOG = LoggerFactory.getLogger(DeterministicDataProvider.class);
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final String PRECALCULATED_VALUES_FILE_NAME = "1000_deterministic_rsa_keys.json";
    private final List<String[]> encodedPrecalculatedValues;


    public DeterministicDataProvider() {
        //use pregenerated keys for everything, still have different keys so we still test key assignment code paths
        //when they were being deterministically generated init containers were taking along time to start because of all the RSA algo compute
        //this class is only used when local-mode=true
        try {
            final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PRECALCULATED_VALUES_FILE_NAME);
            final ObjectMapper objectMapper = new ObjectMapper();
            encodedPrecalculatedValues = objectMapper.readValue(inputStream , new TypeReference<List<String[]>>(){});
        } catch (IOException e) {
            throw new RuntimeException("Could not load pre-calculated rsa keys");
        }
    }

    @Override
    public byte[] generateRaw(SecretTypeEnum type, String annotationName, int size) {
        LOG.info("Generating deterministic {} data for key '{}' of size: {}", type, annotationName, size);
        return super.generateRaw(type, annotationName, size);
    }

    @Override
    public Object[] generatePairedRaw(SecretTypeEnum type, String annotationName, int size, String userId) {
        LOG.info("Using pre-generated value for {}", annotationName);
        String rootAnnotationName = Parameter.extractBareParameterName(annotationName);
        String[] values = encodedPrecalculatedValues.get(
                Math.abs(rootAnnotationName.hashCode()) % encodedPrecalculatedValues.size());

        if (SecretTypeEnum.GPG == type) {
            final byte[] password = super.generateRaw(SecretTypeEnum.PASSWORD, annotationName, 128);
            /*
             * Use the deterministic RSA keys to generate GPG secret key. These are encoded, so decode prior.
             */
            final GPGKeyGenerator gpgKeyGenerator = new GPGKeyGenerator();
            final GPGKeyGenerator.GPGKeyPair gpgKeyPair = gpgKeyGenerator.generateDeterministicKeyPair(
                    userId, new String(password), DECODER.decode(values[0]), DECODER.decode(values[1]));
            return new Object[]{gpgKeyPair.getPublicKey(), gpgKeyPair.getPrivateKey(), userId.getBytes(), password};
        }

        return new Object[]{
                DECODER.decode(values[0]), DECODER.decode(values[1])
        };
    }

    @Override
    protected SecureRandom getRandomFromKey(String key) {
        //only seed based on the name without the field selector, so RSA keys use the same seed
        String comparisonKey = Parameter.extractBareParameterName(key);
        InsecureRandom random = new InsecureRandom();
        random.setSeed(comparisonKey.hashCode());
        return random;
    }
}
