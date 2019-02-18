package com.bsycorp.kees.data;

import com.bsycorp.kees.models.SecretTypeEnum;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import org.bouncycastle.util.io.Streams;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DeterministicDataProviderTest {

    private static final DataProvider DATA_PROVIDER = new DeterministicDataProvider();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    @Test
    public void shouldCreateDeterministicRSA() {
        String expectedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgQI70at0oy5nLrrdJloJPjJMlPkSiPzK+djmCkJkyg5AExEIf2wU/+DoLb8vVJvl6sHq+HTA0ViWoJqbEet8nr6PLI+aSNDhAHgV35RoHDBERSm42dEZswEJ2ZvZhfMuYJLDFER9qO9f+qWUpWR8q5fp8LC2M0ofLcUC7yStBDkzKjyfAqehXG+bHyg90HWZkm8iCZ4TWDndJdB0IBBEP7o3M9wuH8kJiaM1L/i1dl761uoVxyf5ANhec9KvT5L9o49ZuxD8rfjHHa23YvhNFF69MOag+/SwOspVeLZynzeAQ7zioYJXjZrNwvLCdRw3lYF20egTbM2EFOqVGX5alQIDAQAB";
        String expectedPrivateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCBAjvRq3SjLmcuut0mWgk+MkyU+RKI/Mr52OYKQmTKDkATEQh/bBT/4Ogtvy9Um+Xqwer4dMDRWJagmpsR63yevo8sj5pI0OEAeBXflGgcMERFKbjZ0RmzAQnZm9mF8y5gksMURH2o71/6pZSlZHyrl+nwsLYzSh8txQLvJK0EOTMqPJ8Cp6Fcb5sfKD3QdZmSbyIJnhNYOd0l0HQgEEQ/ujcz3C4fyQmJozUv+LV2XvrW6hXHJ/kA2F5z0q9Pkv2jj1m7EPyt+Mcdrbdi+E0UXr0w5qD79LA6ylV4tnKfN4BDvOKhgleNms3C8sJ1HDeVgXbR6BNszYQU6pUZflqVAgMBAAECggEAOhsVclR1TmJCGywTG4kGDLt+/sJIdObXTT1CL3DEELXmajAL0ciOlMlqCeDIoqUtI1WATbPYfPIXtfKs0Z9tG9rchceQSCe8kAeGYpNnaPrcZQJrvb+Oga+ADkFB8jEbvGed8ez/ZC2c3zng/7WI6Yic18nf5q4F6QmJTskIHJQ4gDSsbBjwwFN6VJc7RBHlJViBoUgMpgE3ksHa27NM7DbM+y9tC6rx/CFBn2g0CH0nRATj2fOye8IvozDmOd+Zgp4XbR8MrE8e0rgCxjiLTQlN5PeMgqZoZRLAfPTcWBVFE3aHClBpKIxT+kRyKi9S7TrgIr63O/UXEe5PmAjjgQKBgQDZU3Uvyj+/l/qTaxbyzFdc6zPBt/KAlKd+bwsfyvjkM7Z22r9n46pUxuAcE/2AyoZDZeDqWto9498zdgEHptfEO4qdwKRAYnjT4kb1VU1O/Hs2YmkGmWCCxovPbO/FsRYikNPZ1fOs0j9jobc5rKWdjnOPFwviFp/y1mRWHB6JRQKBgQCX92DL7K54WUc7kL0m6LyxiGUZxv82kq0npIdJ16GK9qo4vgYr3n1lud7aUDoa1opeqGsM8KKdgmcx2Sng3+z+Tw/en6t1RXK0r6SHeeQ4EKr5AYr2qR1IkcGN3wxOSSl9rWXdTZUxfzU3Lm2+zvACbKXSPgkeCvLK20wKCAmZEQKBgDxKJ9djLz4ypkQhUFN5Fr9jTI9wPWkoVDMCET73qwZ7xiHA25qxkP5F0cRYU1pYVNj7uWKqY6MJxVDcarq5sV/x+Kl82NQ3vTdirba/vFuuNWxH7sTy5dBBzmVz3iykzPQ3412qOhh2lzlHkrIAUE2eSDao+RX2mtbeXSV6VYpVAoGAMdz55e/DT7n7pY/YgOvc/mPCyLKDC2UVa+yQd6pJV0+YiwXPAJYAj2BtvzST3DqJLIYWmihbM1OWiMS4+RCAsvE+Q84gdFpVSPRZCBr2x26wqwPWlRjogudQmzyUzRs0gghjZDoQui0DSRfy6qj8F8+OmW3BkBoHkIhkauY4QAECgYByEemoH0FKyCJsr5iWxQTTElPh+z6Ts6ke+21QlURDWJJM39cWG4E1BqdZVoCy9FDpB9paVnFK/612vyVBWjt3p4nvjgsNaI3WnFBS5iA6+llkhgEuhVWH5B741qi+pn9zxu6s/coYnJyoEDVSHUL4a+HOPLniIQMcGBwe/v7lOw==";

        assertEquals(expectedPublicKey, DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.RSA, "app.key.v1_public", 2048, null)[0]);
        assertEquals(expectedPublicKey, DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.RSA, "app.key.v1_public", 2048, null)[0]);
        assertEquals(expectedPublicKey, DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.RSA, "app.key.v1_public", 2048, null)[0]);

        assertEquals(expectedPrivateKey, DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.RSA, "app.key.v1_private", 2048, null)[1]);
        assertEquals(expectedPrivateKey, DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.RSA, "app.key.v1_private", 2048, null)[1]);
        assertEquals(expectedPrivateKey, DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.RSA, "app.key.v1_private", 2048, null)[1]);
    }

    /**
     * We need to test that for the same annotation parameters, the generated key can be used for encryption/decryption/signing.
     *
     * We generate 3 key pairs for the test.
     * 1. A signer using a unique annotation name.
     * 2. Two decrypters, using identical annotations.
     *
     * The raw input is encrypted using recipient1's public key and signed with signer's private key.
     *
     * The encrypted file is then decrypted with recipient2's private key.
     *
     * Note: UserId must be in form of "USER <EMAIL>".
     */
    @Test
    public void shouldCreateDeterministicGPG() throws Exception {
        final String signerUserId = "signer<signer@email.com>";
        final String decrypterUserId = "decrypter<decrypter@email.com>";
        final Object[] signer = DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.GPG, "app.key.signer", 2048, signerUserId);

        byte[] signerPublic = DECODER.decode((String) signer[0]);
        byte[] signerPrivate = DECODER.decode((String) signer[1]);
        final String signerPassword = new String(DECODER.decode((String) signer[2]));

        final Object[] decrypter1 = DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.GPG, "app.key.decrypter", 2048, decrypterUserId);
        byte[] decrypterPublic1 = DECODER.decode((String) decrypter1[0]);
        byte[] decrypterPrivate1 = DECODER.decode((String) decrypter1[1]);
        final String decrypterPassword1 = new String(DECODER.decode((String) decrypter1[2]));

        final Object[] decrypter2 = DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.GPG, "app.key.decrypter", 2048, decrypterUserId);
        byte[] decrypterPublic2 = DECODER.decode((String) decrypter2[0]);
        byte[] decrypterPrivate2 = DECODER.decode((String) decrypter2[1]);
        final String decrypterPassword2 = new String(DECODER.decode((String) decrypter2[2]));

        Assert.assertThat(decrypterPrivate1, IsNot.not(IsEqual.equalTo(decrypterPrivate2)));
        Assert.assertEquals(decrypterPassword1, decrypterPassword2);

        /*
         * Encrypt with decrypterPublic1 and sign with signer.
         */
        CryptoTestUtils.EncryptionService encryptionService = new CryptoTestUtils.EncryptionService(
                decrypterPublic1, signerPrivate, signerPublic, signerPassword);

        /*
         * Decrypt with decrypterPublic2 and verify signature.
         */
        CryptoTestUtils.DecryptionService decryptionService = new CryptoTestUtils.DecryptionService(
                decrypterPassword2, decrypterPrivate2, decrypterPublic2, signerUserId, signerPublic
        );

        final byte[] raw = "This is a test string".getBytes();

        final byte[] encrypted = encryptionService.encrypt(new ByteArrayInputStream(raw), decrypterUserId, signerUserId);

        final byte[] decrypted = decryptionService.decryptAndVerify(new ByteArrayInputStream(encrypted));

        Assert.assertArrayEquals(decrypted, raw);
    }

    @Test
    public void shouldCreateDeterministicPassword() {
        String password = new String(DATA_PROVIDER.generateRaw(SecretTypeEnum.PASSWORD, "app.service.blah", 128));
        assertEquals("8ekD64eU9hDqA5kz", password);
        assertEquals(16, password.length());

        password = new String(DATA_PROVIDER.generateRaw(SecretTypeEnum.PASSWORD, "app.service.blah", 128));
        assertEquals("8ekD64eU9hDqA5kz", password);
        assertEquals(16, password.length());

        password = new String(DATA_PROVIDER.generateRaw(SecretTypeEnum.PASSWORD, "app.service.blah", 128));
        assertEquals("8ekD64eU9hDqA5kz", password);
        assertEquals(16, password.length());
    }

    @Test
    public void shouldCreateDeterministicRandom() {
        String encodedRandom = new String(DATA_PROVIDER.generateBase64Encoded(SecretTypeEnum.RANDOM, "app.service.blah", 288));
        assertEquals("JuTNTwzlgOWVLXk6aG8LQiZ6JXuGlPpaXvEKJkk7jDjsrJpC", encodedRandom);

        encodedRandom = new String(DATA_PROVIDER.generateBase64Encoded(SecretTypeEnum.RANDOM, "app.service.blah", 288));
        assertEquals("JuTNTwzlgOWVLXk6aG8LQiZ6JXuGlPpaXvEKJkk7jDjsrJpC", encodedRandom);

        encodedRandom = new String(DATA_PROVIDER.generateBase64Encoded(SecretTypeEnum.RANDOM, "app.service.blah", 288));
        assertEquals("JuTNTwzlgOWVLXk6aG8LQiZ6JXuGlPpaXvEKJkk7jDjsrJpC", encodedRandom);
    }
}