package cn.teleinfo.idpointer.sdk.security.gm;

import cn.hutool.crypto.SecureUtil;

import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.SecureRandom;

public class SM2KeyPairGeneratorSpi extends KeyPairGeneratorSpi {
    private SecureRandom random;
    private int keySize = 256;

    @Override
    public void initialize(int keysize, SecureRandom random) {
        if (keysize != 256) {
            throw new InvalidParameterException("SM2 supports only 256-bit key size");
        }
        this.keySize = keysize;
        this.random = random;
    }

    @Override
    public KeyPair generateKeyPair() {
        KeyPair pair = SecureUtil.generateKeyPair("SM2");
        SM2PrivateKey privateKey = new SM2PrivateKey(pair.getPrivate().getEncoded());
        SM2PublicKey publicKey = new SM2PublicKey(pair.getPublic().getEncoded());
        return new KeyPair(publicKey, privateKey);
    }
}