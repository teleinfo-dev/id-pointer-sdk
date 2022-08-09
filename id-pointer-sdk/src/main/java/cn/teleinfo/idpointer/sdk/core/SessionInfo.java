/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.security.HdlSecurityProvider;

import javax.crypto.Cipher;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionInfo {
    private static int defaultTimeout = Common.DEFAULT_SESSION_TIMEOUT;
    public int timeOut = Common.DEFAULT_SESSION_TIMEOUT;
    public int sessionId;
    public byte sessionKey[];
    public boolean encryptMessage = false;
    public boolean authenticateMessage = false;
    private int algorithmCode;

    private final byte majorProtocolVersion, minorProtocolVersion;

    public byte identityKeyHandle[] = null; //authentication info handle
    public int identityKeyIndex = -1; //authentication info index

    private int lastTransactionTime;

    //  private javax.crypto.Cipher encryptCipher = null; // cached encryption cipher
    //  private javax.crypto.Cipher decryptCipher = null; // cached decryption cipher

    private Set<Integer> seenSessionCounters;
    private int earliestRecordedSeenSessionCounter;

    private final AtomicInteger sessionCounter = new AtomicInteger(1);

    @Deprecated
    public SessionInfo(int sessionid, byte[] sessionkey, byte idenHandle[], int idenIndex, int majorProtocolVersion, int minorProtocolVersion) {
        this(sessionid, sessionkey, idenHandle, idenIndex, HdlSecurityProvider.ENCRYPT_ALG_DES, majorProtocolVersion, minorProtocolVersion);
    }

    public SessionInfo(int sessionid, byte[] sessionkey, byte idenHandle[], int idenIndex, int algorithmCode, int majorProtocolVersion, int minorProtocolVersion) {
        this.identityKeyHandle = idenHandle;
        this.identityKeyIndex = idenIndex;
        this.algorithmCode = algorithmCode;
        this.sessionId = sessionid;
        this.sessionKey = sessionkey;
        this.lastTransactionTime = (int) (System.currentTimeMillis() / 1000);
        this.timeOut = defaultTimeout;
        this.majorProtocolVersion = (byte) majorProtocolVersion;
        this.minorProtocolVersion = (byte) minorProtocolVersion;

        if (AbstractMessage.hasEqualOrGreaterVersion(majorProtocolVersion, minorProtocolVersion, 2, 5)) setUpSeenSessionCounters();
    }

    public int getNextSessionCounter() {
        return sessionCounter.getAndIncrement();
    }

    private void setUpSeenSessionCounters() {
        seenSessionCounters = Collections.newSetFromMap(new LinkedHashMap<Integer, Boolean>() {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<Integer, Boolean> eldest) {
                if (size() < 200) return false;
                Iterator<Integer> iter = keySet().iterator();
                iter.next();
                earliestRecordedSeenSessionCounter = iter.next().intValue();
                return true;
            }
        });
    }

    public void addSessionCounter(@SuppressWarnings("hiding") int sessionCounter, boolean enforceUniqueness) throws HandleException {
        if (seenSessionCounters == null) return;
        if (sessionCounter == 0) return;
        synchronized (seenSessionCounters) {
            if (!enforceUniqueness) {
                seenSessionCounters.add(Integer.valueOf(sessionCounter));
                return;
            }
            if (sessionCounter <= earliestRecordedSeenSessionCounter || !seenSessionCounters.add(Integer.valueOf(sessionCounter))) {
                throw new HandleException(HandleException.DUPLICATE_SESSION_COUNTER, "Duplicate session counter " + sessionCounter + " in session " + sessionId);
            }
        }
    }

    /**
    Return the algorithm that is being used for encryption in this session.
    Codes include HdlSecurityProvider.ENCRYPT_ALG_DES (the default),
    HdlSecurityProvider.ENCRYPT_ALG_DESEDE and HdlSecurityProvider.ENCRYPT_ALG_AES
     */
    public int getEncryptionAlgorithmCode() {
        return this.algorithmCode;
    }

    /**
    Set the algorithm that is to be used for encryption in this session.
    Codes include HdlSecurityProvider.ENCRYPT_ALG_DES (the default),
    HdlSecurityProvider.ENCRYPT_ALG_DESEDE and HdlSecurityProvider.ENCRYPT_ALG_AES
     */
    public void setEncryptionAlgorithmCode(int algCode) {
        switch (algCode) {
        case HdlSecurityProvider.ENCRYPT_ALG_AES:
        case HdlSecurityProvider.ENCRYPT_ALG_DES:
        case HdlSecurityProvider.ENCRYPT_ALG_DESEDE:
            //        encryptCipher = null;
            //        decryptCipher = null;
            this.algorithmCode = algCode;
            break;
        default:
            throw new IllegalArgumentException("Invalid algorithm ID: " + algCode);
        }
    }

    /** Encrypt the given buffer using the session key and algorithm that should
     * have already been set. */
    public byte[] encryptBuffer(byte buf[], int offset, int len) throws HandleException {
        try {
            // create, initialize and cache a new encryption cipher
            HdlSecurityProvider provider = HdlSecurityProvider.getInstance();
            if (provider == null) {
                throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "Encryption/Key generation engine missing");
            }
            Cipher encryptCipher = provider.getCipher(algorithmCode, sessionKey, Cipher.ENCRYPT_MODE, null, majorProtocolVersion, minorProtocolVersion);
            byte[] ciphertext = encryptCipher.doFinal(buf, offset, len);
            boolean legacy = !AbstractMessage.hasEqualOrGreaterVersion(majorProtocolVersion, minorProtocolVersion, 2, 4);
            if (legacy) return ciphertext;
            byte[] iv = encryptCipher.getIV();
            if (iv == null) iv = new byte[0];
            return Util.concat(iv, ciphertext);
        } catch (Exception e) {
            if (e instanceof HandleException) throw (HandleException) e;
            throw new HandleException(HandleException.ENCRYPTION_ERROR, "Error encrypting buffer", e);
        }
    }

    /** Decrypt the given buffer using the session key and algorithm that should
     * have already been set. */
    public byte[] decryptBuffer(byte buf[], int offset, int len) throws HandleException {
        try {
            // create, initialize and cache a new decryption cipher
            HdlSecurityProvider provider = HdlSecurityProvider.getInstance();
            if (provider == null) {
                throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "Encryption/Key generation engine missing");
            }

            boolean legacy = !AbstractMessage.hasEqualOrGreaterVersion(majorProtocolVersion, minorProtocolVersion, 2, 4);
            byte[] iv = null;
            if (!legacy) {
                int ivSize = provider.getIvSize(algorithmCode, majorProtocolVersion, minorProtocolVersion);
                if (ivSize > 0) iv = Util.substring(buf, offset, offset + ivSize);
                offset += ivSize;
                len -= ivSize;
            }
            Cipher decryptCipher = provider.getCipher(algorithmCode, sessionKey, Cipher.DECRYPT_MODE, iv, majorProtocolVersion, minorProtocolVersion);
            return decryptCipher.doFinal(buf, offset, len);
        } catch (Exception e) {
            if (e instanceof HandleException) throw (HandleException) e;
            throw new HandleException(HandleException.ENCRYPTION_ERROR, "Error decrypting buffer", e);
        }
    }

    public boolean isSessionAnonymous() {
        return (identityKeyHandle == null || identityKeyIndex == -1);
    }

    public void setTimeOut(int newTimeout) {
        if (newTimeout > 0) this.timeOut = newTimeout;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(byte[] sessionkey) {
        this.sessionKey = sessionkey;
    }

    public boolean getEncryptedMesssageFlag() {
        return encryptMessage;
    }

    public boolean getAuthenticateMessageFlag() {
        return authenticateMessage;
    }

    public void setEncryptedMesssageFlag(boolean flag) {
        this.encryptMessage = flag;
    }

    public void setAuthenticateMessageFlag(boolean flag) {
        this.authenticateMessage = flag;
    }

    public byte getMajorProtocolVersion() {
        return majorProtocolVersion;
    }

    public byte getMinorProtocolVersion() {
        return minorProtocolVersion;
    }

    public void touch() {
        this.lastTransactionTime = (int) (System.currentTimeMillis() / 1000);
    }

    public final boolean hasExpired() {
        return lastTransactionTime < (System.currentTimeMillis() / 1000 - timeOut);
    }

    public static void setDefaultTimeout(int maxSessionTimeout) {
        defaultTimeout = maxSessionTimeout;
    }

    public static int getDefaultTimeout() {
        return defaultTimeout;
    }

    public int getSessionID() {
        return sessionId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (authenticateMessage ? 1231 : 1237);
        result = prime * result + (encryptMessage ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(identityKeyHandle);
        result = prime * result + identityKeyIndex;
        result = prime * result + sessionId;
        result = prime * result + Arrays.hashCode(sessionKey);
        result = prime * result + timeOut;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SessionInfo other = (SessionInfo) obj;
        if (authenticateMessage != other.authenticateMessage) return false;
        if (encryptMessage != other.encryptMessage) return false;
        if (!Arrays.equals(identityKeyHandle, other.identityKeyHandle)) return false;
        if (identityKeyIndex != other.identityKeyIndex) return false;
        if (sessionId != other.sessionId) return false;
        if (!Arrays.equals(sessionKey, other.sessionKey)) return false;
        if (timeOut != other.timeOut) return false;
        return true;
    }

}
