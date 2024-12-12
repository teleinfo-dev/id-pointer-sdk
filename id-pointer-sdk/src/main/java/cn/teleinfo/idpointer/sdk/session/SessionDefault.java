package cn.teleinfo.idpointer.sdk.session;

import cn.teleinfo.idpointer.sdk.core.ValueReference;
import cn.teleinfo.idpointer.sdk.security.HdlSecurityProvider;
import cn.teleinfo.idpointer.sdk.session.v3.Session;

public class SessionDefault implements Session {
    private int sessionId = -1;
    private ValueReference idUserId;
    private boolean encryptMessage = false;

    private byte sessionKey[];
    private int sessionKeyAlgorithmCode;

    public SessionDefault(int sessionId) {
        this.sessionId = sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public void setIdUserId(ValueReference idUserId) {
        this.idUserId = idUserId;
    }

    public void setEncryptMessage(boolean encryptMessage) {
        this.encryptMessage = encryptMessage;
    }

    public void setSessionKey(byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }

    public void setSessionKeyAlgorithmCode(int sessionKeyAlgorithmCode) {
        switch (sessionKeyAlgorithmCode) {
            case HdlSecurityProvider.ENCRYPT_ALG_AES:
            case HdlSecurityProvider.ENCRYPT_ALG_DES:
            case HdlSecurityProvider.ENCRYPT_ALG_DESEDE:
            case HdlSecurityProvider.ENCRYPT_ALG_SM4:
                //        encryptCipher = null;
                //        decryptCipher = null;
                //this.sessionKeyAlgorithmCode = sessionKeyAlgorithmCode;
                break;
            default:
                throw new IllegalArgumentException("Invalid algorithm ID: " + sessionKeyAlgorithmCode);
        }
        this.sessionKeyAlgorithmCode = sessionKeyAlgorithmCode;
    }

    public int getSessionId() {
        return sessionId;
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }

    public boolean isEncryptMessage() {
        return encryptMessage;
    }


    public boolean isAuthenticated() {
        return idUserId != null;
    }

    public int getSessionKeyAlgorithmCode() {
        return sessionKeyAlgorithmCode;
    }


}
