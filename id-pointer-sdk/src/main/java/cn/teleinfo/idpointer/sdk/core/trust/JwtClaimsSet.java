/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;

public class JwtClaimsSet {
    public String iss;
    public String sub;
    public Long exp;
    public Long nbf;
    public Long iat;

    public boolean isDateInRange(long nowInSeconds) {
        if (exp != null && nowInSeconds > exp) return false;
        if (nbf != null && nowInSeconds < nbf) return false;
        return true;
    }

    public boolean isSelfIssued() {
        return sub != null && sub.equals(iss);
    }
}
