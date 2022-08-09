/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRequiredSignerStore {

    private static HandleVerifier handleVerifier = HandleVerifier.getInstance();
    protected volatile List<JsonWebSignature> requiredSigners;

    public void loadSigners() {
        //no-op
    }

    public boolean needsLoadSigners() {
        return false;
    }

    protected boolean validateSelfSignedCert(JsonWebSignature cert) throws TrustException {
        HandleClaimsSet claims = handleVerifier.getHandleClaimsSet(cert);
        PublicKey publicKey = claims.publicKey;
        String issuer = claims.iss;
        String subject = claims.sub;
        if (!issuer.equals(subject)) {
            return false;
        }
        if (!claims.isDateInRange(System.currentTimeMillis() / 1000L)) {
            return false;
        }
        return cert.validates(publicKey);
    }

    public List<JsonWebSignature> getRequiredSignersAuthorizedOver(String handle) {
        List<JsonWebSignature> currentRequiredSigners = requiredSigners;
        List<JsonWebSignature> results = new ArrayList<>();
        for (JsonWebSignature cert : currentRequiredSigners) {
            HandleClaimsSet claims = handleVerifier.getHandleClaimsSet(cert);
            List<Permission> perms = claims.perms;
            boolean isAuthorizedOver = handleVerifier.verifyPermissionsAreAuthorizedOverHandle(handle, perms);
            if (isAuthorizedOver) {
                results.add(cert);
            }
        }
        return results;
    }

}
