package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.util.KeyConverter;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * -----BEGIN PUBLIC KEY-----
 * MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgIe1pa4giqqu0rAXmQDQ9J1JO5ocL7mj
 * GXQ4IKX+ANW0HDDG7rzbO2d2sXtLPmG5RjXSqktn8rg+mR3O+h4zka9Ub/0HU/oCVtINetrNTLM8
 * 292BOoa9QcwzYSB24H7JbtEsu0MZ55jR5zPX1SY2rQzqu9M+2X/HIvpCxbIr/YnUR0IxxsMsVUf+
 * xsRyA8nmpeKMiSHDpx+VdkQ6Y9CqPFSFBFdurJsWjY8qaCGZOHlXPprUkaHFAumey2po+FykeXHA
 * 9f6LdOMyPdWEPSNG6bRPD+9qK6/A6IMOw9rn3fuewWqM4vXmxf4aO0Ie5yx2gpShpqwKO5itB/9Z
 * ZaJXPQIDAQAB
 * -----END PUBLIC KEY-----
 * <p>
 * -----BEGIN PRIVATE KEY-----
 * MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCAh7WlriCKqq7SsBeZAND0nUk7
 * mhwvuaMZdDggpf4A1bQcMMbuvNs7Z3axe0s+YblGNdKqS2fyuD6ZHc76HjORr1Rv/QdT+gJW0g16
 * 2s1Mszzb3YE6hr1BzDNhIHbgfslu0Sy7QxnnmNHnM9fVJjatDOq70z7Zf8ci+kLFsiv9idRHQjHG
 * wyxVR/7GxHIDyeal4oyJIcOnH5V2RDpj0Ko8VIUEV26smxaNjypoIZk4eVc+mtSRocUC6Z7Lamj4
 * XKR5ccD1/ot04zI91YQ9I0bptE8P72orr8Dogw7D2ufd+57Baozi9ebF/ho7Qh7nLHaClKGmrAo7
 * mK0H/1llolc9AgMBAAECggEATGDY9/DG08BkDT2peDm88m/5UdrdvxqgqoKey//4NkTIOCxgOwSt
 * ftqX9RYge5yU/f0ECECxL9J/GHxXP9jsqjk3gykozqV1+GPwsL4PW/HzS3CQ19ez5cLUEXa6TePq
 * DdltrIZOgYsVyT/MlZSUzzcq0pzQ72z4SHfRcCvV6l6/zEkJHfojQy+pc2rD937ilTVHofHSe1tq
 * MVVJy8imKyCJZhmRNGyQcESAmOKHACR9eWybQUb4XklKuJZYis0Uu+ZEfBLhTIJg9EcfNGwcccL4
 * RYzcjNYJKAeb2OCpeS4K4K//bdCIwwzUr1r7c7ii0O3knR4aEt43njQLt29cIQKBgQDWlp9BnfB9
 * GciI5ypIBFp63EWmSfctWmMBjvvEWCn8rEOMJKEsuHro+JLLs1dz+Tuapc+sgiFpBms4z1y1aVJx
 * HZd+0v8CYyMc5DB1qhwI2QxhPRHDV0EAD/JKwCeYdosB5fclM7ixKzDI/eoKyLmLk1YBlSO67aVW
 * uK1QnucxdQKBgQCZVYYoQazcF/MYU8dQZCI6utfqi1Q7sqGTyWrERlXRuW85a5Llv7zIcfaZcyxk
 * Uhp/1vh4BsHg4pPBVeXfWy2scMGBz4v8ebk2D8DCsb5blkAfQIWe00+xtQFNeT+rSacNkpekrGtb
 * dPfmwZ9KCSMvjN6UAZtnd+m8oNG/tcjNqQKBgQDJYGRnsY4D+HRGdZkqKFu4vInoObVqrE4JImOG
 * qK4OXqKtG5rdWuqNQuiPnOfO/+89e6leXGh6JPnuQDriS8qAOKL48404ckx7SnFnmpHgg7+oaSUI
 * ShPHuS9JkvYVj/l3eJXsJZOHP97yX8aJBEGLiGLH7WuwemAU+A49gEG6GQKBgHvn54xaXfRRCilL
 * chHdiOeKmvY2du/yzqzdI4DOEYVzYpS1ADTWNxiHNy4TKvk6e514urpLQ5qVna6q2iIezQ6+4zAh
 * 5k40kt1D55GtDlV2WgSnIBMTUSAaU54mb7PbxbmrDw2MIwj5wWXgavbp4VwIasiRb+IxvVinYDea
 * D1VZAoGAMr27uqId9Ul0WsTq+sp8X31D3UoronXPlV/U4MNpilzZxGIgjaciW3aAOEwA5vzbe/bt
 * z1GNrXVpkhu3Gy18GkDPTql/w5rzs8XOTrsfydyQ9yz2hEmY3zx6WSFyICTNvitjTjMV0c+nc06/
 * nggRZkOvx3z3Lb4k+UZ4Qc6GrHM=
 * -----END PRIVATE KEY-----
 */
