/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;


import cn.teleinfo.idpointer.sdk.core.GsonUtility;
import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.core.ValueReference;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

public class HandleSigner {

    private static HandleSigner INSTANCE = new HandleSigner();

    public static HandleSigner getInstance() {
        return INSTANCE;
    }

    public HandleClaimsSet createPayload(String handleToSign, List<HandleValue> valuesToSign, ValueReference signer, List<String> chain, long notBefore, long expiration) {
        HandleClaimsSet claims = new HandleClaimsSet();
        claims.sub = handleToSign;
        if (signer != null) {
            claims.iss = signer.toString();
        } else {
            claims.iss = "";
        }
        claims.iat = System.currentTimeMillis()/1000L;
        claims.nbf = notBefore;
        claims.exp = expiration;
        claims.chain = chain;
        DigestedHandleValues digests;
        try {
            digests = new HandleValueDigester().digest(valuesToSign, "SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        claims.digests = digests;
        return claims;
    }

    public JsonWebSignature signClaims(HandleClaimsSet claims, PrivateKey privateKey) throws TrustException {
        String payload = GsonUtility.getGson().toJson(claims);
        JsonWebSignature jws = JsonWebSignatureFactory.getInstance().create(payload, privateKey);
        return jws;
    }

    public JsonWebSignature signClaimsRemotely(HandleClaimsSet claims, String baseUri, String username, String password, String privateKeyId, String privateKeyPassphrase) throws TrustException {
        String payloadString = GsonUtility.getGson().toJson(claims);
        RemoteJsonWebSignatureSigner remoteSigner = new RemoteJsonWebSignatureSigner(baseUri);
        JsonWebSignature jws = remoteSigner.create(payloadString, username, password, privateKeyId, privateKeyPassphrase);
        return jws;
    }

    public JsonWebSignature signHandleValues(String handleToSign, List<HandleValue> valuesToSign, ValueReference signer, PrivateKey privateKey, List<String> chain, long notBefore, long expiration) throws TrustException {
        HandleClaimsSet claims = new HandleClaimsSet();
        claims.sub = handleToSign;
        claims.iss = signer.toString();
        claims.iat = System.currentTimeMillis()/1000L;
        claims.nbf = notBefore;
        claims.exp = expiration;
        claims.chain = chain;
        DigestedHandleValues digests;
        try {
            digests = new HandleValueDigester().digest(valuesToSign, "SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        claims.digests = digests;

        JsonWebSignature jws = signClaims(claims, privateKey);
        return jws;
    }

    public JsonWebSignature signHandleValuesRemotely(String handleToSign, List<HandleValue> valuesToSign, ValueReference signer, List<String> chain, long notBefore, long expiration, String baseUri, String username, String password, String privateKeyId, String privateKeyPassphrase) throws TrustException {
        HandleClaimsSet claims = new HandleClaimsSet();
        claims.sub = handleToSign;
        claims.iss = signer.toString();
        claims.iat = System.currentTimeMillis()/1000L;
        claims.nbf = notBefore;
        claims.exp = expiration;
        claims.chain = chain;
        DigestedHandleValues digests;
        try {
            digests = new HandleValueDigester().digest(valuesToSign, "SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        claims.digests = digests;

        JsonWebSignature jws = signClaimsRemotely(claims, baseUri, username, password, privateKeyId, privateKeyPassphrase);
        return jws;
    }

    public JsonWebSignature signPermissions(ValueReference subject, PublicKey subjectPublicKey, List<Permission> permissions, ValueReference signer, PrivateKey privateKey, List<String> chain, long notBefore, long expiration) throws TrustException {
        HandleClaimsSet claims = new HandleClaimsSet();
        claims.sub = subject.toString();
        claims.iss = signer.toString();
        claims.iat = System.currentTimeMillis()/1000L;
        claims.nbf = notBefore;
        claims.exp = expiration;
        claims.chain = chain;
        claims.perms = permissions;
        claims.publicKey = subjectPublicKey;

        JsonWebSignature jws = signClaims(claims, privateKey);
        return jws;
    }

    public JsonWebSignature signPermissionsRemotely(ValueReference subject, PublicKey subjectPublicKey, List<Permission> permissions, ValueReference signer, List<String> chain, long notBefore, long expiration, String baseUri, String username, String password, String privateKeyId, String privateKeyPassphrase) throws TrustException {
        HandleClaimsSet claims = new HandleClaimsSet();
        claims.sub = subject.toString();
        claims.iss = signer.toString();
        claims.iat = System.currentTimeMillis()/1000L;
        claims.nbf = notBefore;
        claims.exp = expiration;
        claims.chain = chain;
        claims.perms = permissions;
        claims.publicKey = subjectPublicKey;

        JsonWebSignature jws = signClaimsRemotely(claims, baseUri, username, password, privateKeyId, privateKeyPassphrase);
        return jws;
    }
}
