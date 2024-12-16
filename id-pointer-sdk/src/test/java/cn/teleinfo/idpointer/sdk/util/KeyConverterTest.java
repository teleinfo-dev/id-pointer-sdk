package cn.teleinfo.idpointer.sdk.util;

import org.junit.jupiter.api.Test;

import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

class KeyConverterTest {

    @Test
    void keyTest() throws Exception {
        String mpk = "-----BEGIN SM9 ENC MASTER PUBLIC KEY-----\n" +
                "MEQDQgAEsdkfa842OmdIaniSWjnWLp7Ful2nyklDoAMOdInul5eHbQQK4Pm8yAIE\n" +
                "Pz3ZmFN2ePzJY2u4KWCo7GVEIeuMKw==\n" +
                "-----END SM9 ENC MASTER PUBLIC KEY-----";
        String mPrivateKey = "-----BEGIN ENCRYPTED SM9 ENC MASTER KEY-----\n" +
                "MIH2MGEGCSqGSIb3DQEFDTBUMDQGCSqGSIb3DQEFDDAnBBCFmQgagZJFNQ9jHZ4H\n" +
                "nL30AgMBAAACARAwCwYJKoEcz1UBgxECMBwGCCqBHM9VAWgCBBBxFAKnDopg2wmv\n" +
                "FxoQXbTdBIGQwyH7f2A49j7dQZmcVgo/ExSj8UGitgwpJlVRM5HfgA7zy4+HoJhx\n" +
                "7aXl+jtdXbqhwDYypzD26P6zD2qWpNl+YtJu2n+syfN3+igzgDCixxBpj7iwQjnu\n" +
                "ALV+Cne/Qmi1e5WAoHkE2v7vhOWJ6Q8aogAVrKtIkdRFj0revBd8y5RKMD5sFdKp\n" +
                "uGpOLMcHdpVJ\n" +
                "-----END ENCRYPTED SM9 ENC MASTER KEY-----";

        PublicKey publicKey = KeyConverter.fromX509Pem(mpk);
        System.out.println(publicKey);

    }
}