/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
 All rights reserved.

 The HANDLE.NET software is made available subject to the
 Handle.Net Public License Agreement, which may be obtained at
 http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
 \**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/****************************************************************
 * Base class for all request types
 ****************************************************************/

@Slf4j
public abstract class AbstractMessage implements Cloneable {
    // message types...(opCode)
    public static final int OC_RESERVED = 0;
    public static final int OC_RESOLUTION = 1;
    public static final int OC_GET_SITE_INFO = 2;

    public static final int OC_CREATE_HANDLE = 100;
    public static final int OC_DELETE_HANDLE = 101;
    public static final int OC_ADD_VALUE = 102;
    public static final int OC_REMOVE_VALUE = 103;
    public static final int OC_MODIFY_VALUE = 104;
    public static final int OC_LIST_HANDLES = 105;

    public static final int OC_RESPONSE_TO_CHALLENGE = 200;
    public static final int OC_VERIFY_CHALLENGE = 201;

    public static final int OC_HOME_NA = 300;
    public static final int OC_UNHOME_NA = 301;
    public static final int OC_LIST_HOMED_NAS = 302;

    public static final int OC_SESSION_SETUP = 400;
    public static final int OC_SESSION_TERMINATE = 401;
    public static final int OC_SESSION_EXCHANGEKEY = 402;

    public static final int OC_GET_NEXT_TXN_ID = 1000;
    public static final int OC_RETRIEVE_TXN_LOG = 1001;
    public static final int OC_DUMP_HANDLES = 1002;
    public static final int OC_BACKUP_SERVER = 1003;

    // idis 登录自定义
    public static final int OC_LOGIN_ID_SYSTEM = 2001;

    // response codes... (responseCode)
    public static final int RC_RESERVED = 0; // (only used for requests)
    public static final int RC_SUCCESS = 1;
    public static final int RC_ERROR = 2;
    public static final int RC_SERVER_TOO_BUSY = 3;
    public static final int RC_PROTOCOL_ERROR = 4;
    public static final int RC_OPERATION_NOT_SUPPORTED = 5;
    public static final int RC_RECURSION_COUNT_TOO_HIGH = 6;
    public static final int RC_SERVER_BACKUP = 7;

    public static final int RC_HANDLE_NOT_FOUND = 100;
    public static final int RC_HANDLE_ALREADY_EXISTS = 101;
    public static final int RC_INVALID_HANDLE = 102;

    public static final int RC_VALUES_NOT_FOUND = 200;
    public static final int RC_VALUE_ALREADY_EXISTS = 201;
    public static final int RC_INVALID_VALUE = 202;

    public static final int RC_OUT_OF_DATE_SITE_INFO = 300;
    public static final int RC_SERVER_NOT_RESP = 301;
    public static final int RC_SERVICE_REFERRAL = 302;
    public static final int RC_PREFIX_REFERRAL = 303; // formerly RC_NA_DELEGATE

    public static final int RC_INVALID_ADMIN = 400; // requestor not an administrator for the operation
    public static final int RC_INSUFFICIENT_PERMISSIONS = 401; // requestor doesn't have permission
    public static final int RC_AUTHENTICATION_NEEDED = 402; // no auth info provided, but necessary
    public static final int RC_AUTHENTICATION_FAILED = 403; // couldn't verify requestor identity
    public static final int RC_INVALID_CREDENTIAL = 404; // requestor auth info is invalid
    public static final int RC_AUTHEN_TIMEOUT = 405; // got response after challenge timed out
    public static final int RC_AUTHEN_ERROR = 406; // unexpected error trying to authenticate (catch-all)

    public static final int RC_SESSION_TIMEOUT = 500;
    public static final int RC_SESSION_FAILED = 501;
    public static final int RC_INVALID_SESSION_KEY = 502;
    public static final int RC_NEED_RSAKEY_FOR_SESSIONEXCHANGE = 503;
    public static final int RC_INVALID_SESSIONSETUP_REQUEST = 504; //server is down, then session info is gone, whie client info is still there
    public static final int RC_SESSION_MESSAGE_REJECTED = 505; // used by replay-defense if a duplicate message is detected

    public static final int RC_CLIENT_CHANNEL_ERROR = 601;
    public static final int RC_CLIENT_TIME_OUT = 602;
    public static final int RC_REQUEST_LIMIT_DAILY = 603;
    public static final int RC_REGISTER_COUNT_LIMIT = 604;
    public static final int RC_PREFIX_LIMIT = 605;

