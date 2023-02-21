package cn.teleinfo.idpointer.sdk.security.sm;

import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.SecureRandom;

// todo: impl
public class SM2KeyPairGenerator extends KeyPairGeneratorSpi {
    @Override
    public void initialize(int keySize, SecureRandom random) {

    }

    @Override
    public KeyPair generateKeyPair() {
        return null;
    }
}
