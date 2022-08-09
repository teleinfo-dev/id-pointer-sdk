/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
 All rights reserved.

 The HANDLE.NET software is made available subject to the
 Handle.Net Public License Agreement, which may be obtained at
 http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
 \**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import cn.teleinfo.idpointer.sdk.core.Encoder;
import cn.teleinfo.idpointer.sdk.core.SiteInfo;
import cn.teleinfo.idpointer.sdk.core.sample.SiteInfoConverter;
import cn.teleinfo.idpointer.sdk.security.HdlSecurityProvider;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.io.*;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;

public abstract class Util {

    private static final char HEX_VALUES[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static final boolean looksLikeBinary(byte buf[]) {
        if (buf == null) return true;
        if (!isValidString(buf, 0, buf.length)) return true;
        // 0x20 <= x < 0x7F -- but pass whitespace control chars, and pass >=0x80 for UTF-8 encoding purposes
        for (byte b : buf) {
            if (b >= 0x09 && b <= 0x13) continue; // whitespace
            if ((b >= 0x00 && b < 0x20) || b == 0x7F) return true;
        }
        return false;
    }

    public static final byte[] duplicateByteArray(byte buf[]) {
        if (buf == null) return null;
        byte newbuf[] = new byte[buf.length];
        System.arraycopy(buf, 0, newbuf, 0, newbuf.length);
        return newbuf;
    }

    public static final String decodeHexString(byte buf[], int offset, int len, boolean formatNicely) {
        if (buf == null || buf.length <= 0) return "";
        StringBuffer sb = new StringBuffer();
        for (int i = offset; i < offset + len; i++) {
            if (formatNicely && i > 0 && (i % 16) == 0) sb.append('\n');
            sb.append(HEX_VALUES[(buf[i] & 0xF0) >>> 4]);
            sb.append(HEX_VALUES[(buf[i] & 0xF)]);
        }
        return sb.toString();
    }

    public static final String decodeHexString(byte buf[], boolean formatNicely) {
        return decodeHexString(buf, 0, buf.length, formatNicely);
    }

    public static final byte[] encodeHexString(String s) {
        s = s.toUpperCase().trim();
        byte buf[] = new byte[s.length() / 2 + 1];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte) 0;
        }
        int i = 0;
        boolean lowNibble = false;

