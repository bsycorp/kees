package com.bsycorp.kees.gpg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPSecretKey;

final class GPGKeyUtils {

    private GPGKeyUtils() {
    }

    static byte[] getArmoredGPGPrivateKey(PGPSecretKey secretKey) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ArmoredOutputStream armored = new ArmoredOutputStream(out);
            secretKey.encode(armored);
            armored.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error getting armored GPG private key");
        }
    }

    static byte[] getArmoredGPGPublicKey(PGPSecretKey secretKey) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ArmoredOutputStream armoredOutputStream = new ArmoredOutputStream(os);
            secretKey.getPublicKey().encode(armoredOutputStream);
            armoredOutputStream.close();
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error getting armored GPG public key");
        }
    }

    /**
     * Constructs a PublicKey object from an RSA public key byte array.
     *
     * @param key public key in byte[]
     * @return RSA PublicKey
     */
    static PublicKey getRSAPublicKey(byte[] key) {
        try {
            final KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(new X509EncodedKeySpec(key));

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error constructing PublicKey from byte array");
        }
    }

    /*
     * Constructs a PrivateKey object from an RSA private key byte array.
     *
     * @param key private key in byte[]
     * @return RSA PrivateKey
     */
    static PrivateKey getRSAPrivateKey(byte[] key) {
        try {
            final KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(new PKCS8EncodedKeySpec(key));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error constructing PrivateKey from byte array");
        }
    }
}
