package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.GsonUtility;
import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.core.trust.ChainVerificationReport;
import cn.teleinfo.idpointer.sdk.security.gm.SM2Factory;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.time.LocalDateTime;

public class CertTest {
    @Test
    void sm2CertValueTest() throws Exception {

        ValueHelper valueHelper = ValueHelper.getInstance();

        LocalDateTime now = LocalDateTime.now().minusYears(1);
        LocalDateTime expr = LocalDateTime.now().plusYears(1);

        KeyPair keyPair = SM2Factory.generatePrivateKey();
        PublicKey aPublic = keyPair.getPublic();
        PrivateKey aPrivate = keyPair.getPrivate();

        HandleValue certValue = valueHelper.newCertValue(400, aPublic, "301:88.802.1/0.0", "301:88.802.1/0.0", aPrivate, expr, now, now);
        String json = GsonUtility.getPrettyGson().toJson(certValue);

        System.out.println(json);

        TrustResolveManager trustResolveManager = TrustResolveManager.getInstance();
        ChainVerificationReport chainVerificationReport = trustResolveManager.validateCertValue(certValue);

        String result = GsonUtility.getPrettyGson().toJson(chainVerificationReport);

        System.out.println(result);


    }

}
