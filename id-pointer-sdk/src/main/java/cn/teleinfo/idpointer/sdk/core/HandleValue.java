/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.core.stream.util.FastDateFormat;

import java.util.*;

/** Represents a single handle value */
public class HandleValue {

    public static final byte SUBTYPE_SEPARATOR = (byte) '.';
    public static final byte TTL_TYPE_RELATIVE = 0;
    public static final byte TTL_TYPE_ABSOLUTE = 1;
    public static final int MAX_RECOGNIZED_TTL = 86400 * 2; // two days, higher ttl's will be ignored

    int index = -1;
    byte[] type = Common.EMPTY_BYTE_ARRAY;
    byte[] data = Common.EMPTY_BYTE_ARRAY;
    byte ttlType = TTL_TYPE_RELATIVE;
    int ttl = 86400;
    int timestamp = 0;
    ValueReference[] references = null;
    boolean adminRead = true; // indicates whether or not admins can read this value
    boolean adminWrite = true; // indicates whether or not admins can modify this value
    boolean publicRead = true; // indicates whether or not anyone can read this value
    boolean publicWrite = false; // indicates whether or not anyone can modify this value

    byte cachedBuf[] = null;
    int cachedBufOffset = 0;
    int cachedBufLength = 0;

    public HandleValue() {
    }

    public HandleValue(int index, byte type[], byte data[]) {
        this.index = index;
        this.type = type;
        this.data = data;
    }

    public HandleValue(int index, String type, byte[] data) {
        this.index = index;
        this.type = Util.encodeString(type);
        this.data = data;
    }

    public HandleValue(int index, String type, String data) {
        this.index = index;
        this.type = Util.encodeString(type);
        this.data = Util.encodeString(data);
    }

    public HandleValue(int index, byte type[], byte data[], byte ttlType, int ttl, int timestamp, ValueReference references[], boolean adminRead, boolean adminWrite, boolean publicRead, boolean publicWrite) {
        this.index = index;
        this.type = type;
        this.data = data;
        this.ttlType = ttlType;
        this.ttl = ttl;
        this.timestamp = timestamp;
        this.references = references;
        this.adminRead = adminRead;
        this.adminWrite = adminWrite;
        this.publicRead = publicRead;
        this.publicWrite = publicWrite;
    }

    public final String getPermissionString() {
        return new String(new char[] { adminRead ? 'r' : '-', adminWrite ? 'w' : '-', publicRead ? 'r' : '-', publicWrite ? 'w' : '-' });
    }

    public String toDetailedString() {
        return " index=" + index + " type=" + (type == null ? "" : Util.decodeString(type)) + " " + getPermissionString() + " ttl=" + ttlType + "/" + ttl + " timestamp="
            + FastDateFormat.formatUtc(FastDateFormat.FormatSpec.ISO8601_NO_MS, 1000L * timestamp) + " \"" + (data == null ? "" : (Util.looksLikeBinary(data) ? Util.decodeHexString(data, false) : Util.decodeString(data))) + '"';
    }

    @Override
    public String toString() {
        return " index=" + index + " type=" + (type == null ? "" : Util.decodeString(type)) + " " + getPermissionString() + " \""
            + (data == null ? "" : (Util.looksLikeBinary(data) ? Util.decodeHexString(data, false) : Util.decodeString(data))) + '"';
    }

    /** Given the current time and the time this value was retrieved from a
     *  handle server (in seconds), return true if this value is "stale" and
     *  should be retrieved again. */
    public boolean isExpired(int now, int timeRetrieved) {
        switch (ttlType) {
        case TTL_TYPE_RELATIVE:
            return ttl == 0 || Math.min(ttl, MAX_RECOGNIZED_TTL) < (now - timeRetrieved);
        case TTL_TYPE_ABSOLUTE:
            return MAX_RECOGNIZED_TTL > (now - timeRetrieved) || ttl < now;
        default:
            //System.err.println("Unknown ttl type: "+ttlType);
            return true;
        }
    }

    /** Returns whether or not this handle value has the given type.
     * This handles subtypes, so if you call hasType("URL") and the
     * type of this handle value is "URL.METADATA" then this will
     * return true.
     */
    public final boolean hasType(byte someType[]) {
        return Util.equalsCI(this.type, someType) || (someType.length < type.length && type[someType.length] == SUBTYPE_SEPARATOR && Util.startsWithCI(type, someType));
    }

    public final String getDataAsString() {
        if (data == null) return "";
        if (Util.looksLikeBinary(data)) return Util.decodeHexString(data, false);
        return Util.decodeString(data);
    }

    public final String getTypeAsString() {
        return (type == null ? "" : Util.decodeString(type));
    }

    public final String getTimestampAsString() {
        // format "Thu Apr 13 11:08:57 EDT 2000"
        return (timestamp <= 0 ? "NA" : (new Date(timestamp * 1000L)).toString());
    }

    private static FastDateFormat format = new FastDateFormat(new FastDateFormat.FormatSpec("-", " ", ":", ":", ".", false, true), null);

    public final String getNicerTimestampAsString() {
        if (timestamp < 0) return "NA";
        return format.format(1000L * timestamp);
    }

    public final Date getTimestampAsDate() {
        return (timestamp <= 0 ? null : (new Date(timestamp * 1000L)));
    }

    public final int getIndex() {
        return index;
    }

    public final void setIndex(int newIndex) {
        this.index = newIndex;
        cachedBuf = null;
    }

    public final byte[] getType() {
        return type;
    }

    public final void setType(byte newType[]) {
        this.type = newType;
        cachedBuf = null;
    }

    public final byte[] getData() {
        return data;
    }

