package cn.teleinfo.idpointer.sdk.client;

import cn.hutool.crypto.SecureUtil;
import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.security.gm.SM9IdPrivateKey;
import cn.teleinfo.idpointer.sdk.security.gm.SM9IdPublicKey;
import cn.teleinfo.idpointer.sdk.security.gm.SM9KeyConverter;
import cn.teleinfo.idpointer.sdk.util.KeyConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 *
 */
class IDClientSm9UserTest {

    private static IDClient idClient;
    private String signMpkPem = "-----BEGIN SM9 SIGN MASTER PUBLIC KEY-----\n" +
            "MIGFA4GCAARoa1Vks+x9Kui494YlzfzCDYmRmnCiJk3/Rt/w+6807SrAogeQBqwd\n" +
            "h0isx83JRqSM+WGCoG7Pd239EZs5tiBsfqnawABPV9u9Q6/chVij4eAmN9K/0EVz\n" +
            "OJxQLo3TMtJdWU4/SXHgjRye070SZliZRGL7ivdrLOj2150hwJ1Flw==\n" +
            "-----END SM9 SIGN MASTER PUBLIC KEY-----";

    private String userSm9PrivateKeyPem = "-----BEGIN ENCRYPTED SM9 SIGN PRIVATE KEY-----\n" +
            "MIIBVjBhBgkqhkiG9w0BBQ0wVDA0BgkqhkiG9w0BBQwwJwQQuviQseTmD3q+fJMk\n" +
            "ZNGCDwIDAQAAAgEQMAsGCSqBHM9VAYMRAjAcBggqgRzPVQFoAgQQ2L38kK+ArJ6e\n" +
            "9LVyhohh+gSB8OUYpr0nuzwhsftQBOk+x1hqBXB1pIijY8br5cfjSE597GxrvBN4\n" +
            "kdEzYoTOJ3veZ15y75M2bUVycTwwIKZBXhI/G893+4TV6lktnWDCkzY1U6x+nq4N\n" +
            "/q1+d/S7ObDQisxaW9LwFTjm6EYA/zzwa+mJPIzWKSkrt332AAlS9295CZOSy6oW\n" +
            "Sd26bE0eaU8eGfrU+uahMcoxxFaBomheodBLlZg9NH2AHEEfidNlumakpga4zEUp\n" +
            "Fa/LBR5vFm/2V2tf6eCnypv9pwx3VLzyY6HAyu23+6eE2Gh+8VWNb8w8Vieo6716\n" +
            "gzsp84c6dnlstA==\n" +
            "-----END ENCRYPTED SM9 SIGN PRIVATE KEY-----";

    private String sm9UserId = "Alice";

