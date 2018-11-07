package com.bsycorp.kees.data;

import com.bsycorp.kees.models.SecretTypeEnum;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class RandomDataProviderTest {

    DataProvider dataProvider = new RandomDataProvider();

    @Test
    public void shouldCreateDeterministicRSA() throws Exception {
        String publicKey1 = dataProvider.generatePairedBase64Encoded(SecretTypeEnum.RSA, "app.key.v1_public", 2048)[0];
        String publicKey2 = dataProvider.generatePairedBase64Encoded(SecretTypeEnum.RSA, "app.key.v1_public", 2048)[0];
        assertNotEquals(publicKey1, publicKey2);

        String privateKey1 = dataProvider.generatePairedBase64Encoded(SecretTypeEnum.RSA, "app.key.v1_private", 2048)[1];
        String privateKey2 = dataProvider.generatePairedBase64Encoded(SecretTypeEnum.RSA, "app.key.v1_private", 2048)[1];
        assertNotEquals(privateKey1, privateKey2);
    }

    @Test
    public void shouldCreateDeterministicPassword() throws Exception {
        String password1 = new String(dataProvider.generateRaw(SecretTypeEnum.PASSWORD, "app.service.blah", 128));
        String password2 = new String(dataProvider.generateRaw(SecretTypeEnum.PASSWORD, "app.service.blah", 128));
        assertNotEquals(password1, password2);
    }

    @Test
    public void shouldCreateDeterministicRandom() throws Exception {
        String encodedRandom1 = new String(dataProvider.generateBase64Encoded(SecretTypeEnum.RANDOM, "app.service.blah", 288));
        String encodedRandom2 = new String(dataProvider.generateBase64Encoded(SecretTypeEnum.RANDOM, "app.service.blah", 288));
        assertNotEquals(encodedRandom1, encodedRandom2);
    }
}