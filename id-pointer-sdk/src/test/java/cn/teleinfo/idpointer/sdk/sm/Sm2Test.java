package cn.teleinfo.idpointer.sdk.sm;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import cn.teleinfo.idpointer.sdk.util.KeyConverter;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.util.Base64;

public class Sm2Test {
    @Test
    void test1() {
        // 公钥加密，私钥解密
        String text = "我是一段测试aaaa";

        KeyPair pair = SecureUtil.generateKeyPair("SM2");
        PublicKey aPublic = pair.getPublic();
        byte[] privateKey = pair.getPrivate().getEncoded();
        byte[] publicKey = aPublic.getEncoded();


        SM2 sm2 = SmUtil.sm2(privateKey, publicKey);

        byte[] encrypt = sm2.encrypt(text, KeyType.PublicKey);
        String decryptStr = StrUtil.utf8Str(sm2.decrypt(encrypt, KeyType.PrivateKey));
    }

    @Test
    void test2() {
        // 签名验签
        String content = "我是Hanley.";
        KeyPair pair = SecureUtil.generateKeyPair("SM2");
        final SM2 sm2 = new SM2(pair.getPrivate(), pair.getPublic());


        byte[] sign = sm2.sign(content.getBytes());

        // true
        boolean verify = sm2.verify(content.getBytes(), sign);
    }

    @Test
    void key() throws NoSuchAlgorithmException {
        String content = "我是Hanley.";

        final org.bouncycastle.jce.provider.BouncyCastleProvider provider = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        Security.addProvider(provider);

        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("SM2");
        keyPairGen.initialize(2048);
        KeyPair pair = keyPairGen.generateKeyPair();

        final SM2 sm2 = new SM2(pair.getPrivate(), pair.getPublic());

        byte[] sign = sm2.sign(content.getBytes());

        // true
        boolean verify = sm2.verify(content.getBytes(), sign);
        System.out.println(verify);
    }

    @Test
    void test3() throws Exception {

        KeyPair pair = SecureUtil.generateKeyPair("SM2");

        String pubKeyPem = KeyConverter.toX509Pem(pair.getPublic());

        System.out.println(pubKeyPem);

        PublicKey publicKey = KeyConverter.fromX509Pem(pubKeyPem);

        String pkcs8UnencryptedPem = KeyConverter.toPkcs8UnencryptedPem(pair.getPrivate());

        System.out.println(pkcs8UnencryptedPem);

        PrivateKey privateKey = KeyConverter.fromPkcs8Pem(pkcs8UnencryptedPem);

        System.out.println(privateKey.getAlgorithm());

        //PublicKey aPublic = pair.getPublic();
        //System.out.println(publicKey.getAlgorithm());
        SM2 sm2 = SmUtil.sm2(privateKey, publicKey);

        String text = "我是一段测试aaaa";
        byte[] encryptData = sm2.encrypt(text, KeyType.PublicKey);

        String base64Str = Base64.getMimeEncoder().encodeToString(encryptData);
        System.out.println(base64Str);

        String decryptStr = StrUtil.utf8Str(sm2.decrypt(encryptData, KeyType.PrivateKey));
        System.out.println(decryptStr);


    }

    @Test
    void keyExchange() throws Exception {
        KeyPair pair = SecureUtil.generateKeyPair("SM2");
        String aPubKeyPem = KeyConverter.toX509Pem(pair.getPublic());
        System.out.println(aPubKeyPem);
        PublicKey aPublicKey = KeyConverter.fromX509Pem(aPubKeyPem);
        String aPkcs8UnencryptedPem = KeyConverter.toPkcs8UnencryptedPem(pair.getPrivate());
        System.out.println(aPkcs8UnencryptedPem);

        KeyPair bPair = SecureUtil.generateKeyPair("SM2");
        String bPubKeyPem = KeyConverter.toX509Pem(bPair.getPublic());
        System.out.println(bPubKeyPem);
        PublicKey bPublicKey = KeyConverter.fromX509Pem(aPubKeyPem);
        String bPkcs8UnencryptedPem = KeyConverter.toPkcs8UnencryptedPem(bPair.getPrivate());
        System.out.println(bPkcs8UnencryptedPem);


    }
}
