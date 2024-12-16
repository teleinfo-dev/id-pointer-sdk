package cn.teleinfo.idpointer.sdk.security.gm.jni;

import cn.teleinfo.idpointer.sdk.security.gm.SM9IdPrivateKey;
import cn.teleinfo.idpointer.sdk.security.gm.SM9SignAndVerify;
import cn.teleinfo.idpointer.sdk.security.gm.SM9SignMasterPublicKey;
import org.gmssl.GmSSLJNI;

public class SM9SignAndVerifyImpl implements SM9SignAndVerify {

    @Override
    public byte[] sign(SM9IdPrivateKey sm9IdPrivateKey,byte[] toSignData){
        long sm9_ctx= GmSSLJNI.sm9_sign_ctx_new();
        GmSSLJNI.sm9_sign_init(sm9_ctx);
        GmSSLJNI.sm9_sign_update(sm9_ctx, toSignData, 0, toSignData.length);
        return GmSSLJNI.sm9_sign_finish(sm9_ctx, sm9IdPrivateKey.getKey());
    }

    @Override
    public boolean verify(SM9SignMasterPublicKey sm9SignMasterPublicKey,String id,byte[] toSignData,byte[] sig){
        long sm9_ctx = GmSSLJNI.sm9_sign_ctx_new();
        GmSSLJNI.sm9_verify_init(sm9_ctx);
        GmSSLJNI.sm9_verify_update(sm9_ctx, toSignData, 0, toSignData.length);
        int verify_ret = GmSSLJNI.sm9_verify_finish(sm9_ctx, sig, sm9SignMasterPublicKey.getKey(), id);
        return verify_ret==1;
    }

}
