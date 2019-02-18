package com.bsycorp.kees.data;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.util.io.Streams;

public final class CryptoTestUtils {

    private CryptoTestUtils() { }

    public static class EncryptionService {

        private static final int BUFFER_SIZE = 1048576 * 2;
        private InMemoryKeyring keyring;

        public EncryptionService(
                byte[] encryptionPubKey, byte[] signingPrivateKey, byte[] signingPubKey, String privateKeyPassphrase)
                throws PGPException, IOException {
            keyring = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withPassword(privateKeyPassphrase));
            keyring.addSecretKey(signingPrivateKey);
            keyring.addPublicKey(encryptionPubKey);
            keyring.addPublicKey(signingPubKey);

            if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
                Security.addProvider(new BouncyCastleProvider());
            }
        }

        public byte[] encrypt(InputStream inputStream, String recipientName, String signingUser) throws
                IOException, PGPException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException {
            ByteArrayOutputStream result = new ByteArrayOutputStream();

            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(result, BUFFER_SIZE);//2MB buffer, tweaking might be needed
                 OutputStream outputStream = BouncyGPG
                         .encryptToStream()
                         .withConfig(keyring)
                         .withStrongAlgorithms()
                         .toRecipients(recipientName) // "TC_RECIPIENT_NAME_IN_ENCRYPTION_PUB_KEY"
                         .andSignWith(signingUser)//"DP_SENDER_ID_IN_SIGNING_PRIVATE_KEY"
                         .armorAsciiOutput()
                         .andWriteTo(bufferedOutputStream);
            ) {
                Streams.pipeAll(inputStream, outputStream);
            } finally {
                result.close();
            }
            return result.toByteArray();
        }
    }

    public static class DecryptionService {

        private final InMemoryKeyring keyring;
        private final String signingUserId;

        public DecryptionService(
                String tcPassword,
                byte[] tcPrivateKey,
                byte[] tcPublicKey,
                String signingUserId,
                byte[] signingPublicKey) throws IOException, PGPException {
            this.signingUserId = signingUserId;
            keyring = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withPassword(tcPassword));
            keyring.addSecretKey(tcPrivateKey);
            keyring.addPublicKey(tcPublicKey);
            keyring.addPublicKey(signingPublicKey);
            Security.addProvider(new BouncyCastleProvider());
        }

        public InputStream getDecryptedInputStream(InputStream encryptedInputStream) throws PGPException, IOException, NoSuchProviderException {
            return BouncyGPG
                    .decryptAndVerifyStream()
                    .withConfig(keyring)
                    .andRequireSignatureFromAllKeys(signingUserId)
                    .fromEncryptedInputStream(encryptedInputStream);
        }

        public byte[] decryptAndVerify(InputStream encryptedInputStream) throws PGPException, NoSuchProviderException, IOException {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();

            try (
                    BufferedOutputStream bufferedOut = new BufferedOutputStream(output);
                    InputStream plaintextStream = getDecryptedInputStream(encryptedInputStream);
            ) {
                Streams.pipeAll(plaintextStream, bufferedOut);
            } finally {
                output.close();
            }
            return output.toByteArray();
        }
    }
}
