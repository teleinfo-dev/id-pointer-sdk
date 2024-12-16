package cn.teleinfo.idpointer.sdk.security.gm;

import cn.teleinfo.idpointer.sdk.security.gm.jni.SM9KeyConverterImpl;

public interface SM9KeyConverter {
    SM9SignMasterPrivateKey signMasterPrivateKeyFromPem(String pem, String password);
    SM9SignMasterPublicKey signMasterPublicKeyFromPem(String pem);
    SM9IdPrivateKey idPrivateKeyFromPem(String pem, String password);

    SM9KeyConverter instance = new SM9KeyConverterImpl();

}
