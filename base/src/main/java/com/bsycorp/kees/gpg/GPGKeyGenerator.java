package com.bsycorp.kees.gpg;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyConverter;

public class GPGKeyGenerator {

    private PGPKeyRingGenerator createKeyRingGenerator(String userId, String password, int keySize, SecureRandom random) {
        try {
            final RSAKeyPairGenerator keyPairGenerator = new RSAKeyPairGenerator();
            keyPairGenerator.init(new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), random, keySize, 12));

            final BcPGPKeyPair generalKeyPair = new BcPGPKeyPair(PGPPublicKey.RSA_GENERAL, keyPairGenerator.generateKeyPair(), new Date());

            return new PGPKeyRingGenerator(
                    PGPPublicKey.RSA_GENERAL,
                    generalKeyPair,
                    userId,
                    new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1), generateGeneralSubpacketGenerator().generate(),
                    null, new BcPGPContentSignerBuilder(PGPPublicKey.RSA_SIGN, HashAlgorithmTags.SHA256),
                    new BcPBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256).build(password.toCharArray()));
        } catch (PGPException e) {
            throw new RuntimeException("Error creating PGPKeyRingGenerator", e);
        }
    }

    private PGPSignatureSubpacketGenerator generateGeneralSubpacketGenerator() {
        final PGPSignatureSubpacketGenerator subpacketGenerator = new PGPSignatureSubpacketGenerator();
        subpacketGenerator.setKeyExpirationTime(true, 0);
        subpacketGenerator.setKeyFlags(false,
                KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE | KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER);
        return subpacketGenerator;
    }

    public GPGKeyPair generateKeyPair(String userId, String password, int keySize, SecureRandom random) {
        final PGPSecretKeyRing secretKeyRing = createKeyRingGenerator(userId, password, keySize, random).generateSecretKeyRing();
        return getGPGKeyPair(secretKeyRing);
    }

    public GPGKeyPair generateDeterministicKeyPair(String userId, String password, byte[] rsaPublicKey, byte[] rsaPrivateKey) {
        // Need to set to a fixed creation date for the gpg keys to be deterministic.
        final Date createdDate = Date.from(Instant.parse("2010-01-01T00:00:00.000Z"));

        final JcaPGPKeyConverter converter = new JcaPGPKeyConverter();
        converter.setProvider("BC");

        final PublicKey publicKey = GPGKeyUtils.getRSAPublicKey(rsaPublicKey);
        final PrivateKey privateKey = GPGKeyUtils.getRSAPrivateKey(rsaPrivateKey);

        try {
            final PGPPublicKey pgpPublicKey = converter.getPGPPublicKey(PublicKeyAlgorithmTags.RSA_GENERAL, publicKey, createdDate);
            final PGPPrivateKey pgpPrivateKey = converter.getPGPPrivateKey(pgpPublicKey, privateKey);

            final PGPKeyPair pgpKeyPair = new PGPKeyPair(pgpPublicKey, pgpPrivateKey);

            // Directly create PGPSecretKey using the provided deterministic RSA keys.
            PGPSecretKey pgpSecretKey = generatePGPSecretKey(pgpKeyPair, userId, password);

            byte[] rawGPGPrivateKey = GPGKeyUtils.getArmoredGPGPrivateKey(pgpSecretKey);
            byte[] rawGPGPublicKey = GPGKeyUtils.getArmoredGPGPublicKey(pgpSecretKey);

            return new GPGKeyPair(rawGPGPublicKey, rawGPGPrivateKey);
        } catch (PGPException e) {
            throw new RuntimeException("Error generating PGPPublicKey from provided RSA public key");
        }
    }

    private PGPSecretKey generatePGPSecretKey(
            PGPKeyPair pgpKeyPair, String userId, String password) {
        try {
            return new PGPSecretKey(
                    PGPSignature.DEFAULT_CERTIFICATION,
                    pgpKeyPair,
                    userId,
                    generateGeneralSubpacketGenerator().generate(),
                    null,
                    new BcPGPContentSignerBuilder(PGPPublicKey.RSA_SIGN, HashAlgorithmTags.SHA256),
                    new BcPBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256).build(password.toCharArray())
            );
        } catch (PGPException e) {
            throw new RuntimeException("Error creating deterministic PGPSecretKey");
        }
    }

    private GPGKeyPair getGPGKeyPair(PGPSecretKeyRing secretKeyRing) {
        PGPSecretKey secretKey = secretKeyRing.getSecretKey();
        return new GPGKeyPair(GPGKeyUtils.getArmoredGPGPublicKey(secretKey), GPGKeyUtils.getArmoredGPGPrivateKey(secretKey));
    }

    public static class GPGKeyPair {
        private byte[] publicKey;
        private byte[] privateKey;

        GPGKeyPair(byte[] publicKey, byte[] privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public byte[] getPublicKey() {
            return publicKey;
        }

        public byte[] getPrivateKey() {
            return privateKey;
        }
    }
}
