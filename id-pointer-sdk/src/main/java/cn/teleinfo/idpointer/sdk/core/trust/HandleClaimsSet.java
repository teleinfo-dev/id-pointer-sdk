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

public class HandleClaimsSet extends JwtClaimsSet {
    public List<Permission> perms;
    public DigestedHandleValues digests;
    //first element authorizes the issuer of this claims set.
    //Second element authorizes the issuer of that authorization...
    public List<String> chain;
    public PublicKey publicKey;
    public String content; //Optional string to sign.
}
