package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.Common;
import cn.teleinfo.idpointer.sdk.core.GsonUtility;
import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.core.ValueReference;
import cn.teleinfo.idpointer.sdk.core.trust.ChainVerificationReport;
import cn.teleinfo.idpointer.sdk.core.trust.HandleSigner;
import cn.teleinfo.idpointer.sdk.core.trust.JsonWebSignature;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.security.gm.SM2Factory;
import cn.teleinfo.idpointer.sdk.util.KeyConverter;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CertTest {
    private ValueHelper valueHelper = ValueHelper.getInstance();

    private static IDClient idClient;

    private String rootPublicKeyPem="-----BEGIN PUBLIC KEY-----\n" +
            "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAE6F6lpeRnhprRV3gzspKM+GRkrCrM9QwkjZRHinPi\n" +
            "x3nLnMXxIT+3JK5pWTdsSjqTVSgNQ0nmAbGjDMSb+Y31Ng==\n" +
            "-----END PUBLIC KEY-----";

    private String rootPrivateKeyPem="-----BEGIN PRIVATE KEY-----\n" +
            "MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgNqA0h6kmE8mNnB3pB7HleescLlo7\n" +
            "jF28PZnF63qUMLqgCgYIKoEcz1UBgi2hRANCAAToXqWl5GeGmtFXeDOykoz4ZGSsKsz1DCSNlEeK\n" +
            "c+LHecucxfEhP7ckrmlZN2xKOpNVKA1DSeYBsaMMxJv5jfU2\n" +
            "-----END PRIVATE KEY-----";

    private String secondPublicKeyPem="-----BEGIN PUBLIC KEY-----\n" +
            "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAEyA+v49bJhBrsGrCAT2cyfKTAQwHIwrucfwy00Fm9\n" +
            "rxw+DgJ6YOA7rC7wONnIvmV2iiAa4F30TU+GHZBEUKLZag==\n" +
            "-----END PUBLIC KEY-----";

    private String secondPrivateKeyPem="-----BEGIN PRIVATE KEY-----\n" +
            "MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgg1Vb/CbO93o5ptlXaK0TysCuKKLd\n" +
            "BqmavJZdAsZjCqygCgYIKoEcz1UBgi2hRANCAATID6/j1smEGuwasIBPZzJ8pMBDAcjCu5x/DLTQ\n" +
            "Wb2vHD4OAnpg4DusLvA42ci+ZXaKIBrgXfRNT4YdkERQotlq\n" +
            "-----END PRIVATE KEY-----";


    @BeforeAll
    static void beforeAll() {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.153.106", 30041);
        idClient = new SampleIdClient(inetSocketAddress, 2, 10000);

    }

    @Disabled
    @Test
    void keyTest() throws Exception {
        KeyPair pair = SM2Factory.generatePrivateKey();
        PublicKey aPublic = pair.getPublic();
        String x509Pem = KeyConverter.toX509Pem(aPublic);
        System.out.println(x509Pem);

        PublicKey publicKey = KeyConverter.fromX509Pem(x509Pem);
        
        Assertions.assertEquals(aPublic, publicKey);

        PrivateKey privateKey = pair.getPrivate();
        String pkcs8Pem = KeyConverter.toPkcs8UnencryptedPem(privateKey);

        System.out.println(pkcs8Pem);

        PrivateKey privateKey1 = KeyConverter.fromPkcs8Pem(pkcs8Pem);
        Assertions.assertEquals(privateKey, privateKey1);

    }



    @Order(1)
    @Test
    void afterAllTest() throws IDException {
        idClient.deleteHandle("88.802.1/0.0");
        idClient.deleteHandle("88.802.1/user");
        idClient.deleteHandle("88.802.1/test");
        idClient.unhomeNa("88.802.1");
        idClient.homeNa("88.802.1");
    }



    @Order(1)
    @Test
    void rootCertInitTest() throws Exception {
        List<HandleValue> valueList = new ArrayList<>();

        PublicKey rootPublicKey = KeyConverter.fromX509Pem(rootPublicKeyPem);
        PrivateKey rootPrivateKey = KeyConverter.fromPkcs8Pem(rootPrivateKeyPem);
        HandleValue pubKeyValue = valueHelper.newIdisPublicKeyValue(301,rootPublicKey);
        valueList.add(pubKeyValue);

        LocalDateTime now = LocalDateTime.now().minusYears(1);
        LocalDateTime expr = LocalDateTime.now().plusYears(1);
        HandleValue certValue = valueHelper.newCertValue(400, rootPublicKey, "301:88.802.1/0.0", "301:88.802.1/0.0", rootPrivateKey, expr, now, now);
        valueList.add(certValue);

        idClient.createHandle("88.802.1/0.0", valueHelper.listToArray(valueList));
    }

    @Order(3)
    @Test
    void secondCertInitTest() throws Exception {
        List<HandleValue> valueList = new ArrayList<>();

        PublicKey secondPublicKey = KeyConverter.fromX509Pem(secondPublicKeyPem);
        PrivateKey rootKeyPrivateKey = KeyConverter.fromPkcs8Pem(rootPrivateKeyPem);

        HandleValue pubKeyValue = valueHelper.newIdisPublicKeyValue(301, secondPublicKey);
        valueList.add(pubKeyValue);

        LocalDateTime now = LocalDateTime.now().minusYears(1);
        LocalDateTime expr = LocalDateTime.now().plusYears(1);
        HandleValue certValue = valueHelper.newCertValue(400, secondPublicKey, "301:88.802.1/0.0", "301:88.802.1/user", rootKeyPrivateKey, expr, now, now);
        valueList.add(certValue);

        idClient.createHandle("88.802.1/user", valueHelper.listToArray(valueList));
    }

    @Order(4)
    @Test
    void signatureInitTest() throws Exception {

        String handle = "88.802.1/test";
        List<HandleValue> valueList = new ArrayList<>();

        HandleValue handleValue = new HandleValue(1, "URL", "www.test.cn");
        valueList.add(handleValue);
        handleValue = new HandleValue(2, "EMAIL", "test@teleinfo.cn");
        valueList.add(handleValue);

        PrivateKey secondPrivateKey = KeyConverter.fromPkcs8Pem(secondPrivateKeyPem);

        HandleSigner handleSigner = HandleSigner.getInstance();
        LocalDateTime notBefore = LocalDateTime.now().minusYears(1);
        LocalDateTime expr = LocalDateTime.now().plusYears(1);
        JsonWebSignature jsonWebSignature = handleSigner.signHandleValues(handle, valueList, new ValueReference("88.802.1/user", 301), secondPrivateKey, null, notBefore.toEpochSecond(ValueHelper.zoneOffsetBj), expr.toEpochSecond(ValueHelper.zoneOffsetBj));
        String data = jsonWebSignature.serialize();

        HandleValue sigValue = new HandleValue(400, Common.STR_HS_SIGNATURE_TYPE, data);
        valueList.add(sigValue);

        idClient.createHandle(handle, valueHelper.listToArray(valueList));

    }

    @Order(5)
    @Test
    void certVerifyTest() throws Exception {

        String handle = "88.802.1/user";
        HandleValue[] hvs = idClient.resolveHandle(handle);
        HandleValue certValue = null;
        for (int i = 0; i < hvs.length; i++) {
            if (hvs[i].getIndex() == 400) {
                certValue = hvs[i];
                break;
            }
        }

        TrustResolveManager trustResolveManager = TrustResolveManager.getInstance();
        ChainVerificationReport chainVerificationReport = trustResolveManager.validateCertValue(certValue);

        String reportJson = GsonUtility.getPrettyGson().toJson(chainVerificationReport);
        System.out.println(reportJson);

    }

    @Order(6)
    @Test
    void sigVerifyTest() throws Exception {

        String handle = "88.802.1/test";
        HandleValue[] hvs = idClient.resolveHandle(handle);
        HandleValue sigValue = null;
        for (int i = 0; i < hvs.length; i++) {
            if (hvs[i].getIndex() == 400) {
                sigValue = hvs[i];
                break;
            }
        }

        TrustResolveManager trustResolveManager = TrustResolveManager.getInstance();
        ChainVerificationReport chainVerificationReport = trustResolveManager.validateSignatureValue(handle, hvs, sigValue);

        String reportJson = GsonUtility.getPrettyGson().toJson(chainVerificationReport);
        System.out.println(reportJson);

    }
}
