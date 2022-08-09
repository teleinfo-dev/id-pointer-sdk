/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import cn.teleinfo.idpointer.sdk.security.HdlSecurityProvider;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 *  Class for passing session options to HandleResolver.
 *
 *  keyExchangeMode indicates what key exchange method to use:
 *  KEY_EXCHANGE_NONE - No session
 *  KEY_EXCHANGE_CIPHER_CLIENT - Exchange key is encrypted with client's
 *                               asymmetric key.  Requires RSA.
 *  KEY_EXCHANGE_CIPHER_SERVER - Exchange key is encrypted with server's
 *                               asymmetric key, which should be stored in
 *                               the NA handle.  Requires RSA.
 *  KEY_EXCHANGE_DH - Use diffie-hellman key exchange
 *
 *  The public variables used depend on the key mode.  Polymorphism might be
 *  a cleaner way to go than the different modes, but this should be simple
 *  enough to rely on delegation.
 *
 *  The different constructors can be used as shortcuts for particular modes.
 **/

public class SessionSetupInfo {
    public int keyExchangeMode = 0;

    public byte[] exchangeKeyHandle = null; //the handle, index pair to determine the public key used to encrypt the secret key
    public int exchangeKeyIndex = -1;

    public byte[] publicExchangeKey = null; //user can have a public key to be used for encryption
    public PrivateKey privateExchangeKey = null; //the private key for decrypting the session key

    //define time out span, time in seconds
    public int timeout = Common.DEFAULT_SESSION_TIMEOUT;

    public boolean encrypted; // encrypt all traffic
    public boolean authenticated; // authenticate all traffic

    //this constrcutor is used for handle ref as public key
    public SessionSetupInfo(byte exchangeHandle[], int exchangeIndex, PrivateKey privateKey) {
        this.exchangeKeyHandle = exchangeHandle;
        this.exchangeKeyIndex = exchangeIndex;
        this.publicExchangeKey = null;
        this.privateExchangeKey = privateKey;
        this.keyExchangeMode = Common.KEY_EXCHANGE_CIPHER_HDL;
    }

    //this constrcutor is used for handle ref as public key
    public SessionSetupInfo(String exchangeHandle, int exchangeIndex, PrivateKey privateKey) {
        this.exchangeKeyHandle = Util.encodeString(exchangeHandle);
        this.exchangeKeyIndex = exchangeIndex;
        this.publicExchangeKey = null;
        this.privateExchangeKey = privateKey;
        this.keyExchangeMode = Common.KEY_EXCHANGE_CIPHER_HDL;
    }

    // Default mode of KEY_EXCHANGE_DH
    /**
     * Constructs a SessionSetupInfo.  The authentication info is not used.
     *
     * @deprecated Use {@code new SessionSetupInfo()} instead.
     * @param authInfo ignored
     */
    @Deprecated
    public SessionSetupInfo(AuthenticationInfo authInfo) {
        this.exchangeKeyHandle = null;
        this.exchangeKeyIndex = -1;
        this.keyExchangeMode = Common.KEY_EXCHANGE_DH;
    }

    // Default mode of KEY_EXCHANGE_DH
    public SessionSetupInfo() {
        this.exchangeKeyHandle = null;
        this.exchangeKeyIndex = -1;
        this.keyExchangeMode = Common.KEY_EXCHANGE_DH;
    }

    // this constructor is used for byte array as public key
    // KEY_EXCHANGE_CIPHER_CLIENT, KEY_EXCHANGE_DH
    public SessionSetupInfo(int mode, byte exchangekey[], PrivateKey privateKey) {
        this.exchangeKeyHandle = null;
        this.exchangeKeyIndex = -1;
        this.publicExchangeKey = exchangekey;
        this.privateExchangeKey = privateKey;
        this.keyExchangeMode = mode;
    }

    // KEY_EXCHANGE_CIPHER_CLIENT, KEY_EXCHANGE_DH
    public SessionSetupInfo(int mode, PublicKey exchangekey, PrivateKey privateKey) throws Exception {
        this.exchangeKeyHandle = null;
        this.exchangeKeyIndex = -1;
        this.publicExchangeKey = Util.getBytesFromPublicKey(exchangekey);
        this.privateExchangeKey = privateKey;
        this.keyExchangeMode = mode;
    }

    // this constructor is used primarily for KEY_EXCHANGE_CIPHER_SERVER
    public SessionSetupInfo(int mode) {
        this.keyExchangeMode = mode;
    }

    public void reset() {
        this.exchangeKeyHandle = null;
        this.exchangeKeyIndex = -1;
        this.publicExchangeKey = null;
        this.keyExchangeMode = 0;
    }

    @Override
    public String toString() {
        String str = "";
        str += "exchange key usage " + keyExchangeMode;
        return str;

    }

    public synchronized void initDHKeys() throws HandleException {
        if (this.publicExchangeKey != null || keyExchangeMode != Common.KEY_EXCHANGE_DH) return;
        try {
            KeyPair kp = HdlSecurityProvider.getInstance().generateDHKeyPair(1024);
            this.publicExchangeKey = Util.getBytesFromPublicKey(kp.getPublic());
            this.privateExchangeKey = kp.getPrivate();
        } catch (Exception e) {
            throw new HandleException(HandleException.ENCRYPTION_ERROR, "Unable to initialize Diffie-Hellman keyPair for session", e);
        }
    }
}