class DefaultIdClientTest {

    private String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
            " MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgIe1pa4giqqu0rAXmQDQ9J1JO5ocL7mj\n" +
            " GXQ4IKX+ANW0HDDG7rzbO2d2sXtLPmG5RjXSqktn8rg+mR3O+h4zka9Ub/0HU/oCVtINetrNTLM8\n" +
            " 292BOoa9QcwzYSB24H7JbtEsu0MZ55jR5zPX1SY2rQzqu9M+2X/HIvpCxbIr/YnUR0IxxsMsVUf+\n" +
            " xsRyA8nmpeKMiSHDpx+VdkQ6Y9CqPFSFBFdurJsWjY8qaCGZOHlXPprUkaHFAumey2po+FykeXHA\n" +
            " 9f6LdOMyPdWEPSNG6bRPD+9qK6/A6IMOw9rn3fuewWqM4vXmxf4aO0Ie5yx2gpShpqwKO5itB/9Z\n" +
            " ZaJXPQIDAQAB\n" +
            " -----END PUBLIC KEY-----";

    private String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
            " MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCAh7WlriCKqq7SsBeZAND0nUk7\n" +
            " mhwvuaMZdDggpf4A1bQcMMbuvNs7Z3axe0s+YblGNdKqS2fyuD6ZHc76HjORr1Rv/QdT+gJW0g16\n" +
            " 2s1Mszzb3YE6hr1BzDNhIHbgfslu0Sy7QxnnmNHnM9fVJjatDOq70z7Zf8ci+kLFsiv9idRHQjHG\n" +
            " wyxVR/7GxHIDyeal4oyJIcOnH5V2RDpj0Ko8VIUEV26smxaNjypoIZk4eVc+mtSRocUC6Z7Lamj4\n" +
            " XKR5ccD1/ot04zI91YQ9I0bptE8P72orr8Dogw7D2ufd+57Baozi9ebF/ho7Qh7nLHaClKGmrAo7\n" +
            " mK0H/1llolc9AgMBAAECggEATGDY9/DG08BkDT2peDm88m/5UdrdvxqgqoKey//4NkTIOCxgOwSt\n" +
            " ftqX9RYge5yU/f0ECECxL9J/GHxXP9jsqjk3gykozqV1+GPwsL4PW/HzS3CQ19ez5cLUEXa6TePq\n" +
            " DdltrIZOgYsVyT/MlZSUzzcq0pzQ72z4SHfRcCvV6l6/zEkJHfojQy+pc2rD937ilTVHofHSe1tq\n" +
            " MVVJy8imKyCJZhmRNGyQcESAmOKHACR9eWybQUb4XklKuJZYis0Uu+ZEfBLhTIJg9EcfNGwcccL4\n" +
            " RYzcjNYJKAeb2OCpeS4K4K//bdCIwwzUr1r7c7ii0O3knR4aEt43njQLt29cIQKBgQDWlp9BnfB9\n" +
            " GciI5ypIBFp63EWmSfctWmMBjvvEWCn8rEOMJKEsuHro+JLLs1dz+Tuapc+sgiFpBms4z1y1aVJx\n" +
            " HZd+0v8CYyMc5DB1qhwI2QxhPRHDV0EAD/JKwCeYdosB5fclM7ixKzDI/eoKyLmLk1YBlSO67aVW\n" +
            " uK1QnucxdQKBgQCZVYYoQazcF/MYU8dQZCI6utfqi1Q7sqGTyWrERlXRuW85a5Llv7zIcfaZcyxk\n" +
            " Uhp/1vh4BsHg4pPBVeXfWy2scMGBz4v8ebk2D8DCsb5blkAfQIWe00+xtQFNeT+rSacNkpekrGtb\n" +
            " dPfmwZ9KCSMvjN6UAZtnd+m8oNG/tcjNqQKBgQDJYGRnsY4D+HRGdZkqKFu4vInoObVqrE4JImOG\n" +
            " qK4OXqKtG5rdWuqNQuiPnOfO/+89e6leXGh6JPnuQDriS8qAOKL48404ckx7SnFnmpHgg7+oaSUI\n" +
            " ShPHuS9JkvYVj/l3eJXsJZOHP97yX8aJBEGLiGLH7WuwemAU+A49gEG6GQKBgHvn54xaXfRRCilL\n" +
            " chHdiOeKmvY2du/yzqzdI4DOEYVzYpS1ADTWNxiHNy4TKvk6e514urpLQ5qVna6q2iIezQ6+4zAh\n" +
            " 5k40kt1D55GtDlV2WgSnIBMTUSAaU54mb7PbxbmrDw2MIwj5wWXgavbp4VwIasiRb+IxvVinYDea\n" +
            " D1VZAoGAMr27uqId9Ul0WsTq+sp8X31D3UoronXPlV/U4MNpilzZxGIgjaciW3aAOEwA5vzbe/bt\n" +
            " z1GNrXVpkhu3Gy18GkDPTql/w5rzs8XOTrsfydyQ9yz2hEmY3zx6WSFyICTNvitjTjMV0c+nc06/\n" +
            " nggRZkOvx3z3Lb4k+UZ4Qc6GrHM=\n" +
            " -----END PRIVATE KEY-----";

