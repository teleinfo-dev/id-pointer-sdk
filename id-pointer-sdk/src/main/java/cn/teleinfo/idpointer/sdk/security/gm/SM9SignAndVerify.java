package cn.teleinfo.idpointer.sdk.security.gm;

import cn.teleinfo.idpointer.sdk.security.gm.jni.SM9SignAndVerifyImpl;

public interface SM9SignAndVerify {
    byte[] sign(SM9IdPrivateKey sm9IdPrivateKey, byte[] toSignData);

    boolean verify(SM9SignMasterPublicKey sm9SignMasterPublicKey, String id, byte[] toSignData, byte[] sig);

    SM9SignAndVerify instance = new SM9SignAndVerifyImpl();

}
