/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.util.Arrays;

public class SecretKeyAuthenticationInfo extends AuthenticationInfo {
    private final byte secretKey[];
    private final byte userIdHandle[];
    private final int userIdIndex;

    /** Construct a SecretKeyAuthenticationInfo object using the given user ID
     * handle with the given key at the given handle index.  This does not
     * use a hash of the given password */
    public SecretKeyAuthenticationInfo(byte userIDHandle[], int userIDIndex, byte secretKey[]) {
        this.secretKey = secretKey;
        this.userIdHandle = userIDHandle;
        this.userIdIndex = userIDIndex;
    }

    /** Construct a SecretKeyAuthenticationInfo object using the given user ID
     * handle with the given key at the given handle index.  If the hash
     * parameter is true then the authentication will use a lower case
     * hex encoded copy of the given secret key.  */
    public SecretKeyAuthenticationInfo(byte userIDHandle[], int userIDIndex, byte secretKey[], boolean hash) throws Exception {
        this.secretKey = Encoder.encodeSecretKey(secretKey, hash);
        this.userIdHandle = userIDHandle;
        this.userIdIndex = userIDIndex;
    }

    /***********************************************************************
     * Get the identifier for the type of authentication performed.  In this
     * case, the authentication type is AT_SECRET_KEY.
     ***********************************************************************/
    @Override
    public byte[] getAuthType() {
        return Common.SECRET_KEY_TYPE;
    }

    /***********************************************************************
     * Sign the given nonce and requestDigest given as a challenge to the
     * given request.  The implementation of this method should also probably
     * verify that the client did in fact send the specified request, and
     * that the associated digest is a valid digest of the request.
     * @return a digest of the concatenation of secret key, nonce,
     * requestDigest, and secret key.
     ***********************************************************************/
    @Override
    public byte[] authenticate(ChallengeIdResponse challenge, AbstractIdRequest request) throws HandleException {
        // need to verify that this is actually a digest of the specified request
        byte origMessage[] = request.getEncodedMessageBody();
        byte origDigest[] = Util.doDigest(challenge.rdHashType, origMessage);

        if (!Util.equals(origDigest, challenge.requestDigest)) {
            throw new HandleException(HandleException.SECURITY_ALERT, "Asked to sign unidentified request!");
        }

        // create a hash of the secret key, nonce, and request digest
        byte digestAlg = Common.HASH_CODE_SHA1;
        byte lowerMajorProtocolVersion, lowerMinorProtocolVersion;
        if (challenge.hasEqualOrGreaterVersion(challenge.suggestMajorProtocolVersion, challenge.suggestMinorProtocolVersion)) {
            lowerMajorProtocolVersion = challenge.suggestMajorProtocolVersion;
            lowerMinorProtocolVersion = challenge.suggestMinorProtocolVersion;
        } else {
            lowerMajorProtocolVersion = challenge.majorProtocolVersion;
            lowerMinorProtocolVersion = challenge.minorProtocolVersion;
        }
        boolean oldFormat = !AbstractMessage.hasEqualOrGreaterVersion(lowerMajorProtocolVersion, lowerMinorProtocolVersion, 2, 1);
        if (oldFormat) {
            digestAlg = Common.HASH_CODE_MD5_OLD_FORMAT;
        } else if (AbstractMessage.hasEqualOrGreaterVersion(lowerMajorProtocolVersion, lowerMinorProtocolVersion, 2, 7)) {
            digestAlg = Common.HASH_CODE_PBKDF2_HMAC_SHA1;
        }

        byte digest[] = Util.doMac(digestAlg, Util.concat(challenge.nonce, challenge.requestDigest), secretKey);
        if (oldFormat) {
            return digest;
        } else {
            byte authResponse[] = new byte[digest.length + 1];
            authResponse[0] = digestAlg;
            System.arraycopy(digest, 0, authResponse, 1, digest.length);
            return authResponse;
        }
    }

    /***********************************************************************
     * Get the name of the handle that identifies the user that is
     * represented by this authentication object.
     ***********************************************************************/
    @Override
    public byte[] getUserIdHandle() {
        return userIdHandle;
    }

    /***********************************************************************
     * Get the index of the handle value that identifies this user.
     * The returned index value of the handle that identifies this user
     * should contain a value with a type (public key, secret key, etc)
     * that corresponds to the way that this user is authenticating.
     ***********************************************************************/
    @Override
    public int getUserIdIndex() {
        return userIdIndex;
    }

    /** Return the byte-encoded representation of the secret key. */
    public byte[] getSecretKey() {
        return secretKey;
    }

    @Override
    public String toString() {
        return "secret_key:" + String.valueOf(userIdIndex) + ':' + ((userIdHandle == null) ? "null" : Util.decodeString(userIdHandle));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(secretKey);
        result = prime * result + Arrays.hashCode(userIdHandle);
        result = prime * result + userIdIndex;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SecretKeyAuthenticationInfo other = (SecretKeyAuthenticationInfo) obj;
        if (!Arrays.equals(secretKey, other.secretKey)) return false;
        if (!Arrays.equals(userIdHandle, other.userIdHandle)) return false;
        if (userIdIndex != other.userIdIndex) return false;
        return true;
    }

}