    @Disabled
    @Test
    void initUser() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.149.222", 2641);
        IDClientFactory idClientFactory = GlobalIdClientFactory.getIdClientFactory();
        IDClient idClient = idClientFactory.newInstance(inetSocketAddress);

        ValueHelper valueHelper = ValueHelper.getInstance();
        PublicKey publicKey = KeyConverter.fromX509Pem(publicKeyPem);
        HandleValue handleValue = valueHelper.newPublicKeyValue(300, publicKey);

        HandleValue adminReadValue = new HandleValue(1,"URL","https://www.baidu.com");
        adminReadValue.setAnyoneCanRead(false);
        adminReadValue.setAdminCanRead(true);

        //idClient.deleteHandle("88.888.1024/admin");
        idClient.createHandle("88.888.1024/admin",new HandleValue[]{adminReadValue,handleValue});
    }

    @Test
    void resolveHandle() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.149.222", 2641);
        IDClientFactory idClientFactory = GlobalIdClientFactory.getIdClientFactory();
        IDClient idClient = idClientFactory.newInstance(inetSocketAddress);

        idClient.resolveHandle("88.888.1024/admin", null, null);
    }

    @Test
    void resolveHandleWithToken() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.149.222", 2641);
        IDClientFactory idClientFactory = GlobalIdClientFactory.getIdClientFactory();
        IDClient idClient = idClientFactory.newInstance(inetSocketAddress);

        PublicKey publicKey = KeyConverter.fromX509Pem(publicKeyPem);
        PrivateKey privateKey = KeyConverter.fromPkcs8Pem(privateKeyPem, null);

        ValueHelper valueHelper = ValueHelper.getInstance();
        String token = valueHelper.generateUserToken(300, "88.888.1024/admin", 123456, (RSAPrivateKey) privateKey);

        System.out.println(token);

        //idClient.resolveHandle("88.888.1024/admin", null, null);
        idClient.resolveHandle("88.888.1024/admin", null, null,token);
    }

    @Disabled
    @Test
    void tokenJWTTest() throws Exception {

        PublicKey publicKey = KeyConverter.fromX509Pem(publicKeyPem);
        PrivateKey privateKey = KeyConverter.fromPkcs8Pem(privateKeyPem, null);
        try {
            Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);
            String token = JWT.create()
                    .withClaim("user_index", 300)
                    .withClaim("user_handle", "88.888.1024/admin")
                    .withClaim("nonce", 123456)
                    //.withClaim("timestamp",System.currentTimeMillis())
                    .sign(algorithm);
            System.out.println(token);

            // 解析 token
            DecodedJWT decodedJWT = JWT.decode(token);

            // 验证 token
            Algorithm algorithm1 = Algorithm.RSA256((RSAPublicKey) publicKey, null);
            JWT.require(algorithm1).build().verify(decodedJWT);

        } catch (JWTCreationException exception) {
            // Invalid Signing configuration / Couldn't convert Claims.
        }
    }
}