        char ch;
        for (int c = 0; c < s.length(); c++) {
            ch = s.charAt(c);
            if (ch >= '0' && ch <= '9') {
                if (lowNibble) buf[i++] |= ch - '0';
                else buf[i] = (byte) ((ch - '0') << 4);
                lowNibble = !lowNibble;
            } else if (ch >= 'A' && ch <= 'F') {
                if (lowNibble) buf[i++] |= ch - 'A' + 10;
                else buf[i] = (byte) ((ch - 'A' + 10) << 4);
                lowNibble = !lowNibble;
            }
        }
        byte realBuf[];
        if (!lowNibble) {
            realBuf = new byte[i];
        } else {
            realBuf = new byte[i + 1];
        }
        System.arraycopy(buf, 0, realBuf, 0, realBuf.length);
        return realBuf;

    }

    /** Encoded the specified string into a byte array */
    public static final byte[] encodeString(String s) {
        try {
            return s.getBytes(Common.TEXT_ENCODING);
        } catch (Exception e) {
            System.err.println(e);
        }
        return s.getBytes();
    }

    public static final String decodeString(byte buf[]) {
        if (buf == null || buf.length == 0) return "";
        try {
            return new String(buf, Common.TEXT_ENCODING);
        } catch (Exception e) {
            System.err.println(e);
        }
        return new String(buf);
    }

    public static final String decodeString(byte buf[], int offset, int len) {
        if (buf == null || buf.length == 0) return "";
        try {
            return new String(buf, offset, len, Common.TEXT_ENCODING);
        } catch (Exception e) {
            System.err.println(e);
        }
        return new String(buf, offset, len);
    }

    /** Returns true is the given byte array represents a valid
     text string in the encoding used by the handle protocol (utf8). */
    public static final boolean isValidString(byte buf[], int offset, int len) {
        int byte2mask = 0x00; // should be unsigned???
        int c;

        int trailing = 0; // trailing (continuation) bytes to follow
        int i = offset;

        while (i < len) {
            c = buf[i++];
            if (trailing != 0) {
                if ((c & 0xC0) == 0x80) { // Does trailing byte follow UTF-8 format?
                    if (byte2mask != 0) { // Need to check 2nd byte for proper range?
                        if ((c & byte2mask) != 0) { // Are appropriate bits set?
                            byte2mask = 0x00;
                        }
                        else return false;
                    }
                    trailing--;
                } else {
                    return false;
                }
            } else {
                if ((c & 0x80) == 0x00) {
                    continue; // valid 1 byte UTF-8
                } else if ((c & 0xE0) == 0xC0) { // valid 2 byte UTF-8
                    if ((c & 0x1E) != 0) { // Is UTF-8 byte in
                        // proper range?
                        trailing = 1;
                    } else return false;
                } else if ((c & 0xF0) == 0xE0) { // valid 3 byte UTF-8
                    if ((c & 0x0F) == 0) { // Is UTF-8 byte in
                        // proper range?
                        byte2mask = 0x20; // If not set mask
                    }
                    // to check next byte
                    trailing = 2;
                } else if ((c & 0xF8) == 0xF0) { // valid 4 byte UTF-8
                    if ((c & 0x07) == 0) { // Is UTF-8 byte in
                        // proper range?
                        byte2mask = 0x30; // If not set mask
                    }
                    // to check next byte
                    trailing = 3;
                } else if ((c & 0xFC) == 0xF8) { // valid 5 byte UTF-8
                    if ((c & 0x03) == 0) { // Is UTF-8 byte in
                        // proper range?
                        byte2mask = 0x38; // If not set mask
                    }
                    // to check next byte
                    trailing = 4;
                } else if ((c & 0xFE) == 0xFC) { // valid 6 byte UTF-8
                    if ((c & 0x01) == 0) { // Is UTF-8 byte in
                        // proper range?
                        byte2mask = 0x3C; // If not set mask
                    }
                    // to check next byte
                    trailing = 5;
                } else {
                    return false;
                }
            }
        }
        return trailing == 0;
    }

    /** Return whether a handle has a slash */
    public static final boolean hasSlash(byte handle[]) {
        return indexOf(handle, (byte) '/') >= 0;
    }

    /**
     * Get only the suffix part of this handle.
     * @deprecated use getSuffixPart(byte[]) instead
     */
    @Deprecated
    public static final byte[] getIDPart(byte handle[]) {
        return getSuffixPart(handle);
    }

    /**
     * Get only the prefix part of this handle.
     * @deprecated use getPrefixPart(byte[]) instead
     */
    @Deprecated
    public static final byte[] getNAPart(byte handle[]) {
        return getPrefixPart(handle);
    }

    /**
     * Get the 0.NA authority handle that applies to the specified handle
     * @deprecated use getZeroNAHandle(byte[]) instead
     */
    @Deprecated
    public static final byte[] getNAHandle(byte handle[]) {
        return getZeroNAHandle(handle);
    }

    /** Get the 0.NA authority handle that applies to the specified handle */
    public static final byte[] getZeroNAHandle(byte handle[]) {
        int slashIndex = indexOf(handle, (byte) '/');
        if (slashIndex >= 0) {
            byte naHandle[] = new byte[slashIndex + Common.NA_HANDLE_PREFIX.length];
            System.arraycopy(Common.NA_HANDLE_PREFIX, 0, naHandle, 0, Common.NA_HANDLE_PREFIX.length);
            System.arraycopy(handle, 0, naHandle, Common.NA_HANDLE_PREFIX.length, slashIndex);
            upperCaseInPlace(naHandle);
            return naHandle;
        } else {
            return Common.ROOT_HANDLE;
        }
    }

    public static String getZeroNAHandle(String handle) {
        return decodeString(getZeroNAHandle(encodeString(handle)));
    }

    public static final byte[] convertSlashlessHandleToZeroNaHandle(byte[] handle) {
        if (Util.hasSlash(handle)) return handle;
        byte result[] = new byte[Common.NA_HANDLE_PREFIX.length + handle.length];
        System.arraycopy(Common.NA_HANDLE_PREFIX, 0, result, 0, Common.NA_HANDLE_PREFIX.length);
        System.arraycopy(handle, 0, result, Common.NA_HANDLE_PREFIX.length, handle.length);
        upperCaseInPlace(result);
        return result;
    }

    /** only for 0.NA/derived.prefix handles */
    public static final boolean isSubNAHandle(byte handle[]) {
        if (Util.startsWithCI(handle, Common.NA_HANDLE_PREFIX)) {
            byte dot = (byte) '.';
            for (int i = Common.NA_HANDLE_PREFIX.length; i < handle.length; i++) {
                if (handle[i] == dot) return true;
            }
        }
        return false;
    }

    public static boolean isSubNAHandle(String handle) {
        return isSubNAHandle(encodeString(handle));
    }

    /** Get the parent prefix handle for the given prefix
     * handle.  The given handle MUST be a prefix handle of
     * form 0.NA/derived.prefix. */
    public static final byte[] getParentNAOfNAHandle(byte naHandle[]) {
        int parentEndIdx = naHandle.length - 1;
        int slashIdx = indexOf(naHandle, (byte) '/');
        byte dot = (byte) '.';
        while (parentEndIdx > slashIdx) {
            if (naHandle[parentEndIdx] == dot) {
                parentEndIdx--;
                break;
            }
            parentEndIdx--;
        }
        byte parentNAHandle[] = new byte[Common.NA_HANDLE_PREFIX.length + (parentEndIdx - slashIdx)];
        int loc = 0;
        System.arraycopy(Common.NA_HANDLE_PREFIX, 0, parentNAHandle, loc, Common.NA_HANDLE_PREFIX.length);
        System.arraycopy(naHandle, slashIdx + 1, parentNAHandle, Common.NA_HANDLE_PREFIX.length, (parentEndIdx - slashIdx));
        return parentNAHandle;
    }

    public static String getParentNAOfNAHandle(String naHandle) {
        return decodeString(getParentNAOfNAHandle(encodeString(naHandle)));
    }

    public static boolean isHandleUnderPrefix(String handle, String prefix) {
        prefix = upperCase(prefix);
        handle = upperCasePrefix(handle);
        if (!prefix.startsWith("0.NA/")) return false;
        String actualPrefix = prefix.substring("0.NA/".length());
        return handle.startsWith(actualPrefix + "/");
    }

    public static boolean isDerivedFrom(String handle, String ancestorHandle) {
        ancestorHandle = upperCase(ancestorHandle);
        handle = upperCase(handle);
        if (!handle.startsWith("0.NA/")) return false;
        //        if (handle.equals(ancestorHandle)) return true;
        return handle.startsWith(ancestorHandle + ".");
    }

    /** Get only the prefix part of this handle. */
    public static final byte[] getPrefixPart(byte handle[]) {
        int slashIndex = indexOf(handle, (byte) '/');
        return slashIndex < 0 ? Common.NA_HANDLE_PREFIX_NOSLASH : substring(handle, 0, slashIndex);
    }

    public static String getPrefixPart(String handle) {
        return decodeString(getPrefixPart(encodeString(handle)));
    }

    /** Get only the suffix part of this handle. */
    public static final byte[] getSuffixPart(byte handle[]) {
        int slashIndex = indexOf(handle, (byte) '/');
        return slashIndex < 0 ? handle : substring(handle, slashIndex + 1, handle.length);
    }

    public static String getSuffixPart(String handle) {
        return decodeString(getSuffixPart(encodeString(handle)));
    }

    public static final boolean startsWith(byte b1[], byte b2[]) {
        if (b1.length < b2.length) return false;
        for (int i = 0; i < b2.length; i++) {
            if (b1[i] != b2[i]) return false;
        }
        return true;
    }

    /**********************************************************************
     * compare the two arrays.  If they are the same true is returned.
     **********************************************************************/
    public static final boolean equals(byte b1[], byte b2[]) {
        if (b1 == null && b2 == null) return true;
        if (b1 == null || b2 == null) return false;
        if (b1.length != b2.length) return false;
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) return false;
        }
        return true;
    }

    /**********************************************************************
     * compare the two arrays starting at the given index. If they are
     * the same true is returned.
     **********************************************************************/
    public static final boolean equals(byte b1[], int b1Start, byte b2[], int b2Start) {
        if (b1 == null && b2 == null) return true;
        if (b1 == null || b2 == null) return false;
        if ((b1.length - b1Start) != (b2.length - b2Start)) return false;
        for (; b1Start < b1.length; b1Start++, b2Start++) {
            if (b1[b1Start] != b2[b2Start]) return false;
        }
        return true;
    }

    public static final byte CASE_DIFF = 'A' - 'a';

    /**********************************************************************
     * create and return an upper-case copy of the given UTF8 byte array
     **********************************************************************/
    public static final byte[] upperCase(byte b[]) {
        if (b == null || b.length == 0) return new byte[0];
        int sz = b.length;
        byte b2[] = new byte[sz];
        System.arraycopy(b, 0, b2, 0, sz);

        for (int i = sz - 1; i >= 0; i--) {
            if (b2[i] >= 'a' && b2[i] <= 'z') b2[i] += CASE_DIFF;
        }
        return b2;
    }

    public static String upperCase(String s) {
        if (s == null) return "";
        return decodeString(upperCase(encodeString(s)));
    }

    /**********************************************************************
     * Convert all of the characters in the given utf-8 byte array
     * to upper case.  Return the same array.
     **********************************************************************/
    public static final byte[] upperCaseInPlace(byte b[]) {
        if (b == null || b.length == 0) return b;
        for (int i = 0; i < b.length; i++) {
            if (b[i] >= 'a' && b[i] <= 'z') b[i] += CASE_DIFF;
        }
        return b;
    }

    /**********************************************************************
     * create and return an upper-case (prefix only, or all of a global handle) copy of the given UTF8 byte array
     **********************************************************************/
    public static final byte[] upperCasePrefix(byte b[]) {
        if (b == null || b.length == 0) return new byte[0];
        if (startsWith(b, Common.GLOBAL_NA_PREFIX)) return upperCase(b);
        int sz = b.length;
        byte b2[] = new byte[sz];
        System.arraycopy(b, 0, b2, 0, sz);

        boolean inPrefix = true;
        for (int i = 0; i < sz; i++) {
            if (inPrefix && b2[i] >= 'a' && b2[i] <= 'z') b2[i] += CASE_DIFF;
            if (b2[i] == '/') inPrefix = false;
        }
        return b2;
    }

    public static String upperCasePrefix(String s) {
        if (s == null) return "";
        return decodeString(upperCasePrefix(encodeString(s)));
    }

    /**********************************************************************
     * Convert all of the characters in the prefix of the given utf-8 byte array
     * to upper case; for global handles upper case all.  Return the same array.
     **********************************************************************/
    public static final byte[] upperCasePrefixInPlace(byte b[]) {
        if (b == null || b.length == 0) return b;
        if (startsWith(b, Common.GLOBAL_NA_PREFIX)) return upperCaseInPlace(b);
        boolean inPrefix = true;
        for (int i = 0; i < b.length; i++) {
            if (inPrefix && b[i] >= 'a' && b[i] <= 'z') b[i] += CASE_DIFF;
            if (b[i] == '/') inPrefix = false;
        }
        return b;
    }

    /** Determine if the first parameter equals the second
     *  parameter in a case insensitive comparison. */
    public static final boolean equalsCI(byte b1[], byte b2[]) {
        if (b1 == null && b2 == null) return true;
        if (b1 == null || b2 == null) return false;
        return equalsCI(b1, b1.length, b2, b2.length);
    }

    public static boolean equalsCI(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        return equalsCI(encodeString(s1), encodeString(s2));
    }

    /** Determine if the first parameter equals the second parameter
     in a case insensitive manner over the given length. */
    public static final boolean equalsCI(byte b1[], int b1Len, byte b2[], int b2Len) {
        if (b1 == null && b2 == null) return true;
        if (b1 == null || b2 == null) return false;
        if (b1Len != b2Len || b1Len > b1.length || b2Len > b2.length) return false;

        byte byte1, byte2;
        for (int i = 0; i < b1Len; i++) {
            byte1 = b1[i];
            byte2 = b2[i];
            if (byte1 == byte2) continue;
            if (byte1 >= 'a' && byte1 <= 'z') byte1 += CASE_DIFF;
            if (byte2 >= 'a' && byte2 <= 'z') byte2 += CASE_DIFF;
            if (byte1 != byte2) return false;
        }
        return true;
    }

    /** Determine if the first parameter equals the second
     *  parameter in a case insensitive (within prefix) comparison;
     *  for global handles, entire handles are compared case insensitively. */
    public static final boolean equalsPrefixCI(byte b1[], byte b2[]) {
        if (b1 == null && b2 == null) return true;
        if (b1 == null || b2 == null) return false;
        return equalsPrefixCI(b1, b1.length, b2, b2.length);
    }

    /** Determine if the first parameter equals the second
     *  parameter in a case insensitive (within prefix) comparison;
     *  for global handles, entire handles are compared case insensitively. */
    public static final boolean equalsPrefixCI(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        return equalsPrefixCI(encodeString(s1), encodeString(s2));
    }

    /** Determine if the first parameter equals the second parameter
     * in a case insensitive (within prefix) manner over the given length;
     * for global handles, entire handles are compared case insensitively. */
    public static final boolean equalsPrefixCI(byte b1[], int b1Len, byte b2[], int b2Len) {
        if (b1 == null && b2 == null) return true;
        if (b1 == null || b2 == null) return false;
        if (b1Len != b2Len || b1Len > b1.length || b2Len > b2.length) return false;

        boolean global = startsWith(b1, Common.GLOBAL_NA_PREFIX);

        byte byte1, byte2;
        boolean inPrefix = true;
        for (int i = 0; i < b1Len; i++) {
            byte1 = b1[i];
            byte2 = b2[i];
            if (byte1 == '/' && !global) inPrefix = false;
            if (byte1 == byte2) continue;
            if (inPrefix && byte1 >= 'a' && byte1 <= 'z') byte1 += CASE_DIFF;
            if (inPrefix && byte2 >= 'a' && byte2 <= 'z') byte2 += CASE_DIFF;
            if (byte1 != byte2) return false;
        }
        return true;
    }

    /** Determine if the first parameter begins with the second
     *  parameter in a case insensitive comparison. */
    public static final boolean startsWithCI(byte b1[], byte b2[]) {
        if (b1.length < b2.length) return false;
        byte byte1, byte2;
        for (int i = 0; i < b2.length; i++) {
            byte1 = b1[i];
            byte2 = b2[i];
            if (byte1 == byte2) continue;
            if (byte1 >= 'a' && byte1 <= 'z') byte1 += CASE_DIFF;
            if (byte2 >= 'a' && byte2 <= 'z') byte2 += CASE_DIFF;
            if (byte1 != byte2) return false;
        }
        return true;
    }

    public static boolean startsWithCI(String s1, String s2) {
        return startsWithCI(encodeString(s1), encodeString(s2));
    }

    /** determine if the second UTF8 encoded parameter begins
     *  with the second parameter in a case sensitive comparison. */
    public static final byte[] substring(byte b[], int i1) {
        return substring(b, i1, b.length);
    }

    public static final byte[] substring(byte b[], int i1, int i2) {
        byte rb[] = new byte[i2 - i1];
        System.arraycopy(b, i1, rb, 0, i2 - i1);
        return rb;
    }

    public static final int indexOf(byte b[], byte ch) {
        for (int i = 0; i < b.length; i++) {
            if (b[i] == ch) return i;
        }
        return -1;
    }

    public static final int countValuesOfType(HandleValue values[], byte type[]) {
        if (values == null) return 0;
        int matches = 0;
        for (HandleValue value : values) {
            if (equals(value.type, type)) {
                matches++;
            }
        }
        return matches;
    }

    public static String rfcIpPortRepr(InetAddress addr, int port) {
        if (!(addr instanceof Inet6Address)) {
            return addr.getHostAddress() + ":" + port;
        } else return "[" + Util.rfcIpRepr(addr) + "]:" + port;
    }

    private static int[] intsFromStringIPv6Address(String ipv6Address) {
        String tokenizedAddress[] = ipv6Address.split(":");
        if (tokenizedAddress.length != 8) return null;
        int integerAddress[] = new int[8];
        for (int i = 0; i < 8; i++) {
            if (tokenizedAddress[i].length() == 0) {
                return null;
            }
            try {
                integerAddress[i] = Integer.parseInt(tokenizedAddress[i], 16);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return integerAddress;
    }

    private static int[] intsFromByteIPv6Address(byte[] ipv6Address) {
        if (ipv6Address.length != 16) return null;
        int integerAddress[] = new int[8];
        for (int i = 0; i < 16; i += 2) {
            integerAddress[i / 2] = ((ipv6Address[i] & 0xFF) << 8) | (ipv6Address[i + 1] & 0xFF);
        }
        return integerAddress;
    }

    private static String integerIPv6AddressToString(int[] integerAddress) {
        StringBuilder sb = new StringBuilder();
        boolean previousWasZero = false;
        int currentZeros = 0;
        int currentZeroStartIndex = 0;
        int largestZeros = 0;
        int largestZeroStartIndex = -1;

        for (int i = 0; i < 8; i++) {
            if (integerAddress[i] == 0) {
                if (!previousWasZero) {
                    currentZeroStartIndex = i;
                    previousWasZero = true;
                }
                currentZeros += 1;
            } else {
                previousWasZero = false;
                if (currentZeros > largestZeros) {
                    largestZeroStartIndex = currentZeroStartIndex;
                    largestZeros = currentZeros;
                }
                currentZeros = 0;
            }

        }
        if (largestZeros == 1) {
            // Do not shorten just one 0 to ::.
            largestZeroStartIndex = -1;
        }
        for (int i = 0; i < 8; i++) {
            if (i != largestZeroStartIndex) {
                sb.append(Integer.toHexString(integerAddress[i]));
                if (i < 8 - 1) {
                    sb.append(":");
                }
            } else {
                if (i == 0) sb.append(":");
                i += largestZeros - 1;
                sb.append(":");
            }
        }
        return sb.toString();
    }

    public static String rfcIpRepr(byte[] ipv6Address) {
        int[] ints = intsFromByteIPv6Address(ipv6Address);
        if (ints == null) {
            if (ipv6Address == null) return null;
            if (ipv6Address.length == 4) return (ipv6Address[0] & 0xFF) + "." + (ipv6Address[1] & 0xFF) + "." + (ipv6Address[2] & 0xFF) + "." + (ipv6Address[3] & 0xFF);
            throw new IllegalArgumentException();
        }
        return integerIPv6AddressToString(ints);
    }

    public static String rfcIpRepr(InetAddress addr) {
        if (addr == null) return null;
        if (!(addr instanceof Inet6Address)) {
            return addr.getHostAddress();
        }

        String fullAddress = addr.getHostAddress();
        String IPv6Addr = fullAddress;
        String scope = null;
        int percent = fullAddress.indexOf('%');
        if (percent >= 0) {
            IPv6Addr = fullAddress.substring(0, percent);
            scope = fullAddress.substring(percent + 1);
        }

        int[] integerAddress = intsFromStringIPv6Address(IPv6Addr);
        if (integerAddress == null) return fullAddress;

        String res = integerIPv6AddressToString(integerAddress);
        if (scope != null) return res + "%" + scope;
        return res;
    }

    /**
     * Types in the array are either exact types (not ending in '.')
     * or prefixes of type families (ending in '.').
     *
     * Returns true when the given type is equal to an exact type in the array,
     * or is equal to a prefix (ignoring the '.'), or has a prefix ending with '.'
     * in the array.
     *
     * For example:
     *
     *  isParentInArray( { "url.", "email", "public_key" }, "url.us" ) returns true
     *  isParentInArray( { "url", "email", "public_key" }, "url.us" ) returns false
     *  isParentInArray( { "url.jp", "email", "public_key" }, "url" ) returns false
     *
     */
    public static final boolean isParentTypeInArray(byte a[][], byte val[]) {
        if (a == null) return false;
        if (val == null || val.length <= 0) return false;

        for (int i = a.length - 1; i >= 0; i--) {
            byte queryType[] = a[i];
            if (queryType.length > 0 && queryType[queryType.length - 1] == '.') {
                // look for a prefix match
                if (startsWithCI(val, queryType)) return true;

                // check for an exact match of just the prefix
                if (equalsCI(queryType, queryType.length - 1, val, val.length)) return true;

            } else {
                // looking for an exact match for this type
                if (equalsCI(queryType, val)) return true;
            }
        }

        return false;
    }

    /**********************************************************************
     * returns true if the given int value is in the specified array.
     **********************************************************************/
    public static final boolean isInArray(int a[], int val) {
        if (a == null) return false;
        for (int element : a) {
            if (element == val) return true;
        }
        return false;
    }

    /**********************************************************************
     * returns true if the given byte array is contained in the
     * specified byte array array.
     **********************************************************************/
    public static final boolean isInArray(byte a[][], byte val[]) {
        if (a == null) return false;
        for (byte[] element : a) {
            if (Util.equals(element, val)) return true;
        }
        return false;
    }

    public static final int getNextUnusedIndex(HandleValue values[], int firstIdx) {
        int nextIdx = firstIdx;
        outer: while (true) {
            for (HandleValue val : values) {
                if (val != null && val.getIndex() == nextIdx) {
                    nextIdx++;
                    continue outer;
                }
            }
            return nextIdx;
        }
    }

    public static SiteInfo getAltSiteInfo(SiteInfo site) {
        if (site.attributes == null) return null;
        List<ServerInfo> altServers = new ArrayList<>();
        HashMap<Integer, ServerInfo> id2Server = null;
        for (Attribute attribute : site.attributes) {
            String name = Util.decodeString(attribute.name);
            if ("alt_addr".equals(name) && site.servers.length == 1) {
                ServerInfo newServer = altServer(site.servers[0], attribute.value);
                if (newServer != null) {
                    altServers.add(newServer);
                }
            } else if (name.startsWith("alt_addr.")) {
                int periodIndex = name.indexOf(".");
                String serverIdString = name.substring(periodIndex + 1);
                int serverId;
                try {
                    serverId = Integer.parseInt(serverIdString);
                } catch (NumberFormatException e) {
                    System.err.println("Error decoding alt_addr: " + name + "\n  " + e);
                    e.printStackTrace(System.err);
                    continue;
                }
                if (id2Server == null) id2Server = site.getId2ServerMap();
                ServerInfo serverInfo = id2Server.get(serverId).cloneServerInfo();
                if (serverInfo == null) {
                    System.err.println("Error decoding alt_addr: " + name + "\n  ");
                    System.err.println("No server " + serverId);
                    continue;
                }
                ServerInfo newServer = altServer(site.servers[0], attribute.value);
                if (newServer != null) {
                    altServers.add(newServer);
                }
            }
        }
        if (altServers.size() > 0) {
            SiteInfo altSiteInfo = new SiteInfo(site);
            altSiteInfo.servers = altServers.toArray(new ServerInfo[1]);
            return altSiteInfo;
        } else {
            return null;
        }
    }

    public static byte[] fill16(byte[] bytes) {
        if (bytes.length == 16) return bytes;
        byte[] res = new byte[16];
        System.arraycopy(bytes, 0, res, 16 - bytes.length, bytes.length);
        return res;
    }

    private static ServerInfo altServer(ServerInfo orig, byte[] newAddr) {
        String addressString = Util.decodeString(newAddr);
        InetAddress address = null;
        try {
            address = InetAddress.getByName(addressString);
        } catch (UnknownHostException e) {
            System.err.println("Error decoding alt_addr: " + addressString + "\n  " + e);
            e.printStackTrace(System.err);
            return null;
        }

        ServerInfo serverInfo = orig.cloneServerInfo();
        serverInfo.ipAddress = fill16(address.getAddress());
        return serverInfo;
    }

    /**********************************************************************
     * Extract and return all of the SiteInfo records from the given list
     * of handle values.  Returns null if no site values were found.
     **********************************************************************/
    public static SiteInfo[] getSitesFromValues(HandleValue values[]) {
        if (values == null) return null;
        List<SiteInfo> sites = new ArrayList<>();
        for (HandleValue value : values) {
            if (Util.isInArray(Common.SITE_INFO_TYPES, value.type)) {
                try {
                    SiteInfo newSite = new SiteInfo();
                    Encoder.decodeSiteInfoRecord(value.data, 0, newSite);
                    sites.add(newSite);
                } catch (Exception e) {
                    System.err.println("Error decoding site record: " + e);
                    e.printStackTrace(System.err);
                }
            }
        }
        if (sites.isEmpty()) return null;
        return sites.toArray(new SiteInfo[sites.size()]);
    }

    /**********************************************************************
     * Extract and return all of the SiteInfo records from the given list
     * of handle values.  Include SiteInfos generated using the "alt_addr"
     * attribue.  Returns null if no site values were found.
     **********************************************************************/
    public static SiteInfo[] getSitesAndAltSitesFromValues(HandleValue values[]) {
        return getSitesAndAltSitesFromValues(values, Common.SITE_INFO_TYPES);
    }

    public static SiteInfo[] getSitesAndAltSitesFromValues(HandleValue values[], byte[][] types) {
        if (values == null) return null;
        List<SiteInfo> sites = new ArrayList<>();
        for (HandleValue value : values) {
            if (Util.isInArray(types, value.type)) {
                try {
                    SiteInfo newSite = new SiteInfo();
                    Encoder.decodeSiteInfoRecord(value.data, 0, newSite);
                    sites.add(newSite);

                    SiteInfo altSite = getAltSiteInfo(newSite);
                    if (altSite != null) {
                        sites.add(altSite);
                    }
                } catch (Exception e) {
                    System.err.println("Error decoding site record: " + e);
                    e.printStackTrace(System.err);
                }
            }
        }
        if (sites.isEmpty()) return null;
        return sites.toArray(new SiteInfo[sites.size()]);
    }

    /**
     * Extract and return the namespace information contained in the given
     * handle values.  If there are multiple values with type HS_NAMESPACE then
     * the one with the lowest index value will be used.  If no valid namespace
     * values are encountered then this will return null.
     */
    public static NamespaceInfo getNamespaceFromValues(HandleValue values[]) {
        return getNamespaceFromValues(null, values);
    }

    public static NamespaceInfo getNamespaceFromValues(String handle, HandleValue values[]) {
        NamespaceInfo nsInfo = null;
        int currentNSIdx = 0;
        for (int i = 0; values != null && i < values.length; i++) {
            if (values[i] == null) continue;
            if (values[i].hasType(Common.NAMESPACE_INFO_TYPE)) {
                if (values[i].getData() == null || values[i].getData().length == 0) continue;
                if (nsInfo == null || values[i].index < currentNSIdx) {
                    try {
                        nsInfo = new NamespaceInfo(values[i]);
                        currentNSIdx = values[i].index;
                    } catch (HandleException e) {
                        if (handle == null) {
                            System.err.println("Error decoding namespace info (" + values[i].getDataAsString() + "): " + e);
                            e.printStackTrace(System.err);
                        } else {
                            System.err.println("Error decoding namespace info of " + values[i].getIndex() + ":" + handle + " (" + values[i].getDataAsString() + "): " + e);
                        }
                    }
                }
            }
        }
        return nsInfo;
    }

    static final java.util.Random RANDOM = new java.util.Random();
    static final java.util.Random SECURE_RANDOM = new SecureRandom();

    static final Comparator<SiteInfo> SITE_INFO_RESPONSE_TIME_COMPARATOR = (o1, o2) -> Long.signum(o1.responseTime - o2.responseTime);

    /**************************************************************************
     * rearranges the given sites in a more efficient order so that resolution
     * from the current location should tend to access the faster sites first.
     * If a preferred site is listed in the server configuration file, it is
     * accessed first.
     **************************************************************************/
    public static final SiteInfo[] orderSitesByPreference(SiteInfo sites[]) {
        if (sites == null) return new SiteInfo[0];
        if (sites.length == 1) return sites;
        long[] responseTimes = new long[sites.length];
        byte[] randomBytes = new byte[sites.length];
        RANDOM.nextBytes(randomBytes);
        SiteInfo[] sitesInOriginalOrder = sites.clone();
        for (int i = 0; i < sites.length; i++) {
            long rt = sites[i].responseTime;
            responseTimes[i] = rt;
            sites[i].responseTime = rt + randomBytes[i];
        }
        // The result of this randomization is that (x+256) will never be used in preference to x
        // but (x+128) will be used 1/8 of the time
        // and (x+64) will be used 9/32 (just over 1/4) of the time
        Arrays.sort(sites, SITE_INFO_RESPONSE_TIME_COMPARATOR);
        for (int i = 0; i < sites.length; i++) {
            sitesInOriginalOrder[i].responseTime = responseTimes[i];
        }
        //if a preferred site is given, this site is accessed first
        String preferredGlobal = System.getProperty("hdllib.preferredGlobal");
        if (preferredGlobal != null) {
            for (int j = 0; j < sites.length; j++) {
                for (ServerInfo server : sites[j].servers) {
                    if (preferredGlobal.equals(server.getAddressString())) {
                        SiteInfo temp = sites[0];
                        sites[0] = sites[j];
                        sites[j] = temp;
                    }
                }
            }
        }
        return sites;
    }

    public static SiteInfo getPrimarySite(SiteInfo[] sites) {
        for (SiteInfo site : sites) {
            if (site.isPrimary) return site;
        }
        return null;
    }

    public static HandleValue[] filterValues(HandleValue[] allValues, int[] indexList, byte[][] typeList) {
        if (allValues == null) return null;
        if ((indexList == null || indexList.length == 0) && (typeList == null || typeList.length == 0)) return allValues;
        List<HandleValue> values = new ArrayList<>(allValues.length);
        for (HandleValue value : allValues) {
            if ((typeList != null && typeList.length > 0 && Util.isParentTypeInArray(typeList, value.getType())) || (indexList != null && indexList.length > 0 && Util.isInArray(indexList, value.getIndex()))) {
                values.add(value);
            }
        }
        return values.toArray(new HandleValue[values.size()]);
    }

    public static List<HandleValue> filterOnlyPublicValues(List<HandleValue> values) {
        List<HandleValue> res = null;
        for (int i = values.size() - 1; i >= 0; i--) {
            HandleValue value = values.get(i);
            if (!value.getAnyoneCanRead()) {
                if (res == null) res = new ArrayList<>(values);
                res.remove(i);
            }
        }
        if (res == null) return values;
        return res;
    }

    /**********************************************************************
     *  Get a passphrase from the user.
     **********************************************************************/
    /*
    public static final byte getPassphrase()
    throws Exception
    {
    return getPassphrase("Please enter the private key passphrase: ");
    }
     */

    /**********************************************************************
     *  Get a passphrase from the user.
     **********************************************************************/
    public static final byte[] getPassphrase(String prompt) throws Exception {
        byte passphrase[] = new byte[2048];
        int charIdx = 0;

        //        // read any characters that may already be entered but not read
        //        long endTime = System.currentTimeMillis() + 1000; // only read for a maximum of one second
        //        while(System.currentTimeMillis()<endTime && System.in.available()>0)
        //            System.in.read();

        System.out.println(prompt);
        System.out.println("Note: Your passphrase will be displayed as it is entered");
        System.out.flush();
        while (true) {
            int input = System.in.read();
            if (input == '\r') continue;
            if (input < 0 || input == '\n' /* || input=='\r'*/ ) break;
            passphrase[charIdx++] = (byte) input;
        }
        byte secKey[] = new byte[charIdx];
        System.arraycopy(passphrase, 0, secKey, 0, charIdx);
        for (int i = 0; i < passphrase.length; i++) {
            passphrase[i] = (byte) 0;
        }
        return secKey;
    }

    /** Get the ID that the handle protocol uses to identify the hash algorithm
     * used in the given signature algorithm descriptor.  */
    public static byte[] getHashAlgIdFromSigId(String signatureAlgorithm) throws HandleException {
        if (signatureAlgorithm.startsWith("SHA1")) return Common.HASH_ALG_SHA1;
        else if (signatureAlgorithm.startsWith("SHA256")) return Common.HASH_ALG_SHA256;
        else if (signatureAlgorithm.startsWith("MD5")) return Common.HASH_ALG_MD5;
        throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE, "Unknown signature algorithm: " + signatureAlgorithm);
    }

    public static String getSigIdFromHashAlgId(byte hashAlgId[], String sigKeyType) throws HandleException {
        if (Util.equals(hashAlgId, Common.HASH_ALG_SHA1) || Util.equals(hashAlgId, Common.HASH_ALG_SHA1_ALTERNATE)) return "SHA1with" + sigKeyType;
        else if (Util.equals(hashAlgId, Common.HASH_ALG_SHA256) || Util.equals(hashAlgId, Common.HASH_ALG_SHA256_ALTERNATE)) return "SHA256with" + sigKeyType;
        else if (Util.equals(hashAlgId, Common.HASH_ALG_MD5)) return "MD5with" + sigKeyType;
        else if (hashAlgId.length == 1 && hashAlgId[0] == Common.HASH_CODE_SHA1) return "SHA1with" + sigKeyType;
        else if (hashAlgId.length == 1 && hashAlgId[0] == Common.HASH_CODE_SHA256) return "SHA256with" + sigKeyType;
        else if (hashAlgId.length == 1 && hashAlgId[0] == Common.HASH_CODE_MD5) return "MD5with" + sigKeyType;
        throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE, "Unknown hash algorithm ID: " + Util.decodeString(hashAlgId));
    }

    public static String getDefaultSigId(String algorithm) {
        return "SHA256with" + algorithm;
    }

    public static String getDefaultSigId(String algorithm, AbstractMessage message) throws HandleException {
        if (message.hasEqualOrGreaterVersion(2, 7)) {
            String res = getDefaultSigId(algorithm);
            if ("SHA256withDSA".equals(res) && !message.hasEqualOrGreaterVersion(2, 11)) {
                res = "SHA1withDSA";
            }
            return res;
        }
        else return getSigIdFromHashAlgId(Common.HASH_ALG_SHA1, algorithm);
    }

    public static byte[] getBytesFromPrivateKey(PrivateKey key) throws Exception {
        if (key instanceof DSAPrivateKey) {
            DSAPrivateKey dsaKey = (DSAPrivateKey) key;
            byte x[] = dsaKey.getX().toByteArray();
            DSAParams params = dsaKey.getParams();
            byte p[] = params.getP().toByteArray();
            byte q[] = params.getQ().toByteArray();
            byte g[] = params.getG().toByteArray();
            byte enc[] = new byte[Encoder.INT_SIZE * 5 + Common.KEY_ENCODING_DSA_PRIVATE.length + x.length + p.length + q.length + g.length];
            int loc = 0;
            loc += Encoder.writeByteArray(enc, loc, Common.KEY_ENCODING_DSA_PRIVATE);
            loc += Encoder.writeByteArray(enc, loc, x);
            loc += Encoder.writeByteArray(enc, loc, p);
            loc += Encoder.writeByteArray(enc, loc, q);
            loc += Encoder.writeByteArray(enc, loc, g);
            return enc;
        } else if (key instanceof RSAPrivateKey) {
            RSAPrivateKey rsaKey = (RSAPrivateKey) key;
            if (rsaKey instanceof RSAPrivateCrtKey) {
                //instance is a RSA Private Crt key
                RSAPrivateCrtKey rsacrtKey = (RSAPrivateCrtKey) rsaKey;
                byte x[] = rsacrtKey.getModulus().toByteArray();
                byte ex[] = rsacrtKey.getPrivateExponent().toByteArray();
                byte pubEx[] = rsacrtKey.getPublicExponent().toByteArray();
                byte p[] = rsacrtKey.getPrimeP().toByteArray();
                byte q[] = rsacrtKey.getPrimeQ().toByteArray();
                byte exP[] = rsacrtKey.getPrimeExponentP().toByteArray();
                byte exQ[] = rsacrtKey.getPrimeExponentQ().toByteArray();
                byte coeff[] = rsacrtKey.getCrtCoefficient().toByteArray();

                byte enc[] = new byte[Encoder.INT_SIZE * 9 + Common.KEY_ENCODING_RSACRT_PRIVATE.length + x.length + ex.length + pubEx.length + p.length + q.length + exP.length + exQ.length + coeff.length];
                int loc = 0;
                loc += Encoder.writeByteArray(enc, loc, Common.KEY_ENCODING_RSACRT_PRIVATE);
                loc += Encoder.writeByteArray(enc, loc, x);
                loc += Encoder.writeByteArray(enc, loc, pubEx);
                loc += Encoder.writeByteArray(enc, loc, ex);
                loc += Encoder.writeByteArray(enc, loc, p);
                loc += Encoder.writeByteArray(enc, loc, q);
                loc += Encoder.writeByteArray(enc, loc, exP);
                loc += Encoder.writeByteArray(enc, loc, exQ);
                loc += Encoder.writeByteArray(enc, loc, coeff);
                return enc;
            } else {
                //just a RSA private Key
                byte x[] = rsaKey.getModulus().toByteArray();
                byte y[] = rsaKey.getPrivateExponent().toByteArray();
                byte enc[] = new byte[Encoder.INT_SIZE * 3 + Common.KEY_ENCODING_RSA_PRIVATE.length + x.length + y.length];
                int loc = 0;
                loc += Encoder.writeByteArray(enc, loc, Common.KEY_ENCODING_RSA_PRIVATE);
                loc += Encoder.writeByteArray(enc, loc, x);
                loc += Encoder.writeByteArray(enc, loc, y);
                return enc;
            }
        } else {
            throw new HandleException(HandleException.INVALID_VALUE, "Unknown private key type: \"" + key + '"');
        }
    }

    public static PrivateKey getPrivateKeyFromBytes(byte pkBuf[]) throws HandleException, InvalidKeySpecException {
        return getPrivateKeyFromBytes(pkBuf, 0);
    }

    public static PrivateKey getPrivateKeyFromBytes(byte pkBuf[], int offset) throws HandleException, InvalidKeySpecException {
        byte keyType[] = Encoder.readByteArray(pkBuf, offset);
        offset += Encoder.INT_SIZE + keyType.length;
        if (Util.equals(keyType, Common.KEY_ENCODING_DSA_PRIVATE)) {
            byte x[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + x.length;
            byte p[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + p.length;
            byte q[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + q.length;
            byte g[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + g.length;
            DSAPrivateKeySpec keySpec = new DSAPrivateKeySpec(new BigInteger(1, x), new BigInteger(1, p), new BigInteger(1, q), new BigInteger(1, g));
            try {
                KeyFactory dsaKeyFactory = KeyFactory.getInstance("DSA");
                return dsaKeyFactory.generatePrivate(keySpec);
            } catch (NoSuchAlgorithmException e) {
                throw new HandleException(HandleException.ENCRYPTION_ERROR, "DSA encryption not supported", e);
            }
        } else if (Util.equals(keyType, Common.KEY_ENCODING_RSA_PRIVATE)) {
            byte m[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + m.length;
            byte exp[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + exp.length;
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(new BigInteger(1, m), new BigInteger(1, exp));
            try {
                KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
                return rsaKeyFactory.generatePrivate(keySpec);
            } catch (NoSuchAlgorithmException e) {
                throw new HandleException(HandleException.ENCRYPTION_ERROR, "RSA encryption not supported", e);
            }
        } else if (Util.equals(keyType, Common.KEY_ENCODING_RSACRT_PRIVATE)) {
            byte n[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + n.length;
            byte pubEx[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + pubEx.length;
            byte ex[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + ex.length;
            byte p[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + p.length;
            byte q[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + q.length;
            byte exP[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + exP.length;
            byte exQ[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + exQ.length;
            byte coeff[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + coeff.length;
            RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(new BigInteger(1, n), new BigInteger(1, pubEx), new BigInteger(1, ex), new BigInteger(1, p), new BigInteger(1, q), new BigInteger(1, exP), new BigInteger(1, exQ),
                    new BigInteger(1, coeff));
            try {
                KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
                return rsaKeyFactory.generatePrivate(keySpec);
            } catch (NoSuchAlgorithmException e) {
                throw new HandleException(HandleException.ENCRYPTION_ERROR, "RSA encryption not supported", e);
            }
        }
        throw new HandleException(HandleException.INVALID_VALUE, "Unknown format for private key: \"" + Util.decodeString(keyType) + '"');
    }

    public static byte[] getBytesFromPublicKey(PublicKey key) throws HandleException {
        int flags = 0;
        if (key instanceof DSAPublicKey) {
            DSAPublicKey dsaKey = (DSAPublicKey) key;
            byte y[] = dsaKey.getY().toByteArray();
            DSAParams params = dsaKey.getParams();
            byte p[] = params.getP().toByteArray();
            byte q[] = params.getQ().toByteArray();
            byte g[] = params.getG().toByteArray();
            byte enc[] = new byte[Encoder.INT_SIZE * 5 + Common.KEY_ENCODING_DSA_PUBLIC.length + 2 + y.length + p.length + q.length + g.length];
            int loc = 0;
            loc += Encoder.writeByteArray(enc, loc, Common.KEY_ENCODING_DSA_PUBLIC);
            loc += Encoder.writeInt2(enc, loc, flags); // 2 octets reserved for future use
            loc += Encoder.writeByteArray(enc, loc, q);
            loc += Encoder.writeByteArray(enc, loc, p);
            loc += Encoder.writeByteArray(enc, loc, g);
            loc += Encoder.writeByteArray(enc, loc, y);
            return enc;
        } else if (key instanceof RSAPublicKey) {
            RSAPublicKey rsaKey = (RSAPublicKey) key;
            byte m[] = rsaKey.getModulus().toByteArray();
            byte ex[] = rsaKey.getPublicExponent().toByteArray();
            byte enc[] = new byte[Encoder.INT_SIZE * 4 + m.length + ex.length + 2 + Common.KEY_ENCODING_RSA_PUBLIC.length];
            int loc = 0;
            loc += Encoder.writeByteArray(enc, loc, Common.KEY_ENCODING_RSA_PUBLIC);
            loc += Encoder.writeInt2(enc, loc, flags); // 2 octets reserved for future use
            loc += Encoder.writeByteArray(enc, loc, ex);
            loc += Encoder.writeByteArray(enc, loc, m);
            return enc;
        } else if (key instanceof DHPublicKey) {
            DHPublicKey dhKey = (DHPublicKey) key;
            DHParameterSpec dhSpec = dhKey.getParams();
            byte y[] = dhKey.getY().toByteArray();
            byte p[] = dhSpec.getP().toByteArray();
            byte g[] = dhSpec.getG().toByteArray();
            byte enc[] = new byte[y.length + g.length + p.length + Encoder.INT_SIZE * 4 + Common.KEY_ENCODING_DH_PUBLIC.length + 2];
            int offset = Encoder.writeByteArray(enc, 0, Common.KEY_ENCODING_DH_PUBLIC);
            offset += Encoder.writeInt2(enc, offset, flags); // 2 octets reserved
            offset += Encoder.writeByteArray(enc, offset, y);
            offset += Encoder.writeByteArray(enc, offset, p);
            offset += Encoder.writeByteArray(enc, offset, g);
            return enc;
        } else {
            throw new HandleException(HandleException.INVALID_VALUE, "Unknown public key type: \"" + key + '"');
        }
    }

    public static PublicKey getPublicKeyFromFile(String filename) throws Exception {
        File f = new File(filename);
        FileInputStream in = new FileInputStream(f);
        byte buf[] = new byte[(int) f.length()];
        try {
            int r, n = 0;
            while ((r = in.read(buf, n, buf.length - n)) > 0) {
                n += r;
            }
        } finally {
            in.close();
        }
        return getPublicKeyFromBytes(buf, 0);
    }

    public static PublicKey getPublicKeyFromBytes(byte pkBuf[]) throws Exception {
        return getPublicKeyFromBytes(pkBuf, 0);
    }

    public static PublicKey getPublicKeyFromBytes(byte pkBuf[], int offset) throws Exception {
        byte keyType[] = Encoder.readByteArray(pkBuf, offset);
        offset += Encoder.INT_SIZE + keyType.length;
        // int flags = Encoder.readInt2(pkBuf, offset); // currently not used... reserved
        offset += Encoder.INT2_SIZE;
        if (Util.equals(keyType, Common.KEY_ENCODING_DSA_PUBLIC)) {
            byte q[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + q.length;
            byte p[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + p.length;
            byte g[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + g.length;
            byte y[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + y.length;

            DSAPublicKeySpec keySpec = new DSAPublicKeySpec(new BigInteger(1, y), new BigInteger(1, p), new BigInteger(1, q), new BigInteger(1, g));
            try {
                KeyFactory dsaKeyFactory = KeyFactory.getInstance("DSA");
                return dsaKeyFactory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException e) {
                throw new HandleException(HandleException.ENCRYPTION_ERROR, "DSA encryption not supported", e);
            }
        } else if (Util.equals(keyType, Common.KEY_ENCODING_RSA_PUBLIC)) {
            byte ex[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + ex.length;
            byte m[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + m.length;
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(new BigInteger(1, m), new BigInteger(1, ex));
            try {
                KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
                return rsaKeyFactory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException e) {
                throw new HandleException(HandleException.ENCRYPTION_ERROR, "RSA encryption not supported", e);
            }
        } else if (Util.equals(keyType, Common.KEY_ENCODING_DH_PUBLIC)) {

            byte y[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + y.length;
            byte p[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + p.length;
            byte g[] = Encoder.readByteArray(pkBuf, offset);
            offset += Encoder.INT_SIZE + g.length;
            DHPublicKeySpec keySpec = new DHPublicKeySpec(new BigInteger(1, y), new BigInteger(1, p), new BigInteger(1, g));
            try {
                KeyFactory dhKeyFactory = KeyFactory.getInstance("DiffieHellman");
                return dhKeyFactory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException e) {
                throw new HandleException(HandleException.ENCRYPTION_ERROR, "DH encryption not supported", e);
            }
        }
        throw new HandleException(HandleException.INVALID_VALUE, "Unknown format for public key: \"" + Util.decodeString(keyType) + '"');
    }

    public static List<PublicKey> getPublicKeysFromValues(HandleValue[] values) {
        List<PublicKey> keys = new ArrayList<>();
        for (HandleValue value : values) {
            if (value.hasType(Common.PUBLIC_KEY_TYPE)) {
                try {
                    keys.add(Util.getPublicKeyFromBytes(value.getData()));
                } catch (Exception e) {
                    System.err.println("Error parsing key " + value);
                    // ignore
                }
            }
        }
        return keys;
    }

    /********************************************************************
     * Encrypt the given set of bytes using the specified secret key and
     * the default encryption algorithm.
     ********************************************************************/
    public static byte[] encrypt(byte cleartext[], byte secretKey[]) throws Exception {
        if (secretKey == null) {
            return encrypt(cleartext, null, Common.ENCRYPT_NONE);
        } else {
            return encrypt(cleartext, secretKey, Common.ENCRYPT_PBKDF2_AES_CBC_PKCS5);
        }
    }

    /******************************************************************
     * Encrypt the given set of bytes using the specified secret key
     * and encryption algorithm.
     ******************************************************************/
    public static byte[] encrypt(byte cleartext[], byte secretKey[], int encType) throws Exception {
        HdlSecurityProvider cryptoProvider = HdlSecurityProvider.getInstance();

        byte enc[];
        byte enc2[];
        switch (encType) {
            case SUPPRESS_WARNINGS_ENCRYPT_DES_ECB_PKCS5:
                if (cryptoProvider == null) {
                    throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "Encryption engine missing");
                }

                secretKey = doMD5Digest(secretKey);
                Cipher encryptCipher = cryptoProvider.getCipher(HdlSecurityProvider.ENCRYPT_ALG_DES, secretKey, Cipher.ENCRYPT_MODE, null, 2, 0);
                enc = encryptCipher.doFinal(cleartext, 0, cleartext.length);
                enc2 = new byte[enc.length + Encoder.INT_SIZE];
                Encoder.writeInt(enc2, 0, encType);
                System.arraycopy(enc, 0, enc2, Encoder.INT_SIZE, enc.length);
                return enc2;
            case Common.ENCRYPT_DES_CBC_PKCS5:
                if (cryptoProvider == null) {
                    throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "Encryption engine missing");
                }

                secretKey = doMD5Digest(secretKey);
                encryptCipher = cryptoProvider.getCipher(HdlSecurityProvider.ENCRYPT_ALG_DES, secretKey, Cipher.ENCRYPT_MODE, null, 2, 4);
                enc = encryptCipher.doFinal(cleartext, 0, cleartext.length);
                byte[] iv = encryptCipher.getIV();
                enc2 = new byte[enc.length + iv.length + Encoder.INT_SIZE];
                Encoder.writeInt(enc2, 0, encType);
                System.arraycopy(iv, 0, enc2, Encoder.INT_SIZE, iv.length);
                System.arraycopy(enc, 0, enc2, Encoder.INT_SIZE + iv.length, enc.length);
                return enc2;
            case Common.ENCRYPT_PBKDF2_DESEDE_CBC_PKCS5:
                if (cryptoProvider == null) {
                    throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "Encryption engine missing");
                }

                byte[] salt = new byte[16];
                SECURE_RANDOM.nextBytes(salt);
                int iterations = 10000;
                secretKey = doPBKDF2(secretKey, salt, iterations, 192);
                encryptCipher = cryptoProvider.getCipher(HdlSecurityProvider.ENCRYPT_ALG_DESEDE, secretKey, Cipher.ENCRYPT_MODE, null, 2, 4);
                enc = encryptCipher.doFinal(cleartext, 0, cleartext.length);
                iv = encryptCipher.getIV();
                enc2 = new byte[Encoder.INT_SIZE + Encoder.INT_SIZE + salt.length + Encoder.INT_SIZE + Encoder.INT_SIZE + Encoder.INT_SIZE + iv.length + Encoder.INT_SIZE + enc.length];
                int offset = 0;
                offset += Encoder.writeInt(enc2, offset, encType);
                offset += Encoder.writeByteArray(enc2, offset, salt);
                offset += Encoder.writeInt(enc2, offset, iterations);
                offset += Encoder.writeInt(enc2, offset, 192);
                offset += Encoder.writeByteArray(enc2, offset, iv);
                offset += Encoder.writeByteArray(enc2, offset, enc);
                return enc2;
            case Common.ENCRYPT_PBKDF2_AES_CBC_PKCS5:
                if (cryptoProvider == null) {
                    throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "Encryption engine missing");
                }

                salt = new byte[16];
                SECURE_RANDOM.nextBytes(salt);
                iterations = 10000;
                secretKey = doPBKDF2(secretKey, salt, iterations, 128);
                encryptCipher = cryptoProvider.getCipher(HdlSecurityProvider.ENCRYPT_ALG_AES, secretKey, Cipher.ENCRYPT_MODE, null, 2, 4);
                enc = encryptCipher.doFinal(cleartext, 0, cleartext.length);
                iv = encryptCipher.getIV();
                enc2 = new byte[Encoder.INT_SIZE + Encoder.INT_SIZE + salt.length + Encoder.INT_SIZE + Encoder.INT_SIZE + Encoder.INT_SIZE + iv.length + Encoder.INT_SIZE + enc.length];
                offset = 0;
                offset += Encoder.writeInt(enc2, offset, encType);
                offset += Encoder.writeByteArray(enc2, offset, salt);
                offset += Encoder.writeInt(enc2, offset, iterations);
                offset += Encoder.writeInt(enc2, offset, 128);
                offset += Encoder.writeByteArray(enc2, offset, iv);
                offset += Encoder.writeByteArray(enc2, offset, enc);
                return enc2;
            case Common.ENCRYPT_NONE:
                enc2 = new byte[cleartext.length + Encoder.INT_SIZE];
                Encoder.writeInt(enc2, 0, encType);
                System.arraycopy(cleartext, 0, enc2, Encoder.INT_SIZE, cleartext.length);
                return enc2;
            default:
                throw new HandleException(HandleException.UNKNOWN_ALGORITHM_ID, "Unknown algorithm ID: " + encType);
        }
    }

    public static byte[] doPBKDF2(byte[] password, byte[] salt, int iterations, int length) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        char[] charPassword = new char[password.length];
        for (int i = 0; i < password.length; i++) {
            charPassword[i] = (char) (password[i] & 0xFF);
        }
        KeySpec spec = new PBEKeySpec(charPassword, salt, iterations, length);
        SecretKey tmp = factory.generateSecret(spec);
        return tmp.getEncoded();
    }

    public static byte[] constructPbkdf2Encoding(byte[] salt, int iterations, int keyLength, byte[] mac) {
        byte[] res = new byte[3 * Encoder.INT_SIZE + salt.length + Encoder.INT_SIZE + mac.length];
        int offset = 0;
        offset += Encoder.writeByteArray(res, offset, salt);
        offset += Encoder.writeInt(res, offset, iterations);
        offset += Encoder.writeInt(res, offset, keyLength);
        offset += Encoder.writeByteArray(res, offset, mac);
        return res;
    }

    /********************************************************************
     * Returns true is the given ciphertext requires a secret key to be
     *    decrypted (ie if the encryption algorithm is ENCRYPT_NONE).
     ********************************************************************/
    public static final boolean requiresSecretKey(byte ciphertext[]) throws Exception {
        int encryptionType = Encoder.readInt(ciphertext, 0);
        if (encryptionType == Common.ENCRYPT_NONE) return false;
        return true;
    }

    @SuppressWarnings("deprecation")
    private static final int SUPPRESS_WARNINGS_ENCRYPT_DES_ECB_PKCS5 = Common.ENCRYPT_DES_ECB_PKCS5;

    /******************************************************************
     * Decrypt the given set of bytes using the specified secret key
     ******************************************************************/
    public static byte[] decrypt(byte ciphertext[], byte secretKey[]) throws Exception {
        HdlSecurityProvider cryptoProvider = HdlSecurityProvider.getInstance();

        int encryptionType = Encoder.readInt(ciphertext, 0);
        switch (encryptionType) {
            case SUPPRESS_WARNINGS_ENCRYPT_DES_ECB_PKCS5:
                if (cryptoProvider == null) {
                    throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "Encryption engine missing");
                }
                secretKey = doMD5Digest(secretKey);
                try {
                    Cipher decryptCipher = cryptoProvider.getCipher(HdlSecurityProvider.ENCRYPT_ALG_DES, secretKey, Cipher.DECRYPT_MODE, null, 2, 3);
                    return decryptCipher.doFinal(ciphertext, Encoder.INT_SIZE, ciphertext.length - Encoder.INT_SIZE);
                } catch (Exception e) {
                    //e.printStackTrace(System.err);
                    throw new Exception("Unable to decrypt");
                }
            case Common.ENCRYPT_DES_CBC_PKCS5:
                if (cryptoProvider == null) {
                    throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "Encryption engine missing");
                }
                secretKey = doMD5Digest(secretKey);
                try {
                    byte[] iv = Util.substring(ciphertext, 4, 12);
                    ciphertext = Util.substring(ciphertext, 12);
                    Cipher decryptCipher = cryptoProvider.getCipher(HdlSecurityProvider.ENCRYPT_ALG_DES, secretKey, Cipher.DECRYPT_MODE, iv, 2, 4);
                    return decryptCipher.doFinal(ciphertext, 0, ciphertext.length);
                } catch (Exception e) {
                    //e.printStackTrace(System.err);
                    throw new Exception("Unable to decrypt");
                }
            case Common.ENCRYPT_PBKDF2_DESEDE_CBC_PKCS5:
                if (cryptoProvider == null) {
                    throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "Encryption engine missing");
                }
                try {
                    int offset = 4;
                    byte[] salt = Encoder.readByteArray(ciphertext, offset);
                    offset += Encoder.INT_SIZE + salt.length;
                    int iterations = Encoder.readInt(ciphertext, offset);
                    offset += Encoder.INT_SIZE;
                    int keyLength = Encoder.readInt(ciphertext, offset);
                    offset += Encoder.INT_SIZE;
                    secretKey = doPBKDF2(secretKey, salt, iterations, keyLength);
                    byte[] iv = Encoder.readByteArray(ciphertext, offset);
                    offset += Encoder.INT_SIZE + iv.length;
                    ciphertext = Encoder.readByteArray(ciphertext, offset);
                    Cipher decryptCipher = cryptoProvider.getCipher(HdlSecurityProvider.ENCRYPT_ALG_DESEDE, secretKey, Cipher.DECRYPT_MODE, iv, 2, 4);
                    return decryptCipher.doFinal(ciphertext, 0, ciphertext.length);
                } catch (Exception e) {
                    //e.printStackTrace(System.err);
                    throw new Exception("Unable to decrypt");
                }
            case Common.ENCRYPT_PBKDF2_AES_CBC_PKCS5:
                if (cryptoProvider == null) {
                    throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "Encryption engine missing");
                }
                try {
                    int offset = 4;
                    byte[] salt = Encoder.readByteArray(ciphertext, offset);
                    offset += Encoder.INT_SIZE + salt.length;
                    int iterations = Encoder.readInt(ciphertext, offset);
                    offset += Encoder.INT_SIZE;
                    int keyLength = Encoder.readInt(ciphertext, offset);
                    offset += Encoder.INT_SIZE;
                    secretKey = doPBKDF2(secretKey, salt, iterations, keyLength);
                    byte[] iv = Encoder.readByteArray(ciphertext, offset);
                    offset += Encoder.INT_SIZE + iv.length;
                    ciphertext = Encoder.readByteArray(ciphertext, offset);
                    Cipher decryptCipher = cryptoProvider.getCipher(HdlSecurityProvider.ENCRYPT_ALG_AES, secretKey, Cipher.DECRYPT_MODE, iv, 2, 4);
                    return decryptCipher.doFinal(ciphertext, 0, ciphertext.length);
                } catch (Exception e) {
                    //e.printStackTrace(System.err);
                    throw new Exception("Unable to decrypt");
                }
            case Common.ENCRYPT_NONE:
                byte cleartext[] = new byte[ciphertext.length - Encoder.INT_SIZE];
                System.arraycopy(ciphertext, Encoder.INT_SIZE, cleartext, 0, cleartext.length);
                return cleartext;
            default:
                throw new HandleException(HandleException.INVALID_VALUE, "Unknown encryption type code: " + encryptionType);
        }
    }

    private static final MessageDigest getSHA1Digest() throws HandleException {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "SHA1 algorithm not found", e);
        }
    }

    private static final MessageDigest getSHA256Digest() throws HandleException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "SHA-256 algorithm not found", e);
        }
    }

    public static final byte[] doSHA1Digest(byte[]... bufs) throws HandleException {
        MessageDigest digest = getSHA1Digest();
        for (byte[] buf : bufs) {
            digest.update(buf);
        }
        return digest.digest();
    }

    public static byte[] doHmacSHA1(byte[] buf, byte[] key) throws HandleException {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(buf);
        } catch (NoSuchAlgorithmException e) {
            throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "HmacSHA1 algorithm not found", e);
        } catch (InvalidKeyException e) {
            throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "HmacSHA1 key error", e);
        }
    }

    public static byte[] doHmacSHA256(byte[] buf, byte[] key) throws HandleException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(buf);
        } catch (NoSuchAlgorithmException e) {
            throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "HmacSHA256 algorithm not found", e);
        } catch (InvalidKeyException e) {
            throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "HmacSHA256 key error", e);
        }
    }

    public static byte[] doPbkdf2HmacSHA1(byte[] buf, byte[] key, byte[] paramsToMatch) throws HandleException {
        byte[] salt;
        int iterations;
        int keyLength;
        if (paramsToMatch != null) {
            int offset = 0;
            salt = Encoder.readByteArray(paramsToMatch, offset);
            offset += Encoder.INT_SIZE + salt.length;
            iterations = Encoder.readInt(paramsToMatch, offset);
            offset += Encoder.INT_SIZE;
            keyLength = Encoder.readInt(paramsToMatch, offset);
            offset += Encoder.INT_SIZE;
        } else {
            salt = new byte[16];
            SECURE_RANDOM.nextBytes(salt);
            iterations = 10000;
            keyLength = 160;
        }
        try {
            byte[] derivedKey = doPBKDF2(key, salt, iterations, keyLength);
            byte[] mac = doHmacSHA1(buf, derivedKey);
            return Util.constructPbkdf2Encoding(salt, iterations, keyLength, mac);
        } catch (NoSuchAlgorithmException e) {
            throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "PBKDF2WithHmacSHA1 algorithm not found", e);
        } catch (InvalidKeySpecException e) {
            throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "PBKDF2WithHmacSHA1 key error", e);
        }
    }

    public static byte[] doSHA256Digest(byte[]... bufs) throws HandleException {
        MessageDigest digest = getSHA256Digest();
        for (byte[] buf : bufs) {
            digest.update(buf);
        }
        return digest.digest();
    }

    private static final MessageDigest getMD5Digest() throws HandleException {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "MD-5 algorithm not found", e);
        }
    }

    public static final byte[] doMD5Digest(byte[]... bufs) throws HandleException {
        MessageDigest digest = getMD5Digest();
        for (byte[] buf : bufs) {
            digest.update(buf);
        }
        return digest.digest();
    }

    public static final byte[] doDigest(byte digestType, byte[]... bufs) throws HandleException {
        switch (digestType) {
            case Common.HASH_CODE_SHA256:
                return doSHA256Digest(bufs);
            case Common.HASH_CODE_SHA1:
                return doSHA1Digest(bufs);
            case Common.HASH_CODE_MD5:
            case Common.HASH_CODE_MD5_OLD_FORMAT:
                return doMD5Digest(bufs);
            default:
                throw new HandleException(HandleException.INVALID_VALUE, "Invalid hash type: " + ((int) digestType));
        }
    }

    public static final byte[] doMac(byte digestType, byte buf[], byte[] key) throws HandleException {
        return doMac(digestType, buf, key, null);
    }

    public static final byte[] doMac(byte digestType, byte buf[], byte[] key, byte[] paramsToMatch) throws HandleException {
        switch (digestType) {
            case Common.HASH_CODE_PBKDF2_HMAC_SHA1:
                return doPbkdf2HmacSHA1(buf, key, paramsToMatch);
            case Common.HASH_CODE_HMAC_SHA256:
                return doHmacSHA256(buf, key);
            case Common.HASH_CODE_HMAC_SHA1:
                return doHmacSHA1(buf, key);
            case Common.HASH_CODE_SHA256:
                return doSHA256Digest(key, buf, key);
            case Common.HASH_CODE_SHA1:
                return doSHA1Digest(key, buf, key);
            case Common.HASH_CODE_MD5:
            case Common.HASH_CODE_MD5_OLD_FORMAT:
                return doMD5Digest(key, buf, key);
            default:
                throw new HandleException(HandleException.INVALID_VALUE, "Invalid hash type: " + ((int) digestType));
        }
    }

    public static final byte[] doDigest(byte[] digestType, byte[]... bufs) throws HandleException {
        if (digestType.length == 1) return doDigest(digestType[0], bufs);
        if (Util.equals(digestType, Common.HASH_ALG_SHA1) || Util.equals(digestType, Common.HASH_ALG_SHA1_ALTERNATE)) return doDigest(Common.HASH_CODE_SHA1, bufs);
        else if (Util.equals(digestType, Common.HASH_ALG_MD5)) return doDigest(Common.HASH_CODE_MD5, bufs);
        else if (Util.equals(digestType, Common.HASH_ALG_SHA256) || Util.equals(digestType, Common.HASH_ALG_SHA256_ALTERNATE)) return doDigest(Common.HASH_CODE_SHA256, bufs);
        else throw new HandleException(HandleException.INVALID_VALUE, "Invalid hash type: " + Util.decodeString(digestType));
    }

    public static final byte[] doMac(byte[] digestType, byte buf[], byte[] key) throws HandleException {
        if (digestType.length == 1) return doMac(digestType[0], buf, key);
        if (Util.equals(digestType, Common.HASH_ALG_SHA1) || Util.equals(digestType, Common.HASH_ALG_SHA1_ALTERNATE)) return doMac(Common.HASH_CODE_SHA1, buf, key);
        else if (Util.equals(digestType, Common.HASH_ALG_MD5)) return doMac(Common.HASH_CODE_MD5, buf, key);
        else if (Util.equals(digestType, Common.HASH_ALG_SHA256) || Util.equals(digestType, Common.HASH_ALG_SHA256_ALTERNATE)) return doMac(Common.HASH_CODE_SHA256, buf, key);
        else if (Util.equalsIgnoreCaseAndPunctuation(digestType, Common.HASH_ALG_HMAC_SHA1)) return doMac(Common.HASH_CODE_HMAC_SHA1, buf, key);
        else if (Util.equalsIgnoreCaseAndPunctuation(digestType, Common.HASH_ALG_HMAC_SHA256)) return doMac(Common.HASH_CODE_HMAC_SHA256, buf, key);
        else throw new HandleException(HandleException.INVALID_VALUE, "Invalid hash type: " + Util.decodeString(digestType));
    }

    public static boolean equalsIgnoreCaseAndPunctuation(byte[] a, byte[] b) {
        int i = 0, j = 0;
        while (true) {
            if (i >= a.length && j >= b.length) return true;
            byte ach = i >= a.length ? 0 : a[i];
            byte bch = j >= b.length ? 0 : b[j];
            if (ach == bch) {
                i++;
                j++;
                continue;
            }
            if (ach >= 'a' && ach <= 'z') ach += 'A' - 'a';
            if (i >= a.length || (ach >= 'A' && ach <= 'Z') || (ach >= '0' && ach <= '9')) {
                if (bch >= 'a' && bch <= 'z') bch += 'A' - 'a';
                if (j >= b.length || (bch >= 'A' && bch <= 'Z') || (bch >= '0' && bch <= '9')) {
                    if (ach == bch) {
                        i++;
                        j++;
                        continue;
                    } else return false;
                } else {
                    j++;
                    continue;
                }
            } else {
                i++;
                continue;
            }
        }
    }

    public static void sortNumberArray(Number a[]) {
        quicksortAscending(a, 0, a.length - 1);
    }

    private static void quicksortAscending(Number a[], int first, int last) {
        int piv_index;
        if (first < last) {
            piv_index = partitionAscending(a, first, last);
            quicksortAscending(a, first, piv_index - 1);
            quicksortAscending(a, piv_index, last);
        }
    }

    private static int partitionAscending(Number a[], int first, int last) {
        Number pivot, temp;
        pivot = a[(first + last) / 2];
        while (first <= last) {
            while (a[first].doubleValue() < pivot.doubleValue()) {
                ++first;
            }
            while (a[last].doubleValue() > pivot.doubleValue()) {
                --last;
            }
            if (first <= last) {
                temp = a[first];
                a[first] = a[last];
                a[last] = temp;
                ++first;
                --last;
            }
        }
        return (first);
    }

    @Deprecated
    public static byte[] encrypt(PublicKey encryptingKey, byte secretKey[]) throws Exception {
        return encrypt(encryptingKey, secretKey, 2, 0);
    }

    /** encrypt with Public key */
    @SuppressWarnings("unused")
    public static byte[] encrypt(PublicKey encryptingKey, byte secretKey[], int majorProtocolVersion, int minorProtocolVersion) throws Exception {
        if (encryptingKey != null && encryptingKey instanceof RSAPublicKey) {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1","BC");
            cipher.init(Cipher.ENCRYPT_MODE, encryptingKey);
            return cipher.doFinal(secretKey);
        } else {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Unsupported key for encrypt: " + encryptingKey);
        }

    }

    /** convert a file into a byte stream */
    public static byte[] getBytesFromFile(String file) {
        return getBytesFromFile(new File(file));
    }

    public static byte[] getBytesFromFile(File file) {
        byte[] rawKey = null;
        try {
            rawKey = new byte[(int) file.length()];
            InputStream in = new FileInputStream(file);
            int n = 0;
            int r = 0;
            while (n < rawKey.length && (r = in.read(rawKey, n, rawKey.length - n)) > 0) {
                n += r;
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return rawKey;
    }

    public static byte[] getBytesFromInputStream(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r = 0;
        while ((r = in.read(buf)) > 0) {
            bout.write(buf, 0, r);
        }
        return bout.toByteArray();
    }

    /**
     * Like in.read(b), but attempts to read as many bytes as possible
     */
    public static void readFully(InputStream in, byte[] b) throws IOException {
        readFully(in, b, 0, b.length);
    }

    /**
     * Like in.read(b, off, len), but attempts to read as many bytes as possible
     */
    public static void readFully(InputStream in, byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        int n = 0;
        int r;
        while ((r = in.read(b, off + n, len - n)) > 0) {
            n += r;
        }
        if (n < len) throw new EOFException();
    }

    /** write byte array into a given file name */
    public static boolean writeBytesToFile(String file, byte keyBytes[]) {
        return writeBytesToFile(new File(file), keyBytes);
    }

    public static boolean writeBytesToFile(File file, byte keyBytes[]) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(keyBytes);
            return true;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    /** check that a given PublicKey and a given PrivateKey are a pair */
    public static boolean isMatchingKeyPair(PublicKey pubkey, PrivateKey privkey) throws HandleException {
        if (pubkey == null && privkey != null) return false;
        if (pubkey != null && privkey == null) return false;
        if (pubkey == null) return true;
        if (!pubkey.getAlgorithm().equals(privkey.getAlgorithm())) return false;

        // compute the signature of the message bytes
        try {
            byte toBeSigned[] = new byte[2048];
            new java.util.Random().nextBytes(toBeSigned);
            String alg = Util.getDefaultSigId(privkey.getAlgorithm());
            Signature sig = Signature.getInstance(alg);
            sig.initSign(privkey);
            sig.update(toBeSigned);
            byte signatureBytes[] = sig.sign();
            sig.initVerify(pubkey);
            sig.update(toBeSigned);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            if (e instanceof HandleException) {
                throw (HandleException) e;
            } else {
                throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE, "Error checking keys: " + e);
            }
        }
    }

    @Deprecated
    public static byte[] decrypt(PrivateKey privKey, byte[] ciphertext) throws Exception {
        return decrypt(privKey, ciphertext, 2, 0);
    }

    // both sever and client can call this method to decrypt the session key
    // with RSA private key.
    // the encryptedSessionKey byte array is encrypted with RSA public key.
    @SuppressWarnings("unused")
    public static byte[] decrypt(PrivateKey privKey, byte[] ciphertext, int majorProtocolVersion, int minorProtocolVersion) throws Exception {
        if (privKey != null && privKey instanceof RSAPrivateKey) {
            Cipher cipher = Cipher.getInstance("RSA/EBC/PKCS1");
            cipher.init(Cipher.DECRYPT_MODE, privKey);
            return cipher.doFinal(ciphertext);
        } else {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Unsupported key for decrypt: " + privKey);
        }
    }

    /* Method for getting PrivateKey from File */
    public static PrivateKey getPrivateKeyFromFileWithPassphrase(File privKeyFile, String passphrase) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        FileInputStream fin = new FileInputStream(privKeyFile);
        byte[] buf = new byte[1024];
        int r = 0;

        /* read the private key file into memory */
        try {
            while ((r = fin.read(buf)) >= 0) {
                bout.write(buf, 0, r);
            }
        } finally {
            fin.close();
        }
        byte[] privateKeyFileContents = bout.toByteArray();

        /* if private key requires a passphrase, get bytes of passphrase string*/
        byte[] passphraseBytes = null;
        if (passphrase != null && Util.requiresSecretKey(privateKeyFileContents)) {
            passphraseBytes = passphrase.getBytes("UTF-8");
        }

        /* decrypt the private key */
        byte[] decryptedPrivateKey = Util.decrypt(privateKeyFileContents, passphraseBytes);

        /* return a java.security.PrivateKey */
        return Util.getPrivateKeyFromBytes(decryptedPrivateKey, 0);
    }

    public static byte[] concat(byte[] first, byte[] second) {
        if (second.length == 0) return first;
        if (first.length == 0) return second;
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static String getAccessLogString(AbstractRequest req, AbstractResponse resp) {
        if (req == null) return " /";
        StringBuilder sb = new StringBuilder();
        try {
            if (req.authInfo != null) {
                sb.append("adm=").append(req.authInfo.getUserIdIndex()).append(":");
                encodeForAccessLog(sb, Util.decodeString(req.authInfo.getUserIdHandle()));
            }
            sb.append(" ");
        } catch (Exception e) {
            sb.setLength(0);
        }
        byte[] handle = null;
        if (resp instanceof ResolutionResponse) handle = ((ResolutionResponse) resp).handle;
        else if (resp instanceof CreateHandleResponse) handle = ((CreateHandleResponse) resp).handle;
        if (handle == null) handle = req.handle;
        encodeForAccessLog(sb, Util.decodeString(handle));
        return sb.toString();
    }

    private static void encodeForAccessLog(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '%') sb.append("%25");
            else if (ch == ' ') sb.append("%20");
            else if (ch == '\n') sb.append("%0A");
            else if (ch == '\r') sb.append("%0D");
            else if (ch == '&') sb.append("%26");
            else if (ch == '?') sb.append("%3F");
            else if (ch == '#') sb.append("%23");
            else if (ch == '"') sb.append("%22");
            else sb.append(ch);
        }
    }

    /**
     * Reads a SiteInfo from a file formatted as either siteinfo.bin or siteinfo.json.
     */
    public static SiteInfo getSiteFromFile(String filename) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(filename));
        return getSiteFromBytes(data);
    }

    public static SiteInfo getSiteFromBytes(byte[] data) throws Exception {
        if (Util.looksLikeBinary(data)) {
            return Encoder.decodeSiteInfoRecord(data, 0);
        } else {
            return SiteInfoConverter.convertToSiteInfo(new String(data, "UTF-8"));
        }
    }
}
