/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.util.Arrays;

public class Attribute {
    public byte name[];
    public byte value[];

    public Attribute() {
    }

    public Attribute(byte name[], byte value[]) {
        this.name = name;
        this.value = value;
    }

    public Attribute cloneAttribute() {
        return new Attribute(Util.duplicateByteArray(name), Util.duplicateByteArray(value));
    }

    @Override
    public String toString() {
        if (name != null && value != null) return Util.decodeString(name) + ':' + Util.decodeString(value);
        if (name != null) return Util.decodeString(name) + ':';
        if (value != null) return ":" + Util.decodeString(value);
        return "<null>";

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(name);
        result = prime * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Attribute other = (Attribute) obj;
        if (!Arrays.equals(name, other.name)) return false;
        if (!Arrays.equals(value, other.value)) return false;
        return true;
    }
}
