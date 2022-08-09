/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.util.Arrays;

public class ValueReference {
    public byte handle[];
    public int index;

    public ValueReference() {
    }

    public ValueReference(byte handle[], int index) {
        this.handle = handle;
        this.index = index;
    }

    public ValueReference(String handleString, int index) {
        this.handle = Util.encodeString(handleString);
        this.index = index;
    }

    @Override
    public String toString() {
        return String.valueOf(index) + ':' + Util.decodeString(handle);
    }

    public static ValueReference fromString(String s) {
        if (s == null) return null;
        int colon = s.indexOf(':');
        if (colon < 0) return new ValueReference(Util.encodeString(s), 0);
        String maybeIndex = s.substring(0, colon);
        if (isDigits(maybeIndex)) {
            String handle = s.substring(colon + 1);
            return new ValueReference(Util.encodeString(handle), Integer.parseInt(maybeIndex));
        }
        return new ValueReference(Util.encodeString(s), 0);
    }

    public String getHandleAsString() {
        return Util.decodeString(handle);
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(Util.upperCasePrefix(handle));
        result = prime * result + index;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ValueReference other = (ValueReference) obj;
        if (!Util.equalsPrefixCI(handle, other.handle)) return false;
        if (index != other.index) return false;
        return true;
    }

    public boolean isMatchedBy(ValueReference other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!Util.equalsPrefixCI(handle, other.handle)) return false;
        if (index != 0 && index != other.index) return false;
        return true;
    }
}