    public final void setData(byte newData[]) {
        this.data = newData;
        cachedBuf = null;
    }

    public final byte getTTLType() {
        return ttlType;
    }

    public final void setTTLType(byte newTTLType) {
        this.ttlType = newTTLType;
        cachedBuf = null;
    }

    public final int getTTL() {
        return ttl;
    }

    public final void setTTL(int newTTL) {
        this.ttl = newTTL;
        cachedBuf = null;
    }

    public final int getTimestamp() {
        return timestamp;
    }

    public final void setTimestamp(int newTimestamp) {
        this.timestamp = newTimestamp;
    }

    public final ValueReference[] getReferences() {
        return references;
    }

    public final void setReferences(ValueReference newReferences[]) {
        this.references = newReferences;
        cachedBuf = null;
    }

    public final boolean getAdminCanRead() {
        return adminRead;
    }

    public final void setAdminCanRead(boolean newAdminRead) {
        this.adminRead = newAdminRead;
        cachedBuf = null;
    }

    public final boolean getAdminCanWrite() {
        return adminWrite;
    }

    public final void setAdminCanWrite(boolean newAdminWrite) {
        this.adminWrite = newAdminWrite;
        cachedBuf = null;
    }

    public final boolean getAnyoneCanRead() {
        return publicRead;
    }

    public final void setAnyoneCanRead(boolean newPublicRead) {
        this.publicRead = newPublicRead;
        cachedBuf = null;
    }

    public final boolean getAnyoneCanWrite() {
        return publicWrite;
    }

    public final void setAnyoneCanWrite(boolean newPublicWrite) {
        this.publicWrite = newPublicWrite;
        cachedBuf = null;
    }

    /**
     * Returns a copy of this HandleValue
     */
    public HandleValue duplicate() {
        ValueReference newRefs[] = null;
        ValueReference myRefs[] = references;
        if (myRefs != null) {
            newRefs = new ValueReference[myRefs.length];
            for (int i = 0; i < newRefs.length; i++) {
                newRefs[i] = new ValueReference(myRefs[i].handle, myRefs[i].index);
            }
        }
        return new HandleValue(index, Util.duplicateByteArray(type), Util.duplicateByteArray(data), ttlType, ttl, timestamp, newRefs, adminRead, adminWrite, publicRead, publicWrite);
    }

    public static Comparator<HandleValue> INDEX_COMPARATOR = (v1, v2) -> {
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;
        if (v1.getIndex() < v2.getIndex()) return -1;
        if (v1.getIndex() == v2.getIndex()) return 0;
        return 1;
    };

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (adminRead ? 1231 : 1237);
        result = prime * result + (adminWrite ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(data);
        result = prime * result + index;
        result = prime * result + (publicRead ? 1231 : 1237);
        result = prime * result + (publicWrite ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(emptyToNull(references));
        result = prime * result + timestamp;
        result = prime * result + ttl;
        result = prime * result + ttlType;
        result = prime * result + Arrays.hashCode(type);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HandleValue other = (HandleValue) obj;
        if (timestamp != other.timestamp) {
            return false;
        }
        return equalsIgnoreTimestamp(other);
    }

    public boolean equalsIgnoreTimestamp(HandleValue other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (adminRead != other.adminRead) {
            return false;
        }
        if (adminWrite != other.adminWrite) {
            return false;
        }
        if (!Arrays.equals(data, other.data)) {
            return false;
        }
        if (index != other.index) {
            return false;
        }
        if (publicRead != other.publicRead) {
            return false;
        }
        if (publicWrite != other.publicWrite) {
            return false;
        }
        if (!Arrays.equals(emptyToNull(references), emptyToNull(other.references))) {
            return false;
        }
        if (ttl != other.ttl) {
            return false;
        }
        if (ttlType != other.ttlType) {
            return false;
        }
        if (!Arrays.equals(type, other.type)) {
            return false;
        }
        return true;
    }

    private static <T> T[] emptyToNull(T[] array) {
        if (array != null && array.length == 0) return null;
        return array;
    }

    public static boolean unorderedEquals(HandleValue[] vals1, HandleValue[] vals2) {
        if (vals1 == null && vals2 == null) return true;
        if (vals1 == null || vals2 == null) return false;
        if (vals1.length != vals2.length) return false;
        List<HandleValue> vals1List = new ArrayList<>(vals1.length);
        for (HandleValue val : vals1) vals1List.add(val);
        List<HandleValue> vals2List = new ArrayList<>(vals2.length);
        for (HandleValue val : vals2) vals2List.add(val);
        Collections.sort(vals1List, INDEX_COMPARATOR);
        Collections.sort(vals2List, INDEX_COMPARATOR);
        return vals1List.equals(vals2List);
    }

    public static boolean unorderedEqualsIgnoreTimestamp(HandleValue[] vals1, HandleValue[] vals2) {
        if (vals1 == null && vals2 == null) return true;
        if (vals1 == null || vals2 == null) return false;
        if (vals1.length != vals2.length) return false;
        List<HandleValue> vals1List = new ArrayList<>(vals1.length);
        for (HandleValue val : vals1) vals1List.add(val);
        List<HandleValue> vals2List = new ArrayList<>(vals2.length);
        for (HandleValue val : vals2) vals2List.add(val);
        Collections.sort(vals1List, INDEX_COMPARATOR);
        Collections.sort(vals2List, INDEX_COMPARATOR);
        for (int i = 0; i < vals1.length; i++) {
            if (!vals1List.get(i).equalsIgnoreTimestamp(vals2List.get(i))) return false;
        }
        return true;
    }
}
