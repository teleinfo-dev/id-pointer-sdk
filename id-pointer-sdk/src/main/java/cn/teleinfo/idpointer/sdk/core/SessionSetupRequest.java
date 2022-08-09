/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/******************************************************************************
 * Request used to setup a new session.  Holds the identity of the client,
 * exchange key (either a public key or a handle/index pair).
 ******************************************************************************/

public class SessionSetupRequest extends AbstractRequest {

    public byte[] identityHandle = null; // the handle, index pair to authenticate client
    public int identityIndex = -1;

    public byte[] exchangeKeyHandle = null; //the handle, index pair to determine the public key used to encrypt the secret key
    public int exchangeKeyIndex = -1;

    public byte[] publicKey = null; //user can have a public key to be used for encryption

    public int timeout = Common.DEFAULT_SESSION_TIMEOUT;
    public boolean encryptAllSessionMsg = false;
    public boolean authAllSessionMsg = false;

    public int keyExchangeMode = Common.KEY_EXCHANGE_NONE;

    /**
     * Empty request.  Caller should initialize.
     **/
    public SessionSetupRequest() {
        super(Common.BLANK_HANDLE, AbstractMessage.OC_SESSION_SETUP, null);
        this.isAdminRequest = false;
    }

    // used for modifying a session attributes, to modify a key ref attribute
    // KEY_EXCHANGE_DH and KEY_EXCHANGE_CIPHER_CLIENT
    public SessionSetupRequest(int mode, byte[] publicKey) {
        super(Common.BLANK_HANDLE, AbstractMessage.OC_SESSION_SETUP, null);
        this.publicKey = publicKey;
        this.isAdminRequest = false;
        keyExchangeMode = mode;
    }

    // used for modifying a session attributes, to modify a public key
    public SessionSetupRequest(byte exchangeKeyHandle[], int exchangeKeyIndex) {
        super(Common.BLANK_HANDLE, AbstractMessage.OC_SESSION_SETUP, null);
        this.exchangeKeyHandle = exchangeKeyHandle;
        this.exchangeKeyIndex = exchangeKeyIndex;
        this.isAdminRequest = false;
        keyExchangeMode = Common.KEY_EXCHANGE_CIPHER_HDL;
    }

    // used for creating a new session request. identityHandle can be null, identityIndex can be -1
    public SessionSetupRequest(byte identityHandle[], int identityIndex, byte exchangeKeyHandle[], int exchangeKeyIndex) {
        super(Common.BLANK_HANDLE, AbstractMessage.OC_SESSION_SETUP, null);
        this.identityHandle = identityHandle;
        this.identityIndex = identityIndex;
        this.exchangeKeyHandle = exchangeKeyHandle;
        this.exchangeKeyIndex = exchangeKeyIndex;
        this.isAdminRequest = false;
        keyExchangeMode = Common.KEY_EXCHANGE_CIPHER_HDL;
    }

    /**
     * used for creating a new session request. identityHandle can be null,
     * @param identityIndex can be -1
     * @param mode KEY_EXCHANGE_CIPHER_CLIENT or KEY_EXCHANGE_DH
     **/
    public SessionSetupRequest(int mode, byte identityHandle[], int identityIndex, byte publicKey[]) {
        super(Common.BLANK_HANDLE, AbstractMessage.OC_SESSION_SETUP, null);
        this.identityHandle = identityHandle;
        this.identityIndex = identityIndex;
        this.publicKey = publicKey;
        this.isAdminRequest = false;
        keyExchangeMode = mode;
    }

    /**
     * use server generated keys
     * @param mode usually KEY_EXCHANGE_CIPHER_SERVER
     **/
    public SessionSetupRequest(int mode, byte identityHandle[], int identityIndex) {
        super(Common.BLANK_HANDLE, AbstractMessage.OC_SESSION_SETUP, null);
        keyExchangeMode = mode;
        this.identityIndex = identityIndex;
        this.identityHandle = identityHandle;
        this.isAdminRequest = false;
    }

    public void setTimeOut(int timeout) {
        if (timeout > 0) {
            this.timeout = timeout;
        }
    }

    /** Returns false because if the resolver tries to setup a session for
      a session setup request it will be a recursive nightmare! */
    public final boolean getShouldInitSession() {
        return false;
    }

}