    private final static String adminPrivateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCpCFImZ398hp8+mjjSighZiaUN\n" +
            "PCEUg/nRAJFEJAtR6ccXZ/lVjQ1JKfGZtRBeRDum88yEftoFUvoGmyh82TB2xqmnlnCHBc3HuPpS\n" +
            "dBEY9m+kIrcPFyevqttyheNsJKTEM8N3H+rA1JYhD/eFMk/0DcRn2vgIgSgm3Lm3IZYcw9ghqqsu\n" +
            "29ltpitFuaWk4YHsAhZEssgjxtEU1xJnao4aEdDQebxrE8qVW9YEjNWK6ce7tnoon4R/xFVgQm+C\n" +
            "Y8cfz1vCMzrhDTxw3rZpfPHXfZHJlopJprxshjJ1P5WolsdPrLWOrZMQteAwaef7D1pQwmADN37o\n" +
            "NnchJEPScvdTAgMBAAECggEAJNK4Or+PSUx2gofMWylQ2lifCTwRJMv/byWFf+euQ6D/Cz1OVvHM\n" +
            "dCTcChUkxLRunlc8NZ0A1+oUp73DWzdhVG7A5M5kIzYDdz/34GCCnQKKnaImaPYtYhqBhfhe698r\n" +
            "ucZhDaF5XZqyPe+hh3XHby5UnB9aq1efrJ+nIZsCfa9dWeZnsFIf1THyuPn6H+yfv1GH3BWtvnpN\n" +
            "92uOBSgnrpVnJJ7WZDHmytjIj/CQhb5pKyXImaOOtOAvQHc8ny+J1jTQLdWinUGm1Zsrzc9IN1zA\n" +
            "jiOXW+J/JBVpRpMWtAkeCeT2aU53RF83gqFC4riLaaVxYEcyKWVs5JQ3IyLq8QKBgQDmXtX4Ziq5\n" +
            "9IIzPgi8nbRlZ7HUawyX3YulBNf6G11r1wYlmROx9HxCbgGT36+mGM7lD6hRborArhGQ6UCX7puf\n" +
            "6X0cavcgZMmuLqUVuRwF+HMA1EONvpoNgHR2+E9RsmCI+kTqy8BktQkhwKUdL5/yh4KdQLlZneUU\n" +
            "OzXAchkXawKBgQC71oX9Kpkg631yGu+z3VR1L7XZyTJkxfhjq9fDtD6uDmGCVVyOISnm/rXzKcZZ\n" +
            "2Mv0fEUs/KNPRzg5ocODOPoJYTNPRQ5fG+qLfiHcoegjWx+56h1dM0e6um0xD7uLJTZLw4BaGLXV\n" +
            "/1Jfw5T/OZzylgp4OdAZn+RKEFGIbTThuQKBgHuJ9gL8fwMz3TKvnK3RgLE0t4erVJiIRV/cRhoo\n" +
            "3KN9Lx2whoBTFOPm7E+pkB9phGIQUHpC5oPHlUH55BrV6X0LEH3R2u7zPbh76SnKF/4Xq0yRiByW\n" +
            "TzTYYxx9ssOj/eLXG2gyld0rvFbuYV1Scdr00pWT5RHq+7MqwXuPdGc7AoGAI93oo9jYeIK+52Th\n" +
            "VHT0xZOgo79ZUgjDTzTm9EWcmlvAnsRx8em/OI52a8IUT9+nwj1gcCClhmPRBqAu1wWwKM5Yn9BN\n" +
            "2DdaZ9xj1t9LlkS2ICfmB3/dOHiUlQOuCfnHDlx8S7fgMMaaEhMkhj+YCnI/+YkAhYQUcVgAWY6Q\n" +
            "xHECgYEAx2yxqhNRrZIqRQvLDWfHu/tvlaDcISBX/HuWE//sqlUG05fFY7DI3r1Va/S5DKf6mueA\n" +
            "ojq/oxjQrsFl3gfOwv9ttjYqfd7vSpojyNbZDdHYtzOkdyrOvpsWlYYn+4ef/qdCHpthsyx4Tv8H\n" +
            "lg/0xZ8oZVr5aJlFPR7uJAQrHdA=\n" +
            "-----END PRIVATE KEY-----";

    @BeforeAll
    public static void setUp() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.3.2", 2641);

        IDClientFactory idClientFactory = GlobalIdClientFactory.getIdClientFactory();

        PrivateKey privateKey = KeyConverter.fromPkcs8Pem(adminPrivateKeyPem);
        idClient = idClientFactory.newInstance(inetSocketAddress, "88.888.1024/admin", 300, privateKey);
    }

    @Disabled
    @Test
    void sm9UserInitTest() throws Exception {
        ValueHelper valueHelper = ValueHelper.getInstance();
        PublicKey publicKey = new SM9IdPublicKey(sm9UserId);
        HandleValue handleValue = valueHelper.newPublicKeyValue(300, publicKey);

        idClient.deleteHandle("88.888.1024/sm9user");
        idClient.createHandle("88.888.1024/sm9user", new HandleValue[]{handleValue});

    }

    @Test
    void sm9UserLogin() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.3.2", 2641);

        IDClientFactory idClientFactory = GlobalIdClientFactory.getIdClientFactory();

        SM9IdPrivateKey sm9IdPrivateKey = SM9KeyConverter.instance.idPrivateKeyFromPem(userSm9PrivateKeyPem, "123456");
        IDClient idClient1 = idClientFactory.newInstance(inetSocketAddress, "88.888.1024/sm9user", 300, sm9IdPrivateKey);

        idClient1.resolveHandle("88.888.1024/sm9user");
    }


}