package cn.teleinfo.idpointer.sdk.security.gm;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

public class SM9IdPublicKey implements PublicKey {
    private String id;

    public SM9IdPublicKey(String id) {
        this.id = id;
    }

    @Override
    public String getAlgorithm() {
        return "SM9";
    }

    @Override
    public String getFormat() {
        return id;
    }

    @Override
    public byte[] getEncoded() {
        return id.getBytes(StandardCharsets.UTF_8);
    }
}
