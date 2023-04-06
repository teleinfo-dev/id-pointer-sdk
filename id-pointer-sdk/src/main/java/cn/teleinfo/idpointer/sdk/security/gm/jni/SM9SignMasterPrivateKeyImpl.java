package cn.teleinfo.idpointer.sdk.security.gm.jni;

import cn.teleinfo.idpointer.sdk.security.gm.SM9SignMasterPrivateKey;
import org.apache.commons.io.IOUtils;
import org.gmssl.GmSSLJNI;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class SM9SignMasterPrivateKeyImpl implements SM9SignMasterPrivateKey {

    private String pem;
    private String password;

    private long data;


    public SM9SignMasterPrivateKeyImpl(String pem, String password, long data) {
        this.pem = pem;
        this.password = password;
        this.data = data;
    }

    public String getPem() {
        return pem;
    }

    public void setPem(String pem) {
        this.pem = pem;
    }

    public long getData() {
        return data;
    }

    public void setData(long data) {
        this.data = data;
    }
}
