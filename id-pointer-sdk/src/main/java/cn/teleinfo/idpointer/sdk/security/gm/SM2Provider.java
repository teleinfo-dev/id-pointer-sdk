package cn.teleinfo.idpointer.sdk.security.gm;

import java.security.Provider;

public class SM2Provider extends Provider {
    /**
     * Constructs a provider with the specified name, version number,
     * and information.
     */
    public SM2Provider() {
        super("SM2Provider", 1.0, "SM2Provider");
        put("KeyFactory.SM2", "cn.teleinfo.idpointer.sdk.security.gm.SM2KeyFactorySpi");
        put("KeyPairGenerator.SM2", "cn.teleinfo.idpointer.sdk.security.gm.SM2KeyPairGeneratorSpi");
        //put("Signature.SM3withSM2", "cn.teleinfo.idpointer.sdk.security.gm.SM2SignatureSpi");
    }
}
