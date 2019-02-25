package com.bsycorp.kees.data;

import com.bsycorp.kees.models.SecretTypeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DeterministicDataProviderTest {

    private static final DataProvider DATA_PROVIDER = new DeterministicDataProvider();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    @Test
    public void shouldCreateDeterministicRSA() {
        String expectedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAz73Y8E9KvvD5wLVwdftSmToymFrtg9uFLJlSakzpULSfxw0ZYxsJjJBdhil10qozwVZPkGaHZpaC1p9dlaaUedHr1pD23KsNT57dwm2NYW6kAZITVNKciddALFAfCW5E8JrQh8zKwHu96N3UMw/HOANOQ2ZxpI1PMONESaIy9bM/2v8p6TAlECe1S86Vha6mLGz8kVmqNaRDAG6kmfQj4dH5RWPL1XMaIxd/eeL+pV+nOHMjU0SJaPWhbaPA9jJEJFm0LgWGGzyFOxtmMKFKPHR9C8SpNJ/CQ+qWi4FWMHOvvxvsgW4x2i+weuylWV2NKxCwH2xZdnHKwHGcnU8DRwIDAQAB";
        String expectedPrivateKey = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDPvdjwT0q+8PnAtXB1+1KZOjKYWu2D24UsmVJqTOlQtJ/HDRljGwmMkF2GKXXSqjPBVk+QZodmloLWn12VppR50evWkPbcqw1Pnt3CbY1hbqQBkhNU0pyJ10AsUB8JbkTwmtCHzMrAe73o3dQzD8c4A05DZnGkjU8w40RJojL1sz/a/ynpMCUQJ7VLzpWFrqYsbPyRWao1pEMAbqSZ9CPh0flFY8vVcxojF3954v6lX6c4cyNTRIlo9aFto8D2MkQkWbQuBYYbPIU7G2YwoUo8dH0LxKk0n8JD6paLgVYwc6+/G+yBbjHaL7B67KVZXY0rELAfbFl2ccrAcZydTwNHAgMBAAECggEAWCmkuI+eFBymkZ8uxBNNwZOOR1RHem8ePIgxDsXnOoX9TPeFL6cYzVuzJS3RP/9ViZZ8m7a3fdUgX78wc0NHvc9V/DB3Y70AICs2x73Ag3n2Bmic5aGgJ2t/Y475LQJsJYQ+PXOvFDMvrHUACk+G54GXpEkEr6X81q2MnIdGgzi0Fhj+nJzg/XiPqzpMcsebdYDupSXzixjvZBA9UKhJxVvDfMLFqKULyaoQ+iGGUiSrvsVZkI3l7FD1LpsZ0UkAYHOe+JKSZAoCfc7eNrEWntwgSzgEc7R2Z/9JB8pSTv5Mkn3+tzfOP0fphOYmEhLfxlKzu/Erhql3W9bccVVsUQKBgQD2R8wa7nM2p7rFjXnc5dZEej3vTqccWc+rNL6U3FzgTSjVCh4cIWOaFUujHpzNKMFODkrTjoTe0cItC+vwIcg1WoJv89Mz1RMCQqV2/dxWJXQ4mV6OzZurSWa2ZKptG4KKXkT7u2PNxWCz88DbAlV19nrg0x7LVlCB4rYpxVPTPwKBgQDX8K/j6n8/XbAumg6WUbMxpf5gwJRgQDVc3pslUQUv9dgj55DO5vaUgjIUAQx6XqY6bWdGe3WmZDgk7LzUBSTHpzkyWco/lWq84GzEQ6D/nTiM6MzrJAcUhEwuEM9vhss/H+zrJdWeRS9d5wAZBDrfUJ4ARw6vcAFYbYeImdW1+QKBgQCYoky8EDZ1lCRsFU+GeSd/jyddbiihqIPNPsYy6hPhq9B3oGqi0oqxTytucCWL/Qs4viDf1r9AfU3Tr0TNsZIshui6S6oEwLSkPPvhsFnjRhkujtcMuB8XXEl9FwyMzHTuHpiwTyX+vKo/PP20flDK6DSlrBK0wMzqgsCVSMe37wKBgQCp8Ij73Pf3bkvQ4PzJ39IeKHxguC4M8XsNc0K1w2VJsThASWT0717u0OeIRqsDQqmfIao9FbwpDoYAyS5xzPp9BWVF8tPv5i7yJcxzSKXThG+UtUFPbDMGOneZaTFWm8YoD3/sLwJGZDw6siiph2KtjExL+5/bAVKNvOUE48wEQQKBgQCW8gozAlnRFvXRDclF10PBqaPUowO/SR1gl8P9ZvdoisNobwonttKhxWpFgDig+7rhnc1PHaPdr16evC3VFC/XZFZMTj9uQ7TVdftf/3T6s9YyMbNnFzIp8Pbg5/4WIBJ220a8rY+chqEe66dNBwprjRW2UMtxo6jsR2uvrkputg==";

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
        final Object[] signer = DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.GPG, "app.key.signer", 4096, signerUserId);

        byte[] signerPublic = DECODER.decode((String) signer[0]);
        byte[] signerPrivate = DECODER.decode((String) signer[1]);
        final String signerPassword = new String(DECODER.decode((String) signer[3]));

        final Object[] decrypter1 = DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.GPG, "app.key.decrypter", 4096, decrypterUserId);
        byte[] decrypterPublic1 = DECODER.decode((String) decrypter1[0]);
        byte[] decrypterPrivate1 = DECODER.decode((String) decrypter1[1]);
        final String decrypterUserId1 = new String(DECODER.decode((String) decrypter1[2]));
        final String decrypterPassword1 = new String(DECODER.decode((String) decrypter1[3]));

        final Object[] decrypter2 = DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.GPG, "app.key.decrypter", 4096, decrypterUserId);
        byte[] decrypterPublic2 = DECODER.decode((String) decrypter2[0]);
        byte[] decrypterPrivate2 = DECODER.decode((String) decrypter2[1]);
        final String decrypterUserId2 = new String(DECODER.decode((String) decrypter2[2]));
        final String decrypterPassword2 = new String(DECODER.decode((String) decrypter2[3]));

        Assert.assertNotEquals(decrypterPrivate1, decrypterPrivate2);
        Assert.assertEquals(decrypterUserId1, decrypterUserId2);
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
                decrypterPassword2, decrypterPrivate2, decrypterPublic2, signerUserId, signerPublic);

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

    @Ignore
    @Test
    public void generateDeterministicRSAKeys() throws IOException {
        final int numberOfKeyPairs = 1000;

        List<String[]> keyPairs = new ArrayList<>();

        for (int i= 0; i < numberOfKeyPairs; i++) {
            String[] keyPair = new RandomDataProvider().generatePairedBase64Encoded(SecretTypeEnum.RSA, "app.key.v1_public", 2048, null);
            keyPairs.add(keyPair);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(new File(numberOfKeyPairs + "_deterministic_rsa_keys.json"), keyPairs);
    }
}