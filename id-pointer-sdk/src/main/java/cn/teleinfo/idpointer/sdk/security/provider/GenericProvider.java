/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.security.provider;


import cn.teleinfo.idpointer.sdk.core.AbstractMessage;
import cn.teleinfo.idpointer.sdk.core.Encoder;
import cn.teleinfo.idpointer.sdk.core.Util;
import cn.teleinfo.idpointer.sdk.security.HdlSecurityProvider;

import javax.crypto.*;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.KeySpec;

/**
 * An implementation of the net.handle.HDLSecurityProvider interface
 * that accesses the generic java security/crypto interfaces.
 */
public final class GenericProvider extends HdlSecurityProvider {

    /** Construct and return a Cipher object, initialized to either decrypt or
     * encrypt using the given algorithm and secret key.  The direction parameter
     * must be either Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE.  The algorithm
     * parameter should be one of the HdlSecurityProvider.ENCRYPT_ALG_* constants.
     */
    @Override
    public Cipher getCipher(int algorithm, byte secretKey[], int direction, byte[] iv, int majorProtocolVersion, int minorProtocolVersion) throws Exception {
        Cipher cipher;
        SecretKey key;

        String keyAlg;
        String cipherAlg;

        KeySpec spec;
        boolean legacy = !AbstractMessage.hasEqualOrGreaterVersion(majorProtocolVersion, minorProtocolVersion, 2, 4);
        switch (algorithm) {
        case HdlSecurityProvider.ENCRYPT_ALG_DES:
            keyAlg = "DES";
            cipherAlg = legacy ? "DES/ECB/PKCS5Padding" : "DES/CBC/PKCS5Padding";
            spec = new DESKeySpec(secretKey);
            break;
        case HdlSecurityProvider.ENCRYPT_ALG_AES:
            keyAlg = "AES";
            cipherAlg = legacy ? "AES" : "AES/CBC/PKCS5Padding";
            if (secretKey.length > 16) secretKey = Util.substring(secretKey, 0, 16);
            spec = new SecretKeySpec(secretKey, "AES");
            break;
        case HdlSecurityProvider.ENCRYPT_ALG_DESEDE:
            keyAlg = "DESede";
            cipherAlg = legacy ? "DESede/ECB/PKCS5Padding" : "DESede/CBC/PKCS5Padding";
            spec = new DESedeKeySpec(secretKey);
            break;
        default:
            throw new Exception("Invalid encryption algorithm code: " + algorithm);
        }

        if (spec instanceof SecretKeySpec) key = (SecretKeySpec) spec;
        else {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(keyAlg);
            key = factory.generateSecret(spec);
        }
        cipher = Cipher.getInstance(cipherAlg);
        if (iv != null) {
            cipher.init(direction, key, new IvParameterSpec(iv));
        } else {
            cipher.init(direction, key);
        }
        return cipher;
    }

    @Override
    public int getIvSize(int algorithm, int majorProtocolVersion, int minorProtocolVersion) {
        boolean legacy = !AbstractMessage.hasEqualOrGreaterVersion(majorProtocolVersion, minorProtocolVersion, 2, 4);
        if (legacy) return 0;
        switch (algorithm) {
        case HdlSecurityProvider.ENCRYPT_ALG_DES:
            return 8;
        case HdlSecurityProvider.ENCRYPT_ALG_AES:
            return 16;
        case HdlSecurityProvider.ENCRYPT_ALG_DESEDE:
            return 8;
        default:
            return 0;
        }
    }

    private KeyGenerator aesKeygen = null;
    private KeyGenerator desKeygen = null;
    private KeyGenerator desedeKeygen = null;