    //public static final int RC_AUTHENTICATION_ERROR = 7;

    public int requestId = -1;
    public int sessionId = 0;

    /**
     * The major version of the protocol used to send this message.
     * This is only valid when the message has been decoded from the network
     * using the Encoder.decodeEnvelope and Encoder.decodeMessage methods.
     */
    public byte majorProtocolVersion = Common.COMPATIBILITY_MAJOR_VERSION; //-1;

    /**
     * the minor version of the protocol used to send this message.
     * This is only valid when the message has been decoded from the network
     * using the Encoder.decodeEnvelope and Encoder.decodeMessage methods.
     */
    public byte minorProtocolVersion = Common.COMPATIBILITY_MINOR_VERSION; //-1;

    public byte suggestMajorProtocolVersion = Common.MAJOR_VERSION;
    public byte suggestMinorProtocolVersion = Common.MINOR_VERSION;

    public int opCode;
    public int responseCode = RC_RESERVED;
    public int siteInfoSerial = -1;
    public int expiration; // time (in seconds from epoch) when message is no longer valid
    public short recursionCount = 0;

    public boolean certify = false;
    public boolean cacheCertify = true;
    public boolean authoritative = false;
    public boolean encrypt = false;
    public boolean ignoreRestrictedValues = true;
    public boolean returnRequestDigest = false;

    public boolean recursive = true; // Indicates whether handle servers should recursively
    // process handle resolution if they are not responsible
    // for the requested handle.  The server is not
    // required to honor this.

    public boolean continuous = false; // Indicates whether this message is to-be-continued

    public boolean keepAlive = false; // Indicates that the client would like the server to
    // keep the current connection open for more requests
    // the server is not required to honor this.

    public boolean overwriteWhenExists = false; // for Create or Add Value messages, if true overwrite when exists
    public boolean mintNewSuffix = false; //used in create request. Asks server to mint a new suffix
    public boolean doNotRefer = false; // request server not to send referral response

    public boolean recursionAuth = false; // request transfer user auth info to referred server,递归传输身份

    public byte signerHdl[] = null; // currently unused
    public int signerHdlIdx = 0; // currently unused

    public byte messageBody[] = null;
    public byte signature[] = null;
    public byte encodedMessage[] = null;
    public byte requestDigest[] = null; // the digest of the request, if this is a response
    public byte rdHashType = Common.HASH_CODE_SHA1;

    public int sessionCounter = 0; // counter for messages in a session to prevent replay attacks; set in signMessage and verifyMessage

    public AbstractMessage() {
        // default expiration is twelve hours from now
        expiration = (int) (System.currentTimeMillis() / 1000) + 43200;
    }

    public AbstractMessage(int opCode) {
        this.opCode = opCode;

        // default expiration is twelve hours from now
        expiration = (int) (System.currentTimeMillis() / 1000) + 43200;
    }

