package cn.teleinfo.idpointer.sdk.security.gm;

import java.security.PublicKey;
import java.util.Arrays;

public class SM2PublicKey implements PublicKey {

    private byte[] encoded;

    public SM2PublicKey(byte[] encoded) {
        this.encoded = Arrays.copyOf(encoded, encoded.length);
    }

    @Override
    public String getAlgorithm() {
        return "SM2";
    }

    @Override
    public String getFormat() {
        return "X.509";
    }

    @Override
    public byte[] getEncoded() {
        return Arrays.copyOf(encoded, encoded.length);
    }
}
