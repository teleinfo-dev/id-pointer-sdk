package cn.teleinfo.idpointer.sdk.security.gm;

import java.security.KeyPair;
import java.security.Provider;
import java.security.Security;

public abstract class SM2Factory {

    private static SM2Provider sm2Provider;

    public static KeyPair generatePrivateKey() {
        SM2KeyPairGeneratorSpi sm2KeyPairGeneratorSpi = new SM2KeyPairGeneratorSpi();
        return sm2KeyPairGeneratorSpi.generateKeyPair();
    }

    public static Provider getProvider() {
        if (sm2Provider == null) {
            synchronized (SM2Factory.class) {
                if (sm2Provider == null) {
                    sm2Provider = new SM2Provider();
                }
            }
        }
        Security.addProvider(sm2Provider);
        return sm2Provider;
    }

}
