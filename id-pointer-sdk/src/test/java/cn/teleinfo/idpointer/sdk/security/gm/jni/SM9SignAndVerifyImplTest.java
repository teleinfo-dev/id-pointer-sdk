package cn.teleinfo.idpointer.sdk.security.gm.jni;

import cn.teleinfo.idpointer.sdk.security.gm.SM9IdPrivateKey;
import cn.teleinfo.idpointer.sdk.security.gm.SM9KeyConverter;
import cn.teleinfo.idpointer.sdk.security.gm.SM9SignMasterPublicKey;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SM9SignAndVerifyImplTest {

    private String signMpkPem = "-----BEGIN SM9 SIGN MASTER PUBLIC KEY-----\n" +
            "MIGFA4GCAARoa1Vks+x9Kui494YlzfzCDYmRmnCiJk3/Rt/w+6807SrAogeQBqwd\n" +
            "h0isx83JRqSM+WGCoG7Pd239EZs5tiBsfqnawABPV9u9Q6/chVij4eAmN9K/0EVz\n" +
            "OJxQLo3TMtJdWU4/SXHgjRye070SZliZRGL7ivdrLOj2150hwJ1Flw==\n" +
            "-----END SM9 SIGN MASTER PUBLIC KEY-----";

    private String userSm9PrivateKeyPem ="-----BEGIN ENCRYPTED SM9 SIGN PRIVATE KEY-----\n" +
            "MIIBVjBhBgkqhkiG9w0BBQ0wVDA0BgkqhkiG9w0BBQwwJwQQuviQseTmD3q+fJMk\n" +
            "ZNGCDwIDAQAAAgEQMAsGCSqBHM9VAYMRAjAcBggqgRzPVQFoAgQQ2L38kK+ArJ6e\n" +
            "9LVyhohh+gSB8OUYpr0nuzwhsftQBOk+x1hqBXB1pIijY8br5cfjSE597GxrvBN4\n" +
            "kdEzYoTOJ3veZ15y75M2bUVycTwwIKZBXhI/G893+4TV6lktnWDCkzY1U6x+nq4N\n" +
            "/q1+d/S7ObDQisxaW9LwFTjm6EYA/zzwa+mJPIzWKSkrt332AAlS9295CZOSy6oW\n" +
            "Sd26bE0eaU8eGfrU+uahMcoxxFaBomheodBLlZg9NH2AHEEfidNlumakpga4zEUp\n" +
            "Fa/LBR5vFm/2V2tf6eCnypv9pwx3VLzyY6HAyu23+6eE2Gh+8VWNb8w8Vieo6716\n" +
            "gzsp84c6dnlstA==\n" +
            "-----END ENCRYPTED SM9 SIGN PRIVATE KEY-----";

    SM9SignAndVerifyImpl sm9SignAndVerify = new SM9SignAndVerifyImpl();
    @Test
    void sign() throws IOException {
        SM9SignMasterPublicKey sm9SignMasterPublicKey = SM9KeyConverter.instance.signMasterPublicKeyFromPem(signMpkPem);
        SM9IdPrivateKey sm9IdPrivateKey = SM9KeyConverter.instance.idPrivateKeyFromPem(userSm9PrivateKeyPem, "123456");

        File file = new File("/Users/bluepoint/Downloads/orign.txt");
        byte[] toSignData = FileUtils.readFileToByteArray(file);
        System.out.println(Hex.encodeHexString(toSignData));

        //byte[] sign = sm9SignAndVerify.sign(sm9IdPrivateKey, toSignData);
        //System.out.println(Hex.encodeHexString(sign));

        File file1 = new File("/Users/bluepoint/Downloads/sign.txt");
        byte[] bytes = FileUtils.readFileToByteArray(file1);
        System.out.println(Hex.encodeHexString(bytes));

        //System.out.println(Hex.encodeHexString(sign).equals(Hex.encodeHexString(bytes)));

        boolean verify = sm9SignAndVerify.verify(sm9SignMasterPublicKey, "Alice", toSignData, bytes);
        System.out.println(verify);
    }

    @Test
    void verify() {
    }
}