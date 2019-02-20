package com.bsycorp.kees.data;

import com.bsycorp.kees.models.SecretTypeEnum;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class RandomDataProviderTest {

    private static final DataProvider DATA_PROVIDER = new RandomDataProvider();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    @Test
    public void shouldCreateRandomGPG() throws Exception {
        final String signerUserId = "signer<signer@email.com>";
        final String decrypterUserId = "decrypter<decrypter@email.com>";

        final Object[] signer = DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.GPG, "app.key.signer", 2048, signerUserId);
        String signerPublic = (String)signer[0];
        String signerPrivate =(String)signer[1];
        String signerPassword = new String(DECODER.decode((String) signer[2]));

        final Object[] decrypter1 = DATA_PROVIDER.generatePairedBase64Encoded(SecretTypeEnum.GPG, "app.key.decrypter", 2048, decrypterUserId);
        String decrypterPublic = (String)decrypter1[0];
        String decrypterPrivate = (String)decrypter1[1];
        String decrypterPassword = new String(DECODER.decode((String) decrypter1[2]));

        final byte[] raw = "This is a test string".getBytes();

        CryptoTestUtils.EncryptionService encryptionService = new CryptoTestUtils.EncryptionService(
                DECODER.decode(decrypterPublic), DECODER.decode(signerPrivate), DECODER.decode(signerPublic), signerPassword);

        byte[] encrypted = encryptionService.encrypt(new ByteArrayInputStream(raw), decrypterUserId, signerUserId);

        CryptoTestUtils.DecryptionService decryptionService = new CryptoTestUtils.DecryptionService(
                decrypterPassword, DECODER.decode(decrypterPrivate), DECODER.decode(decrypterPublic), signerUserId, DECODER.decode(signerPublic)
        );

        byte[] result = decryptionService.decryptAndVerify(new ByteArrayInputStream(encrypted));

        Assert.assertArrayEquals(result, raw);
    }

    @Test
    public void shouldCreateDeterministicPassword() throws Exception {
        String password1 = new String(DATA_PROVIDER.generateRaw(SecretTypeEnum.PASSWORD, "app.service.blah", 128));
        String password2 = new String(DATA_PROVIDER.generateRaw(SecretTypeEnum.PASSWORD, "app.service.blah", 128));
        assertNotEquals(password1, password2);
    }

    @Test
    public void shouldCreateDeterministicRandom() throws Exception {
        String encodedRandom1 = new String(DATA_PROVIDER.generateBase64Encoded(SecretTypeEnum.RANDOM, "app.service.blah", 288));
        String encodedRandom2 = new String(DATA_PROVIDER.generateBase64Encoded(SecretTypeEnum.RANDOM, "app.service.blah", 288));
        assertNotEquals(encodedRandom1, encodedRandom2);
    }
}