package cn.teleinfo.idpointer.sdk.util;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;


class EncryptionUtilsTest {
    @Test
    void test() throws NoSuchAlgorithmException {

        KeyPair keyPair = EncryptionUtils.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String publicKeyPem = KeyConverter.toX509Pem(publicKey);
        System.out.println(publicKeyPem);

        String prvPem = KeyConverter.toPkcs8UnencryptedPem(privateKey);
        System.out.println(prvPem);

    }
}