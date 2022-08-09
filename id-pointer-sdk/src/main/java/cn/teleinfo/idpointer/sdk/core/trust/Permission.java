/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;


import cn.teleinfo.idpointer.sdk.core.Util;

public class Permission {
    public static final String EVERYTHING = "everything";
    public static final String THIS_HANDLE = "thisHandle";
    public static final String DERIVED_PREFIXES = "derivedPrefixes";
    public static final String HANDLES_UNDER_THIS_PREFIX = "handlesUnderThisPrefix";

    public String handle;
    public String perm;

    public Permission() {
    }

    /**
     * @param handle Handle over which permission is granted, generally a prefix handle
     * @param permission
     */
    public Permission(String handle, String permission) {
        this.handle = handle;
        this.perm = permission;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((handle == null) ? 0 : Util.upperCasePrefix(handle).hashCode());
        result = prime * result + ((perm == null) ? 0 : perm.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Permission other = (Permission) obj;
        if (handle == null) {
            if (other.handle != null) return false;
        } else if (!Util.equalsPrefixCI(handle, other.handle)) return false;
        if (perm == null) {
            if (other.perm != null) return false;
        } else if (!perm.equals(other.perm)) return false;
        return true;
    }
}
