package cn.teleinfo.idpointer.sdk.security.gm;

import java.security.PrivateKey;
import java.util.Arrays;

public class SM2PrivateKey implements PrivateKey {
    private final byte[] encoded;

    public SM2PrivateKey(byte[] encoded) {
        this.encoded = Arrays.copyOf(encoded, encoded.length);
    }

    @Override
    public String getAlgorithm() {
        return "SM2";
    }

    @Override
    public String getFormat() {
        return "PKCS#8";
    }

    @Override
    public byte[] getEncoded() {
        return Arrays.copyOf(encoded, encoded.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SM2PrivateKey that = (SM2PrivateKey) o;

        return Arrays.equals(encoded, that.encoded);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(encoded);
    }
}
