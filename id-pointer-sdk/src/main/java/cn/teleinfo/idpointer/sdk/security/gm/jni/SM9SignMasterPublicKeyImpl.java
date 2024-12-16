package cn.teleinfo.idpointer.sdk.security.gm.jni;

import cn.teleinfo.idpointer.sdk.security.gm.SM9SignMasterPublicKey;

public class SM9SignMasterPublicKeyImpl implements SM9SignMasterPublicKey {
    private String pem;
    private long key;

    public SM9SignMasterPublicKeyImpl(String pem, long key) {
        this.pem = pem;
        this.key = key;
    }

    public String getPem() {
        return pem;
    }

    @Override
    public long getKey() {
        return key;
    }
}