    @Override
    protected AbstractMessage clone() {
        try {
            return (AbstractMessage) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public void setSupportedProtocolVersion(AbstractMessage message) {
        this.majorProtocolVersion = message.suggestMajorProtocolVersion;
        this.minorProtocolVersion = message.suggestMinorProtocolVersion;
        this.setSupportedProtocolVersion();
    }

    public void setSupportedProtocolVersion(SiteInfo site) {
        this.majorProtocolVersion = site.majorProtocolVersion;
        this.minorProtocolVersion = site.minorProtocolVersion;
        this.setSupportedProtocolVersion();
    }

    public void setSupportedProtocolVersion() {
        if (hasEqualOrGreaterVersion(Common.MAJOR_VERSION, Common.MINOR_VERSION)) {
            majorProtocolVersion = Common.MAJOR_VERSION;
            minorProtocolVersion = Common.MINOR_VERSION;
        }
    }

    /**
     * Return true if the major and minor version of this message is equal to or
     * greater than the given major/minor versions.
     */
    public boolean hasEqualOrGreaterVersion(int majorVersion, int minorVersion) {
        if (majorProtocolVersion == 5)
            return majorVersion == 5 && minorProtocolVersion >= minorVersion; // apparently really old client used 5 by mistake
        if (majorVersion == 5) return true;
        if (majorProtocolVersion > majorVersion) return true;
        if (majorProtocolVersion < majorVersion) return false;
        return minorProtocolVersion >= minorVersion;
    }

    public static boolean hasEqualOrGreaterVersion(int majorProtocolVersion, int minorProtocolVersion, int majorVersion, int minorVersion) {
        if (majorProtocolVersion == 5)
            return majorVersion == 5 && minorProtocolVersion >= minorVersion; // apparently really old client used 5 by mistake
        if (majorVersion == 5) return true;
        if (majorProtocolVersion > majorVersion) return true;
        if (majorProtocolVersion < majorVersion) return false;
        return minorProtocolVersion >= minorVersion;
    }

    /********************************************************************************
     * Takes the request parameters (certify, cacheCertify, authoritative, and
     * encrypt) from the given request object.  This is useful when several queries
     * are required to resolve a handle and all of the queries must have the same
     * parameters as the overall request.  This is also useful when generating
     * responses to requests that must contain the same flags as the request.
     ********************************************************************************/
    public void takeValuesFrom(AbstractMessage msg) {
        this.certify = msg.certify;
        this.cacheCertify = msg.cacheCertify;
        this.authoritative = msg.authoritative;
        this.encrypt = msg.encrypt;
        this.ignoreRestrictedValues = msg.ignoreRestrictedValues;
        this.doNotRefer = msg.doNotRefer;
        this.recursionCount = msg.recursionCount;
        this.returnRequestDigest = this.returnRequestDigest || msg.returnRequestDigest;
        this.majorProtocolVersion = msg.majorProtocolVersion;
        this.minorProtocolVersion = msg.minorProtocolVersion;
        this.suggestMajorProtocolVersion = msg.suggestMajorProtocolVersion;
        this.suggestMinorProtocolVersion = msg.suggestMinorProtocolVersion;
    }

    /*******************************************************************************
     * Generate a MAC code with a given secretKey.
     * secretKey is a symetric key. The secret keyexchange before hand has to be
     * secured with RSA keys. See HandleResolver.java.
     *
     * One usage now is to sign message with MAC code in session menagement.
     * <pre>
     *
     * Credential section (including signature) within each message:
     *
     *    Version        octet        (always be 0)
     *
     *    Reserved    octet        (set to 0)
     *
     *    Flags        int2
     *
     *    Signer      Handle              0 length UTF8 string
     *                index               was sesion id (speicified in message envelop) ... replaced by session counter
     *
     *    SignatureType   UTF8String      (the handle type of the signature - HS_MAC)
     *    SignatureLength int4            (length of signature section)
     *    SignatureSection                (the bytes of the signature, dependent on SignatureType)
     *
     *
     * SignatureSection for MAC signatures:
     *    DigestAlg    UTF8String      (e.g. SHA-1)
     *    ContentInfo    int4, signedData (version {@literal <=} 2.4: Hash on session key + message header + message body + session key;
     *                  version {@literal >=} 2.5, int4 sessionCounter follower by Hash on session key + protocolMajorVersion + protocolMinorVersion + session id + request id + session counter +
     *                                                                      message header + message body + session key)
     *
     *
     * </pre>
     * Call verifyMessage(byte[] secretKey) to verify.
     * Also see method signMessage(Signature signer), signMessage(PrivateKey key).
     *******************************************************************************/
    public final void signMessage(byte[] secretKey) throws HandleException {
        boolean newVersion = hasEqualOrGreaterVersion(2, 7);
        boolean brokenVersion = !newVersion && hasEqualOrGreaterVersion(2, 5);

        //getEncodedMessageBody() will return the "message header + message body"
        byte[] messageHeaderAndBody = getEncodedMessageBody();

        byte sigType[] = Common.CREDENTIAL_TYPE_MAC;
        byte sigHashType[];

        if (hasEqualOrGreaterVersion(2, 7)) {
            sigHashType = Common.HASH_ALG_HMAC_SHA256;
        } else if (hasEqualOrGreaterVersion(2, 6)) {
            sigHashType = Common.HASH_ALG_SHA1;
        } else {
            sigHashType = new byte[1];
            sigHashType[0] = Common.HASH_CODE_SHA1; //use SHA 1 for hash
        }

        byte[] signatureBytes;
        if (brokenVersion) {
            signatureBytes = brokenTwoFiveTwoSixSignature(sigHashType, messageHeaderAndBody, secretKey);
        } else {
            int tobeMACedLength = messageHeaderAndBody.length;
            if (newVersion) tobeMACedLength += 2 + 3 * Encoder.INT_SIZE;
            boolean hasSuggest = hasEqualOrGreaterVersion(2, 8);
            if (hasSuggest) tobeMACedLength += 2;
            byte[] tobeMACed = new byte[tobeMACedLength];
            int offset = 0;
            if (newVersion) {
                tobeMACed[offset++] = majorProtocolVersion;
                tobeMACed[offset++] = minorProtocolVersion;
                if (hasSuggest) {
                    tobeMACed[offset++] = suggestMajorProtocolVersion;
                    tobeMACed[offset++] = suggestMinorProtocolVersion;
                }
                offset += Encoder.writeInt(tobeMACed, offset, sessionId);
                offset += Encoder.writeInt(tobeMACed, offset, requestId);
                offset += Encoder.writeInt(tobeMACed, offset, sessionCounter);
            }
            System.arraycopy(messageHeaderAndBody, 0, tobeMACed, offset, messageHeaderAndBody.length);

            signatureBytes = Util.doMac(sigHashType, tobeMACed, secretKey);
        }
        int offset = 0;
        int signatureLength = 1 + // version - 1 octet
                1 + // reserved - 1 octet
                2 + // flags - 2 octets, options
                Encoder.INT_SIZE + sigType.length + // signature type length + bytes
                Encoder.INT_SIZE + // signature section length
                Encoder.INT_SIZE * 2 + // signer hdl length and signer hdl index - 8 octets
                Encoder.INT_SIZE + // signature length field - 4 octets
                signatureBytes.length + // signature bytes
                Encoder.INT_SIZE + // signature hash type length field - 4 octets
                sigHashType.length; // signature type bytes
        signature = new byte[signatureLength];
        signature[offset++] = 0; // version
        signature[offset++] = 0; // reserved
        offset += Encoder.writeInt2(signature, offset, 0); // flags

        offset += Encoder.writeByteArray(signature, offset, null); // 0 length string
        offset += Encoder.writeInt(signature, offset, sessionCounter); // was session id but was unused, replaced by session counter

        // the type of the signature (e.g. HS_MAC)
        offset += Encoder.writeByteArray(signature, offset, sigType);

        // length of the signature section...
        offset += Encoder.writeInt(signature, offset, sigHashType.length + Encoder.INT_SIZE + signatureBytes.length + Encoder.INT_SIZE);

        // signature type length + bytes
        offset += Encoder.writeByteArray(signature, offset, sigHashType);

        // signature length + bytes
        offset += Encoder.writeByteArray(signature, offset, signatureBytes);

        // force a re-encoding of the body+signature part of the message.
        encodedMessage = null;
    }

    private byte[] brokenTwoFiveTwoSixSignature(byte[] sigHashType, byte[] messageHeaderAndBody, byte[] secretKey) throws HandleException {
        int tobeMACedLength = 2 * secretKey.length + messageHeaderAndBody.length;
        tobeMACedLength += 2 + 3 * Encoder.INT_SIZE;
        byte[] tobeMACed = new byte[tobeMACedLength];
        System.arraycopy(secretKey, 0, tobeMACed, 0, secretKey.length);
        int offset = secretKey.length;
        tobeMACed[offset++] = majorProtocolVersion;
        tobeMACed[offset++] = minorProtocolVersion;
        offset = Encoder.writeInt(tobeMACed, offset, sessionId);
        offset = Encoder.writeInt(tobeMACed, offset, requestId);
        offset = Encoder.writeInt(tobeMACed, offset, sessionCounter);
        System.arraycopy(messageHeaderAndBody, 0, tobeMACed, offset, messageHeaderAndBody.length);
        System.arraycopy(secretKey, 0, tobeMACed, offset + messageHeaderAndBody.length, secretKey.length);
        return Util.doDigest(sigHashType, tobeMACed);
    }

    /****************************************************************************
     * Generate a signature for this message using the given Signature object.
     * The Signature object must already have been initialized for signing.
     * There can be more than one type of message signature, but this
     * implementation can currently only handle HS_SIGNED-based signatures.
     * <pre>
     *
     * Credential section (including signature) within each message:
     *
     *    Version        octet        (always be 0)
     *
     *    Reserved    octet        (set to 0)
     *
     *    Flags        int2
     *
     *    Signer          HdlValueRef     (place holder in hdl authentication)
     *                      (note: HdlValueRef: UTF8String:int4)
     *
     *    SignatureType   UTF8String      (the handle type of the signature - HS_SIGNED, etc)
     *    SignatureLength int4            (length of signature section)
     *    SignatureSection                (the bytes of the signature, dependent on SignatureType)
     *
     *
     * SignatureSection for HS_SIGNED signatures:
     *    DigestAlg    UTF8String      (e.g. SHA-1)
     *    ContentInfo    int4, signedData
     *
     *
     * </pre>
     *******************************************************************************/
    public final void signMessage(Signature signer) throws HandleException, SignatureException {
        if (hasEqualOrGreaterVersion(2, 6)) {
            boolean newVersion = hasEqualOrGreaterVersion(2, 7);
            boolean hasSuggest = hasEqualOrGreaterVersion(2, 8);
            int versionBytes = hasSuggest ? 4 : 2;
            byte[] toBeSigned = new byte[versionBytes + 3 * Encoder.INT_SIZE]; // fields from envelope and mac
            int offset = 0;
            toBeSigned[offset++] = majorProtocolVersion;
            toBeSigned[offset++] = minorProtocolVersion;
            if (hasSuggest) {
                toBeSigned[offset++] = suggestMajorProtocolVersion;
                toBeSigned[offset++] = suggestMinorProtocolVersion;
            }
            if (newVersion) {
                offset += Encoder.writeInt(toBeSigned, offset, sessionId);
                offset += Encoder.writeInt(toBeSigned, offset, requestId);
                offset += Encoder.writeInt(toBeSigned, offset, sessionCounter);
            } else {
                // painful bug
                offset = Encoder.writeInt(toBeSigned, offset, sessionId);
                offset = Encoder.writeInt(toBeSigned, offset, requestId);
                offset = Encoder.writeInt(toBeSigned, offset, sessionCounter);
            }
            signer.update(toBeSigned);
        }

        signer.update(getEncodedMessageBody());
        byte sigType[] = Common.CREDENTIAL_TYPE_SIGNED;
        byte sigHashType[] = Util.getHashAlgIdFromSigId(signer.getAlgorithm());

        byte signatureBytes[] = signer.sign();
        int offset = 0;
        signature = new byte[1 + // version - 1 octet
                1 + // reserved - 1 octet
                2 + // flags - 2 octets
                Encoder.INT_SIZE + sigType.length + // signature type length + bytes
                Encoder.INT_SIZE + // signature section length
                Encoder.INT_SIZE * 2 + // signer hdl length and signer hdl index - 8 octets
                Encoder.INT_SIZE + // signature length field - 4 octets
                signatureBytes.length + // signature bytes
                Encoder.INT_SIZE + // signature hash type length field - 4 octets
                sigHashType.length]; // signature type bytes
        signature[offset++] = 0; // version
        signature[offset++] = 0; // reserved
        offset += Encoder.writeInt2(signature, offset, 0); // flags

        offset += Encoder.writeByteArray(signature, offset, null); // 0 length string
        offset += Encoder.writeInt(signature, offset, sessionCounter); // was session id but was unused, replaced by session counter

        // the type of the signature (e.g. HS_SIGNED)
        offset += Encoder.writeByteArray(signature, offset, sigType);

        // length of the signature section...
        offset += Encoder.writeInt(signature, offset, sigHashType.length + Encoder.INT_SIZE + signatureBytes.length + Encoder.INT_SIZE);

        // after this point, the format depends on the sigType, but we currently only
        // support HS_SIGNED.

        // signature type length + bytes
        offset += Encoder.writeByteArray(signature, offset, sigHashType);

        // signature length + bytes
        offset += Encoder.writeByteArray(signature, offset, signatureBytes);

        // force a re-encoding of the body+signature part of the message.
        encodedMessage = null;
    }

    @SuppressWarnings("unused")
    public boolean signatureIsMac() throws HandleException {
        if (signature == null || signature.length <= 0) {
            return false;
            //throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE);
        }

        int offset = 0;
        byte sigVersion = signature[offset++];
        byte reserved = signature[offset++];
        int flags = Encoder.readInt2(signature, offset);
        offset += Encoder.INT2_SIZE;

        // 0 length UTF8 string, not used
        byte[] emptyByte = Encoder.readByteArray(signature, offset);
        offset += Encoder.INT_SIZE + emptyByte.length;

        // read the session id now
        int sessionCounterNotSavedHereButDuringVerification = Encoder.readInt(signature, offset);
        offset += Encoder.INT_SIZE;

        byte[] sigType = Encoder.readByteArray(signature, offset);
        offset += Encoder.INT_SIZE + sigType.length;

        return Util.equals(sigType, Common.CREDENTIAL_TYPE_MAC);
    }

    /****************************************************************************
     * Validate the signature for this message.  The given Signature object must
     * have been initialized with the public key of the entity that supposedly
     * signed this message.  Returns true if the signature checks out, false
     * if it doesn't.
     ****************************************************************************/
    @SuppressWarnings("unused")
    public final boolean verifyMessage(byte secretKey[]) throws Exception {
        if (signature == null || signature.length <= 0) {
            return false;
            //throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE);
        }

        int offset = 0;
        byte sigVersion = signature[offset++];
        byte reserved = signature[offset++];
        int flags = Encoder.readInt2(signature, offset);
        offset += Encoder.INT2_SIZE;

        // 0 length UTF8 string, not used
        byte[] emptyByte = Encoder.readByteArray(signature, offset);
        offset += Encoder.INT_SIZE + emptyByte.length;

        // read the session id now
        sessionCounter = Encoder.readInt(signature, offset);
        offset += Encoder.INT_SIZE;

        byte[] sigType = Encoder.readByteArray(signature, offset);
        offset += Encoder.INT_SIZE + sigType.length;

        if (!Util.equals(sigType, Common.CREDENTIAL_TYPE_MAC)) {
            throw new HandleException(HandleException.UNKNOWN_ALGORITHM_ID, "Unknown signature type: " + Util.decodeString(sigType));
        }

        int sigSectionLength = Encoder.readInt(signature, offset);
        offset += Encoder.INT_SIZE;

        byte hashAlgBytes[] = Encoder.readByteArray(signature, offset);
        offset += Encoder.INT_SIZE + hashAlgBytes.length;

        byte origDigestBytes[] = Encoder.readByteArray(signature, offset);

        byte[] messageHeaderAndBody = getEncodedMessageBody();
        byte[] verifyDigest;
        if (hasEqualOrGreaterVersion(2, 5) && !hasEqualOrGreaterVersion(2, 7)) {
            verifyDigest = brokenTwoFiveTwoSixSignature(hashAlgBytes, messageHeaderAndBody, secretKey);
        } else {
            byte[] tobeMACed;
            if (!hasEqualOrGreaterVersion(2, 5)) {
                tobeMACed = new byte[messageHeaderAndBody.length];
                System.arraycopy(messageHeaderAndBody, 0, tobeMACed, 0, messageHeaderAndBody.length);
            } else {
                boolean hasSuggest = hasEqualOrGreaterVersion(2, 8);
                int versionBytes = hasSuggest ? 4 : 2;
                tobeMACed = new byte[messageHeaderAndBody.length + versionBytes + 3 * Encoder.INT_SIZE];
                offset = 0;
                tobeMACed[offset++] = majorProtocolVersion;
                tobeMACed[offset++] = minorProtocolVersion;
                if (hasSuggest) {
                    tobeMACed[offset++] = suggestMajorProtocolVersion;
                    tobeMACed[offset++] = suggestMinorProtocolVersion;
                }
                offset += Encoder.writeInt(tobeMACed, offset, sessionId);
                offset += Encoder.writeInt(tobeMACed, offset, requestId);
                offset += Encoder.writeInt(tobeMACed, offset, sessionCounter);
                System.arraycopy(messageHeaderAndBody, 0, tobeMACed, offset, messageHeaderAndBody.length);
            }
            verifyDigest = Util.doMac(hashAlgBytes, tobeMACed, secretKey);
        }
        return Util.equals(origDigestBytes, verifyDigest);
    }

    /****************************************************************************
     * Validate the signature for this message.  The given Signature object must
     * have been initialized with the public key of the entity that supposedly
     * signed this message.  Returns true if the signature checks out, false
     * if it doesn't.
     *
     ***************************************************************************/
    @SuppressWarnings("unused")
    public final boolean verifyMessage(PublicKey pubKey) throws Exception {
        if (signature == null || signature.length <= 0) {
            return false;
        }
        int offset = 0;
        byte sigVersion = signature[offset++];
        byte reserved = signature[offset++];
        int flags = Encoder.readInt2(signature, offset);
        offset += Encoder.INT2_SIZE;

        byte[] disused = Encoder.readByteArray(signature, offset);
        offset += Encoder.INT_SIZE + disused.length;
        sessionCounter = Encoder.readInt(signature, offset);
        offset += Encoder.INT_SIZE;

        byte[] sigType = Encoder.readByteArray(signature, offset);
        offset += Encoder.INT_SIZE + sigType.length;

        if (!Util.equals(sigType, Common.CREDENTIAL_TYPE_SIGNED) && !Util.equals(sigType, Common.CREDENTIAL_TYPE_OLDSIGNED)) {
            throw new HandleException(HandleException.UNKNOWN_ALGORITHM_ID, "Unknown signature type: " + Util.decodeString(sigType));
        }

        int sigSectionLength = Encoder.readInt(signature, offset);
        offset += Encoder.INT_SIZE;

        byte hashAlgBytes[] = Encoder.readByteArray(signature, offset);
        offset += Encoder.INT_SIZE + hashAlgBytes.length;

        byte sigBytes[] = Encoder.readByteArray(signature, offset);

        Signature sig = Signature.getInstance(Util.getSigIdFromHashAlgId(hashAlgBytes, pubKey.getAlgorithm()));

        sig.initVerify(pubKey);
        if (hasEqualOrGreaterVersion(2, 6)) {
            boolean newVersion = hasEqualOrGreaterVersion(2, 7);
            boolean hasSuggest = hasEqualOrGreaterVersion(2, 8);
            int versionBytes = hasSuggest ? 4 : 2;
            byte[] toBeSigned = new byte[versionBytes + 3 * Encoder.INT_SIZE]; // fields from envelope and mac
            offset = 0;
            toBeSigned[offset++] = majorProtocolVersion;
            toBeSigned[offset++] = minorProtocolVersion;
            if (hasSuggest) {
                toBeSigned[offset++] = suggestMajorProtocolVersion;
                toBeSigned[offset++] = suggestMinorProtocolVersion;
            }
            if (newVersion) {
                offset += Encoder.writeInt(toBeSigned, offset, sessionId);
                offset += Encoder.writeInt(toBeSigned, offset, requestId);
                offset += Encoder.writeInt(toBeSigned, offset, sessionCounter);
            } else {
                // painful bug
                offset = Encoder.writeInt(toBeSigned, offset, sessionId);
                offset = Encoder.writeInt(toBeSigned, offset, requestId);
                offset = Encoder.writeInt(toBeSigned, offset, sessionCounter);
            }
            sig.update(toBeSigned);
        }

        sig.update(getEncodedMessageBody());
        return sig.verify(sigBytes);
    }

    public boolean shouldEncrypt() {
        return false;
    }

    /*****************************************************************************
     * Removed all cached copies of the encoded message.  This should be
     * called after every change to the message object so that the message
     * encoding is regenerated the next time it is used.
     *****************************************************************************/
    public void clearBuffers() {
        encodedMessage = null;
        signature = null;
        messageBody = null;
    }

    /********************************************************************************
     * Encode (if necessary) and retrieve the header and body portion of this
     * message.  This will leave the encoded value laying around (in the messageBody
     * field) for later use.
     *******************************************************************************/
    public final byte[] getEncodedMessageBody() throws HandleException {
        if (messageBody != null) return messageBody;
        return (messageBody = Encoder.encodeMessage(this));
    }

    /*******************************************************************************
     * Get the encoded value of this message.  Since this object is stupid
     * when it comes to caching the encoded value (for the sake of speed),
     * applications need to make sure that they set the rawMessage and
     * signature fields to null after changing any of the messages fields.
     * This is NOT thread-safe.  If you want to make it thread-safe,
     * synchronize this method.
     *******************************************************************************/
    public final byte[] getEncodedMessage() throws HandleException {
        // return the message if it has already been generated...
        if (encodedMessage != null) return encodedMessage;

        // generate the messageBody field, if it hasn't already been generated...
        getEncodedMessageBody();

        // concatenate the message body and the sig (if any) and return the result
        encodedMessage = new byte[messageBody.length + Encoder.INT_SIZE + (signature == null ? 0 : signature.length)];
        System.arraycopy(messageBody, 0, encodedMessage, 0, messageBody.length);
        if (signature == null) {
            Encoder.writeInt(encodedMessage, messageBody.length, 0);
        } else {
            Encoder.writeInt(encodedMessage, messageBody.length, signature.length);
            System.arraycopy(signature, 0, encodedMessage, messageBody.length + Encoder.INT_SIZE, signature.length);
        }

        return encodedMessage;
    }

    @Override
    public String toString() {
        return "version=" + ((int) majorProtocolVersion) + '.' + ((int) minorProtocolVersion) + "; oc=" + opCode + "; rc=" + responseCode + "; snId=" + sessionId +
                "; rqId="+requestId+
                (certify ? " crt" : "") + (cacheCertify ? " caCrt" : "") + (authoritative ? " auth" : "") + (continuous ? " cont'd" : "") + (encrypt ? " encrypt" : "") + (ignoreRestrictedValues ? " noAuth" : "")
                + (overwriteWhenExists ? " overwriteWhenExists" : "")
                + (mintNewSuffix ? " mintNewSuffix" : "")
                + (doNotRefer ? " doNotRefer" : "")
                + (expiration != 0 ? (" expires:" + new java.util.Date(expiration * 1000l)) : "");
    }

    public static final String getResponseCodeMessage(int responseCode) {
        switch (responseCode) {
            case RC_RESERVED:
                return "RC_RESERVED";
            case RC_SUCCESS:
                return "SUCCESS";
            case RC_ERROR:
                return "ERROR";
            case RC_SERVER_TOO_BUSY:
                return "SERVER TOO BUSY";
            case RC_PROTOCOL_ERROR:
                return "PROTOCOL ERROR";
            case RC_OPERATION_NOT_SUPPORTED:
                return "OPERATION NOT SUPPORTED";
            case RC_RECURSION_COUNT_TOO_HIGH:
                return "RECURSION COUNT TOO HIGH";
            case RC_HANDLE_NOT_FOUND:
                return "HANDLE NOT FOUND";
            case RC_HANDLE_ALREADY_EXISTS:
                return "HANDLE ALREADY EXISTS";
            case RC_INVALID_HANDLE:
                return "INVALID HANDLE";
            case RC_VALUES_NOT_FOUND:
                return "VALUES NOT FOUND";
            case RC_VALUE_ALREADY_EXISTS:
                return "VALUE ALREADY EXISTS";
            case RC_INVALID_VALUE:
                return "INVALID VALUE";
            case RC_OUT_OF_DATE_SITE_INFO:
                return "OUT OF DATE SITE INFO";
            case RC_SERVER_NOT_RESP:
                return "SERVER NOT RESPONSIBLE FOR HANDLE";
            case RC_SERVICE_REFERRAL:
                return "SERVICE REFERRAL";
            case RC_INVALID_ADMIN:
                return "INVALID ADMIN";
            case RC_INSUFFICIENT_PERMISSIONS:
                return "INSUFFICIENT PERMISSIONS";
            case RC_AUTHENTICATION_NEEDED:
                return "AUTHENTICATION NEEDED";
            case RC_AUTHENTICATION_FAILED:
                return "AUTHENTICATION FAILED";
            case RC_INVALID_CREDENTIAL:
                return "INVALID CREDENTIAL";
            case RC_AUTHEN_TIMEOUT:
                return "AUTHENTICATION TIMEOUT";
            case RC_AUTHEN_ERROR:
                return "AUTHENTICATION ERROR";
            case RC_SESSION_TIMEOUT:
                return "SESSION TIMEOUT";
            case RC_SESSION_FAILED:
                return "SESSION FAILED";
            case RC_INVALID_SESSION_KEY:
                return "INVALID SESSION KEY";
            case RC_SERVER_BACKUP:
                return "SERVER BACKUP/MAINTAIN";
            case RC_NEED_RSAKEY_FOR_SESSIONEXCHANGE:
                return "REQUIRE RSA KEY FOR SESSION EXCHANGE";
            case RC_INVALID_SESSIONSETUP_REQUEST:
                return "INVALID SESSION REQUEST";
            case RC_SESSION_MESSAGE_REJECTED:
                return "SESSION MESSAGE REJECTED";
            case RC_PREFIX_REFERRAL:
                return "PREFIX REFERRAL";
            case RC_CLIENT_CHANNEL_ERROR:
                return "PROXY FAILED";
            case RC_CLIENT_TIME_OUT:
                return "PROXY FAILED";
            case RC_REQUEST_LIMIT_DAILY:
                return "REQUEST_LIMIT_DAILY";
            case RC_REGISTER_COUNT_LIMIT:
                return "REGISTER_COUNT_LIMIT";
            case RC_PREFIX_LIMIT:
                return "RC_PREFIX_LIMIT";
            default:
                return "??";
        }
    }
}
