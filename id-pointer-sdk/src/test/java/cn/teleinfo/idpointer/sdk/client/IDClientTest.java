package cn.teleinfo.idpointer.sdk.client;

import cn.hutool.crypto.SecureUtil;
import cn.teleinfo.idpointer.sdk.core.AuthenticationInfo;
import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.core.PublicKeyAuthenticationInfo;
import cn.teleinfo.idpointer.sdk.core.Util;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.util.KeyConverter;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * -----BEGIN PUBLIC KEY-----
 * MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqQhSJmd/fIafPpo40ooIWYmlDTwhFIP5
 * 0QCRRCQLUenHF2f5VY0NSSnxmbUQXkQ7pvPMhH7aBVL6BpsofNkwdsapp5ZwhwXNx7j6UnQRGPZv
 * pCK3Dxcnr6rbcoXjbCSkxDPDdx/qwNSWIQ/3hTJP9A3EZ9r4CIEoJty5tyGWHMPYIaqrLtvZbaYr
 * RbmlpOGB7AIWRLLII8bRFNcSZ2qOGhHQ0Hm8axPKlVvWBIzViunHu7Z6KJ+Ef8RVYEJvgmPHH89b
 * wjM64Q08cN62aXzx132RyZaKSaa8bIYydT+VqJbHT6y1jq2TELXgMGnn+w9aUMJgAzd+6DZ3ISRD
 * 0nL3UwIDAQAB
 * -----END PUBLIC KEY-----
 * <p>
 * -----BEGIN PRIVATE KEY-----
 * MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCpCFImZ398hp8+mjjSighZiaUN
 * PCEUg/nRAJFEJAtR6ccXZ/lVjQ1JKfGZtRBeRDum88yEftoFUvoGmyh82TB2xqmnlnCHBc3HuPpS
 * dBEY9m+kIrcPFyevqttyheNsJKTEM8N3H+rA1JYhD/eFMk/0DcRn2vgIgSgm3Lm3IZYcw9ghqqsu
 * 29ltpitFuaWk4YHsAhZEssgjxtEU1xJnao4aEdDQebxrE8qVW9YEjNWK6ce7tnoon4R/xFVgQm+C
 * Y8cfz1vCMzrhDTxw3rZpfPHXfZHJlopJprxshjJ1P5WolsdPrLWOrZMQteAwaef7D1pQwmADN37o
 * NnchJEPScvdTAgMBAAECggEAJNK4Or+PSUx2gofMWylQ2lifCTwRJMv/byWFf+euQ6D/Cz1OVvHM
 * dCTcChUkxLRunlc8NZ0A1+oUp73DWzdhVG7A5M5kIzYDdz/34GCCnQKKnaImaPYtYhqBhfhe698r
 * ucZhDaF5XZqyPe+hh3XHby5UnB9aq1efrJ+nIZsCfa9dWeZnsFIf1THyuPn6H+yfv1GH3BWtvnpN
 * 92uOBSgnrpVnJJ7WZDHmytjIj/CQhb5pKyXImaOOtOAvQHc8ny+J1jTQLdWinUGm1Zsrzc9IN1zA
 * jiOXW+J/JBVpRpMWtAkeCeT2aU53RF83gqFC4riLaaVxYEcyKWVs5JQ3IyLq8QKBgQDmXtX4Ziq5
 * 9IIzPgi8nbRlZ7HUawyX3YulBNf6G11r1wYlmROx9HxCbgGT36+mGM7lD6hRborArhGQ6UCX7puf
 * 6X0cavcgZMmuLqUVuRwF+HMA1EONvpoNgHR2+E9RsmCI+kTqy8BktQkhwKUdL5/yh4KdQLlZneUU
 * OzXAchkXawKBgQC71oX9Kpkg631yGu+z3VR1L7XZyTJkxfhjq9fDtD6uDmGCVVyOISnm/rXzKcZZ
 * 2Mv0fEUs/KNPRzg5ocODOPoJYTNPRQ5fG+qLfiHcoegjWx+56h1dM0e6um0xD7uLJTZLw4BaGLXV
 * /1Jfw5T/OZzylgp4OdAZn+RKEFGIbTThuQKBgHuJ9gL8fwMz3TKvnK3RgLE0t4erVJiIRV/cRhoo
 * 3KN9Lx2whoBTFOPm7E+pkB9phGIQUHpC5oPHlUH55BrV6X0LEH3R2u7zPbh76SnKF/4Xq0yRiByW
 * TzTYYxx9ssOj/eLXG2gyld0rvFbuYV1Scdr00pWT5RHq+7MqwXuPdGc7AoGAI93oo9jYeIK+52Th
 * VHT0xZOgo79ZUgjDTzTm9EWcmlvAnsRx8em/OI52a8IUT9+nwj1gcCClhmPRBqAu1wWwKM5Yn9BN
 * 2DdaZ9xj1t9LlkS2ICfmB3/dOHiUlQOuCfnHDlx8S7fgMMaaEhMkhj+YCnI/+YkAhYQUcVgAWY6Q
 * xHECgYEAx2yxqhNRrZIqRQvLDWfHu/tvlaDcISBX/HuWE//sqlUG05fFY7DI3r1Va/S5DKf6mueA
 * ojq/oxjQrsFl3gfOwv9ttjYqfd7vSpojyNbZDdHYtzOkdyrOvpsWlYYn+4ef/qdCHpthsyx4Tv8H
 * lg/0xZ8oZVr5aJlFPR7uJAQrHdA=
 * -----END PRIVATE KEY-----
 */

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IDClientTest {

    private static IDClient idClient;
    private final static String adminPublicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqQhSJmd/fIafPpo40ooIWYmlDTwhFIP5\n" +
            "0QCRRCQLUenHF2f5VY0NSSnxmbUQXkQ7pvPMhH7aBVL6BpsofNkwdsapp5ZwhwXNx7j6UnQRGPZv\n" +
            "pCK3Dxcnr6rbcoXjbCSkxDPDdx/qwNSWIQ/3hTJP9A3EZ9r4CIEoJty5tyGWHMPYIaqrLtvZbaYr\n" +
            "RbmlpOGB7AIWRLLII8bRFNcSZ2qOGhHQ0Hm8axPKlVvWBIzViunHu7Z6KJ+Ef8RVYEJvgmPHH89b\n" +
            "wjM64Q08cN62aXzx132RyZaKSaa8bIYydT+VqJbHT6y1jq2TELXgMGnn+w9aUMJgAzd+6DZ3ISRD\n" +
            "0nL3UwIDAQAB\n" +
            "-----END PUBLIC KEY-----";
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

    private ValueHelper valueHelper = ValueHelper.getInstance();
    private static IDClient idClient1;

    @BeforeAll
    public static void setUp() throws Exception {
        //InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.149.5", 2641);
        InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.152.17", 2641);
        IDClientFactory idClientFactory = GlobalIdClientFactory.getIdClientFactory();
        PrivateKey privateKey = KeyConverter.fromPkcs8Pem(adminPrivateKeyPem);
        AuthenticationInfo authenticationInfo = new PublicKeyAuthenticationInfo(Util.encodeString("88.888.1024/admin"), 300, privateKey);
        idClient = idClientFactory.newInstance(inetSocketAddress, authenticationInfo, true);
        idClient1 = idClientFactory.newInstance(inetSocketAddress, authenticationInfo, false);
    }


    @Order(1)
    @Test
    void resolveHandle() throws IDException {

        HandleValue[] handleValues = idClient.resolveHandle("88.888.1024/admin");
        Assertions.assertTrue(handleValues.length > 0,"");
        idClient1.resolveHandle("88.888.1024/admin");
        idClient.resolveHandle("88.888.1024/admin");
        idClient1.resolveHandle("88.888.1024/admin");
        idClient.resolveHandle("88.888.1024/admin");
        idClient1.resolveHandle("88.888.1024/admin");
        idClient.resolveHandle("88.888.1024/admin");
        idClient1.resolveHandle("88.888.1024/admin");
        idClient.resolveHandle("88.888.1024/admin");
        idClient1.resolveHandle("88.888.1024/admin");
        idClient.resolveHandle("88.888.1024/admin");

    }

    @Order(2)
    @Test
    void createHandle() throws IDException {
        List<HandleValue> list = new ArrayList<>();

        HandleValue hv = new HandleValue(1, "URL", "https://www.teleinfo.cn");
        list.add(hv);

        HandleValue[] hvs = valueHelper.listToArray(list);

        idClient.createHandle("88.888.1024/test", hvs);
    }

    @Order(3)
    @Test
    void addHandleValues() throws IDException {
        List<HandleValue> list = new ArrayList<>();
        HandleValue hv = new HandleValue(2, "EMAIL", "lilong@teleinfo.cn");
        list.add(hv);
        HandleValue[] hvs = valueHelper.listToArray(list);

        idClient.addHandleValues("88.888.1024/test", hvs);
    }

    @Order(4)
    @Test
    void updateHandleValues() throws IDException {
        List<HandleValue> list = new ArrayList<>();
        HandleValue hv = new HandleValue(2, "EMAIL", "test@teleinfo.cn");
        list.add(hv);
        HandleValue[] hvs = valueHelper.listToArray(list);

        idClient.updateHandleValues("88.888.1024/test", hvs);
    }

    @Order(5)
    @Test
    void deleteHandle() throws IDException {
        idClient.deleteHandle("88.888.1024/test");
    }

    @Order(10)
    @Test
    void homeNa() throws IDException {
        idClient.homeNa("88.888.2048");
    }

    @Order(11)
    @Test
    void unhomeNa() throws IDException {
        idClient.unhomeNa("88.888.2048");
    }
}