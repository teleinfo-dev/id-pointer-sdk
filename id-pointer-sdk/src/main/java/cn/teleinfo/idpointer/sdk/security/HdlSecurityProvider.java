/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.security;

import cn.teleinfo.idpointer.sdk.security.provider.GenericProvider;

import javax.crypto.Cipher;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("unused")
public abstract class HdlSecurityProvider {
    public static final int ENCRYPT_ALG_DES = 1;
    public static final int ENCRYPT_ALG_DESEDE = 2;
    public static final int ENCRYPT_ALG_AES = 3;
    public static final int ENCRYPT_ALG_SM4 = 4;

    private static HdlSecurityProvider instance = null;

    static final String DEFAULT_PROVIDER = "net.handle.security.provider.GenericProvider";

    public static final HdlSecurityProvider getInstance() {
        if (instance != null) {
            return instance;
        }
        String clssName = System.getProperty("handle.security.provider_class", null);
        if (clssName != null) {
            try {
                Class<?> klass = Class.forName(clssName);
                Object obj = klass.getConstructor().newInstance();
                if (obj instanceof HdlSecurityProvider) {
                    instance = (HdlSecurityProvider) obj;
                    return instance;
                }
                System.err.println("Security provider (" + clssName + ") not found");
            } catch (Exception e) {
                System.err.println("Security provider (" + clssName + ") not found; reason: " + e);
            }
            return null;
        }
        return (instance = new GenericProvider());
    }

    /**
     * @deprecated Use {@link #getCipher(int,byte[],int,byte[],int,int)} in order to specify protocol version.
     */
    @Deprecated
    public Cipher getCipher(int algorithm, byte secretKey[], int direction) throws Exception {
        return getCipher(algorithm, secretKey, direction, null, 2, 0);
    }

    /** Construct and return a Cipher object, initialized to either decrypt or
     * encrypt using the given algorithm and secret key.  The direction parameter
     * must be either Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE.  The algorithm
     * parameter should be one of the HdlSecurityProvider.ENCRYPT_ALG_* constants.
     * iv is the initialization vector, needed for decrypting with CBC.
     */
    public abstract Cipher getCipher(int algorithm, byte secretKey[], int direction, byte[] iv, int majorProtocolVersion, int minorProtocolVersion) throws Exception;

    /** Returns the length in bytes of the initialization vector used by the cipher generated by getCipher().  Returns 0 if no initialization vector. */
    public abstract int getIvSize(int algorithm, int majorProtocolVersion, int minorProtocolVersion);

    /** Generate and encode a secret key for use with the given algorithm */
    public abstract byte[] generateSecretKey(int keyAlg) throws Exception;

    public KeyPair generateDHKeyPair(int keySize) throws Exception {
        throw new NoSuchAlgorithmException("Diffie-Hellman key exchange not supported.");
    }

    public KeyPair generateDHKeyPair(BigInteger p, BigInteger g) throws Exception {
        throw new NoSuchAlgorithmException("Diffie-Hellman key exchange not supported.");
    }

    public byte[] getDESKeyFromDH(DHPublicKey pub, DHPrivateKey priv) throws Exception {
        throw new NoSuchAlgorithmException("Diffie-Hellman key exchange not supported");
    }

    /** Using the given diffie-hellman key pair, generate a secret key with the given
     * algorithm.  The first four bytes of the secret key will identify the algorithm
     * for the secret key (DES, AES, DESede) */
    public byte[] getKeyFromDH(DHPublicKey pub, DHPrivateKey priv, int algorithm) throws Exception {
        throw new NoSuchAlgorithmException("Diffie-Hellman key exchange not supported");
    }

}
