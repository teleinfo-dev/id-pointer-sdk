/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;

import java.security.PublicKey;
import java.util.List;

public class IssuedSignature {
    public JsonWebSignature jws;
    public PublicKey issuerPublicKey;
    public List<Permission> issuerPermissions;

    public IssuedSignature(JsonWebSignature jws, PublicKey issuerPublicKey, List<Permission> issuerPermissions) {
        this.jws = jws;
        this.issuerPublicKey = issuerPublicKey;
        this.issuerPermissions = issuerPermissions;
    }
}
