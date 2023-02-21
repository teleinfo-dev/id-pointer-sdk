/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.SM2;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;

public class PublicKeyAuthenticationInfo extends AuthenticationInfo {
    private final PrivateKey privateKey;
    private final byte userIdHandle[];
    private final int userIdIndex;

    public PublicKeyAuthenticationInfo(byte userIdHandle[], int userIdIndex, PrivateKey privateKey) {
        this.privateKey = privateKey;
        this.userIdHandle = userIdHandle;
        this.userIdIndex = userIdIndex;
    }

    /***********************************************************************
     * Get the identifier for the type of authentication performed.  In this
     * case, the authentication type is AT_SECRET_KEY.
     ***********************************************************************/
    @Override
    public byte[] getAuthType() {
        return Common.PUBLIC_KEY_TYPE;
    }

    /***********************************************************************
     * Sign the given nonce and requestDigest given as a challenge to the
     * given request.  The implementation of this method should also probably
     * verify that the client did in fact send the specified request, and
     * that the associated digest is a valid digest of the request.
     * @return a signature of the concatenation of nonce and requestDigest.
     ***********************************************************************/
    @Override
    public byte[] authenticate(ChallengeResponse challenge, AbstractRequest request) throws HandleException {
        // need to verify that this is actually a digest of the specified request
        byte origDigest[] = Util.doDigest(challenge.rdHashType, request.getEncodedMessageBody());
        if (!Util.equals(origDigest, challenge.requestDigest)) {
            throw new HandleException(HandleException.SECURITY_ALERT, "Asked to sign unidentified request!");
        }

        byte signatureBytes[] = null;
        byte sigHashType[] = null;
        // sign the nonce, digest, and return the result
        try {

            String alg = privateKey.getAlgorithm().trim();

            // todo:ll supports sm2
            // EC use sm2
            if(alg.equals("EC")){
                final SM2 sm2 = new SM2(privateKey,null);
                byte[] dataBytes = concatByteArrays(challenge.nonce, challenge.requestDigest);

                System.out.println("signData:"+HexUtil.encodeHexStr(dataBytes));
                ByteArrayInputStream inputStream = new ByteArrayInputStream(dataBytes);
                String digestHex = SmUtil.sm3(inputStream);
                System.out.println("digestHex:"+digestHex);

                String userId = Util.decodeString(userIdHandle);

                System.out.println("idHex:"+HexUtil.encodeHexStr(userId.getBytes(StandardCharsets.UTF_8)));

                String signatureHex = sm2.signHex(digestHex,HexUtil.encodeHexStr(userId.getBytes(StandardCharsets.UTF_8)));
                System.out.println("signatureHex:"+signatureHex);
                signatureBytes = HexUtil.decodeHex(signatureHex);
                sigHashType = Common.HASH_ALG_SM3;
            }else{
                Signature signer = null;
                signer = Signature.getInstance(Util.getDefaultSigId(alg, challenge));
                signer.initSign(privateKey);
                signer.update(challenge.nonce);
                signer.update(challenge.requestDigest);
                signatureBytes = signer.sign();
                sigHashType = Util.getHashAlgIdFromSigId(signer.getAlgorithm());
            }

            int offset = 0;
            byte signature[] = new byte[signatureBytes.length + sigHashType.length + 2 * Encoder.INT_SIZE];
            offset += Encoder.writeByteArray(signature, offset, sigHashType);
            offset += Encoder.writeByteArray(signature, offset, signatureBytes);
            return signature;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new HandleException(HandleException.INTERNAL_ERROR, "Unable to sign challenge: ", e);
        }
    }

    private byte[] concatByteArrays(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    /***********************************************************************
     * Get the handle that identifies the user that is
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
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public String toString() {
        return "public_key:" + String.valueOf(userIdIndex) + ':' + ((userIdHandle == null) ? "null" : Util.decodeString(userIdHandle));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((privateKey == null) ? 0 : Arrays.hashCode(privateKey.getEncoded()));
        result = prime * result + Arrays.hashCode(userIdHandle);
        result = prime * result + userIdIndex;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PublicKeyAuthenticationInfo other = (PublicKeyAuthenticationInfo) obj;
        if (!Arrays.equals(userIdHandle, other.userIdHandle)) return false;
        if (userIdIndex != other.userIdIndex) return false;
        if (privateKey == null) {
            if (other.privateKey != null) return false;
        } else if (!Util.equals(privateKey.getEncoded(), other.privateKey.getEncoded())) return false;
        return true;
    }
}
