package cn.teleinfo.idpointer.sdk.security.gm.jni;

import cn.teleinfo.idpointer.sdk.security.gm.SM9IdPrivateKey;

public class SM9IdPrivateKeyImpl implements SM9IdPrivateKey {
    private String pem;
    private String password;

    private long key;

    public SM9IdPrivateKeyImpl(String pem, String password, long key) {
        this.pem = pem;
        this.password = password;
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
