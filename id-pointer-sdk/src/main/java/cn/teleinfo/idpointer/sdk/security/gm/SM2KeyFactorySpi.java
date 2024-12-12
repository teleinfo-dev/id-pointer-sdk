package cn.teleinfo.idpointer.sdk.security.gm;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class SM2KeyFactorySpi extends KeyFactorySpi {

    @Override
    protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec instanceof X509EncodedKeySpec) {
            byte[] encoded = ((X509EncodedKeySpec) keySpec).getEncoded();
            return new SM2PublicKey(encoded);
        } else {
            throw new InvalidKeySpecException("Unsupported key specification: " + keySpec.getClass());
        }
    }

    @Override
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            byte[] encoded = ((PKCS8EncodedKeySpec) keySpec).getEncoded();
            return new SM2PrivateKey(encoded);
        } else {
            throw new InvalidKeySpecException("Unsupported key specification: " + keySpec.getClass());
        }
    }

    @Override
    protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec) throws InvalidKeySpecException {
        if (key instanceof SM2PublicKey && keySpec.isAssignableFrom(X509EncodedKeySpec.class)) {
            return keySpec.cast(new X509EncodedKeySpec(key.getEncoded()));
        } else {
            throw new InvalidKeySpecException("Unsupported key or key specification: " + key.getClass() + ", " + keySpec);
        }

    }

    @Override
    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        if (key instanceof SM2PublicKey) {
            return key;
        } else if (key instanceof SM2PrivateKey) {
            return key;
        } else {
            throw new InvalidKeyException("Unsupported key type: " + key.getClass());
        }
    }
}
