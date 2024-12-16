package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.AbstractMessage;
import cn.teleinfo.idpointer.sdk.core.AuthenticationInfo;
import cn.teleinfo.idpointer.sdk.core.PublicKeyAuthenticationInfo;
import cn.teleinfo.idpointer.sdk.core.Util;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.security.HdlSecurityProvider;
import cn.teleinfo.idpointer.sdk.util.KeyConverter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

class SessionSetupTest {

    private final static String adminPublicKeyPem="-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqQhSJmd/fIafPpo40ooIWYmlDTwhFIP5\n" +
            "0QCRRCQLUenHF2f5VY0NSSnxmbUQXkQ7pvPMhH7aBVL6BpsofNkwdsapp5ZwhwXNx7j6UnQRGPZv\n" +
            "pCK3Dxcnr6rbcoXjbCSkxDPDdx/qwNSWIQ/3hTJP9A3EZ9r4CIEoJty5tyGWHMPYIaqrLtvZbaYr\n" +
            "RbmlpOGB7AIWRLLII8bRFNcSZ2qOGhHQ0Hm8axPKlVvWBIzViunHu7Z6KJ+Ef8RVYEJvgmPHH89b\n" +
            "wjM64Q08cN62aXzx132RyZaKSaa8bIYydT+VqJbHT6y1jq2TELXgMGnn+w9aUMJgAzd+6DZ3ISRD\n" +
            "0nL3UwIDAQAB\n" +
            "-----END PUBLIC KEY-----";
    private final static String adminPrivateKeyPem="-----BEGIN PRIVATE KEY-----\n" +
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
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SessionSetupTest.class);

    @Test
    @Disabled
    void initTest() throws Exception {
        IDClientFactory idClientFactory = GlobalIdClientFactory.getIdClientFactory();
        InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.3.2", 2641);
        IDClient idClient = idClientFactory.newInstance(inetSocketAddress);
        //ValueHelper valueHelper = ValueHelper.getInstance();

        //PublicKey publicKey = KeyConverter.fromX509Pem(adminPublicKeyPem);
        //HandleValue handleValue = valueHelper.newIdisPublicKeyValue(300, publicKey);
        //idClient.createHandle("88.888.1024/admin",new HandleValue[]{handleValue});

        idClient.resolveHandle("88.888.1024/admin");
    }

    @Test
    void test() throws Exception {
        IDClientFactory idClientFactory = GlobalIdClientFactory.getIdClientFactory();
        InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.149.5", 2641);
        //InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.3.2", 2641);

        PrivateKey privateKey = KeyConverter.fromPkcs8Pem(adminPrivateKeyPem);
        AuthenticationInfo authenticationInfo = new PublicKeyAuthenticationInfo(Util.encodeString("88.888.1024/admin"),300,privateKey);
        IDClient idClient = idClientFactory.newInstance(inetSocketAddress,authenticationInfo,true);

        idClient.resolveHandle("88.888.1024/admin");

        // Util.getPublicKeyFromBytes()
        // Util.getBytesFromPublicKey()
    }

    @Test
    void byteBufferTest() {
        ByteBuf in = Unpooled.buffer();
        in.writeBytes("hello".getBytes(StandardCharsets.UTF_8));

        byte[] dataArray = new byte[in.readableBytes()];
        in.readBytes(dataArray);

        System.out.println(new String(dataArray));;

    }

    @Test
    void encryptionTest() throws IDException {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeBytes("hello".getBytes(StandardCharsets.UTF_8));
        try {
            // create, initialize and cache a new encryption cipher
            HdlSecurityProvider provider = HdlSecurityProvider.getInstance();
            if (provider == null) {
                throw new IDException(IDException.MISSING_CRYPTO_PROVIDER, "Encryption/Key generation engine missing");
            }
            Cipher encryptCipher = provider.getCipher(1, Hex.decodeHex("49344030e177a387"), javax.crypto.Cipher.ENCRYPT_MODE, null, 2, 10);

            int len = byteBuf.readableBytes();
            byte[] buf = new byte[len];
            byteBuf.readBytes(buf);

            byte[] ciphertext = encryptCipher.doFinal(buf, 0, len);

            boolean legacy = !AbstractMessage.hasEqualOrGreaterVersion(2, 10, 2, 4);
            if (!legacy) {
                byte[] iv = encryptCipher.getIV();
                if (iv == null) {
                    iv = new byte[0];
                }
                ByteBuf toWriteBuf = Unpooled.buffer();
                toWriteBuf.writeBytes(iv);
                toWriteBuf.writeBytes(ciphertext);
                byteBuf = toWriteBuf;
            }else{
                byteBuf = Unpooled.wrappedBuffer(ciphertext);
            }
        } catch (Exception e) {
            log.error("===",e);
            if (e instanceof IDException) throw (IDException) e;
            throw new IDException(IDException.ENCRYPTION_ERROR, "Error encrypting buffer", e);
        }

        byte[] dataArray = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(dataArray);

        System.out.println(Hex.encodeHexString(dataArray));;
    }
}