    /** Generate and encode a secret key for use with the given algorithm */
    @Override
    public byte[] generateSecretKey(int keyAlg) throws Exception {
        KeyGenerator kgen = null;
        synchronized (this) {
            switch (keyAlg) {
            case ENCRYPT_ALG_DES:
                if (desKeygen == null) desKeygen = KeyGenerator.getInstance("DES");
                kgen = desKeygen;
                break;
            case ENCRYPT_ALG_DESEDE:
                if (desedeKeygen == null) desedeKeygen = KeyGenerator.getInstance("DESEDE");
                kgen = desedeKeygen;
                break;
            case ENCRYPT_ALG_AES:
                if (aesKeygen == null) aesKeygen = KeyGenerator.getInstance("AES");
                kgen = aesKeygen;
                break;
            default:
                throw new Exception("Invalid encryption algorithm code: " + keyAlg);
            }
        }
        byte tmp[] = kgen.generateKey().getEncoded();

        // put the encoded key into an array that uses the first four bytes to list
        // the key algorithm
        byte encKey[] = new byte[tmp.length + Encoder.INT_SIZE];
        Encoder.writeInt(encKey, 0, keyAlg);
        System.arraycopy(tmp, 0, encKey, Encoder.INT_SIZE, tmp.length);
        return encKey;
    }

    @Override
    public byte[] getDESKeyFromDH(DHPublicKey pub, DHPrivateKey priv) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("DH");
        ka.init(priv);
        ka.doPhase(pub, true);
        return generateSecretWithAlgorithm(ka, "DES");
    }

    /** Using the given diffie-hellman key pair, generate the secret key with the
     * algorithm ID (ENCRYPT_ALG_DES, ENCRYPT_ALG_AES or ENCRYPT_ALG_DESEDE) in the
     * first four bytes of the array */
    @Override
    public byte[] getKeyFromDH(DHPublicKey pub, DHPrivateKey priv, int algorithm) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("DH");
        ka.init(priv);
        ka.doPhase(pub, true);

        String algStr;
        byte[] rawKey;
        switch (algorithm) {
        case HdlSecurityProvider.ENCRYPT_ALG_DES:
            algStr = "DES";
            rawKey = generateSecretWithAlgorithm(ka, algStr);
            break;
        case HdlSecurityProvider.ENCRYPT_ALG_DESEDE:
            algStr = "DESede";
            rawKey = generateSecretWithAlgorithm(ka, algStr);
            break;
        case HdlSecurityProvider.ENCRYPT_ALG_AES:
            algStr = "AES";
            rawKey = stripLeadingZerosAndTruncate(ka.generateSecret(), 32);
            break;
        default:
            throw new Exception("Unknown algorithm code: " + algorithm);
        }
        byte key[] = new byte[rawKey.length + Encoder.INT_SIZE];
        Encoder.writeInt(key, 0, algorithm);
        System.arraycopy(rawKey, 0, key, Encoder.INT_SIZE, rawKey.length);
        return key;
    }

    byte[] generateSecretWithAlgorithm(KeyAgreement ka, String algStr) throws Exception {
        byte[] raw = ka.generateSecret();
        byte[] key;
        if ("DES".equals(algStr)) {
            key = new DESKeySpec(raw).getKey();
        } else if ("DESede".equals(algStr)) {
            key = new DESedeKeySpec(raw).getKey();
        } else {
            throw new Exception("Unknown algorithm code: " + algStr);
        }
        // Set the parity bits. Inspired by JDK code found at:
        // http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b14/com/sun/crypto/provider/DESKeyGenerator.java
        for (int i = 0; i < key.length; i++) {
            int b = key[i] & 0xfe;
            b |= (Integer.bitCount(b) & 1) ^ 1;
            key[i] = (byte)b;
        }
        return key;
    }

    private static byte[] stripLeadingZerosAndTruncate(byte[] secret, int length) {
        int i = 0;
        while (i + length < secret.length && secret[i] == 0) {
            i++;
        }
        return Util.substring(secret, i, i + length);
    }

    @Override
    public KeyPair generateDHKeyPair(int keySize) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        kpg.initialize(keySize);
        KeyPair kp = kpg.generateKeyPair();
        return kp;
    }

    @Override
    public KeyPair generateDHKeyPair(BigInteger p, BigInteger g) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        kpg.initialize(new DHParameterSpec(p, g));
        KeyPair kp = kpg.generateKeyPair();
        return kp;
    }

}
