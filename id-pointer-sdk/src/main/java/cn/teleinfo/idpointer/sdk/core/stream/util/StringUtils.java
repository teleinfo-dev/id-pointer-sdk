package cn.teleinfo.idpointer.sdk.core.stream.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public class StringUtils {
    private static final char HEX_VALUES[] = {'0','1','2','3','4','5','6','7',
            '8','9','A','B','C','D','E','F'};

    /**
     * Return a copy of the given string with the characters in escChars
     * (and any backslashes) escaped with backslashes.  The unbackslash call
     * can be used to return the string to its original state.
     */
    public static final String backslash(String str, String escChars) {
        int len = str.length();
        int currPos = 0;
        StringBuilder sb = new StringBuilder(str.length());
        char ch;
        while(currPos<len) {
            ch = str.charAt(currPos);
            if(ch=='\\' || escChars.indexOf(ch)>=0) {
                sb.append('\\');
                sb.append(ch);
            } else {
                sb.append(ch);
            }
            currPos++;
        }
        return sb.toString();
    }

    /**
     * Return a copy of the given string with the characters in escChars
     * (and any backslashes) unescaped with backslashes.  This should perform 
     * the reverse of the backslash() call, given the same escChars 
     * parameter.
     */
    public static final String unbackslash(String str) {
        int len = str.length();
        int currPos = 0;
        char backslash = '\\';
        StringBuilder sb = new StringBuilder(str.length());
        while(currPos<len) {
            char ch = str.charAt(currPos++);
            if(ch==backslash && currPos<len) {
                sb.append(str.charAt(currPos++));  // ignore backslash, append char
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /** decodes the special characters in a URL encoded string *except* for
     * the + to space conversion. */
    public static String decodeURLIgnorePlus(String str) {
        byte utf8Buf[] = new byte[str.length()];
        int utf8Loc = 0;
        int strLoc = 0;
        int strLen = str.length();
        while(strLoc < strLen) {
            char ch = str.charAt(strLoc++);
            if(ch=='%' && strLoc+2<=strLen) {
                utf8Buf[utf8Loc++] = decodeHexByte(str.charAt(strLoc++),
                    str.charAt(strLoc++));
            } else {
                utf8Buf[utf8Loc++] = (byte)ch;
            }
        }

        try {
            return new String(utf8Buf, 0, utf8Loc, "UTF8");
        } catch (Exception e) {
            return new String(utf8Buf, 0, utf8Loc);
        }
    }

    /** decodes the special characters in a URL encoded string. */
    public static String decodeURL(String str) {
        byte utf8Buf[] = new byte[str.length()];
        int utf8Loc = 0;
        int strLoc = 0;
        int strLen = str.length();
        while(strLoc < strLen) {
            char ch = str.charAt(strLoc++);
            if(ch=='%' && strLoc+2<=strLen) {
                utf8Buf[utf8Loc++] = decodeHexByte(str.charAt(strLoc++),
                    str.charAt(strLoc++));
            } else if(ch=='+') {
                utf8Buf[utf8Loc++] = (byte)' ';
            } else {
                utf8Buf[utf8Loc++] = (byte)ch;
            }
        }

        try {
            return new String(utf8Buf, 0, utf8Loc, "UTF8");
        } catch (Exception e) {
            return new String(utf8Buf, 0, utf8Loc);
        } 
    }

    public static final byte decodeHexByte(char ch1, char ch2) {
        char n1 = (char) ((ch1>='0' && ch1<='9') ? ch1-'0' : ((ch1>='a' && ch1<='z') ? ch1-'a'+10 : ch1-'A'+10));
        char n2 = (char) ((ch2>='0' && ch2<='9') ? ch2-'0' : ((ch2>='a' && ch2<='z') ? ch2-'a'+10 : ch2-'A'+10));
        return (byte)(n1<<4 | n2);
    }

    public static String encodeHexChar(byte b) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEX_VALUES[(b&0xF0)>>4]);
        sb.append(HEX_VALUES[b&0x0F]);
        return sb.toString();
    }

    public static final String padr(String str, int n, char padchar) {
        StringBuilder sb = new StringBuilder(str);
        while(sb.length()<n) sb.append(padchar);
        return sb.toString();
    }

    public static final String padl(String str, int n, char padchar) {
        StringBuilder sb = new StringBuilder(str);
        while(sb.length()<n) sb.insert(0,padchar);
        return sb.toString();
    }

    /**
     * Allows arbitrary text to be embedded safely in HTML or XML
     */
    public static String cgiEscape(String str) {
        if(str==null) return "null";
        StringBuilder sb = new StringBuilder("");
        for(int i=0;i<str.length();i++) {
            char ch = str.charAt(i);
            if(ch=='<')
                sb.append("&lt;");
            else if(ch=='>')
                sb.append("&gt;");
            else if(ch=='"')
                sb.append("&quot;");
            else if(ch=='&')
                sb.append("&amp;");
            else if(ch=='\'') 
                sb.append("&#39;");
            else
                sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Allows arbitrary text to be embedded safely in HTML or XML.  Deals with whitespace normalization.  The 'quot' and 'apos' parameters determine which conversions are performed
     * (use 'quot' true for attributes in double-quotes and 'apos' true for attributes in single-quotes).
     */
    public static void xmlEscape(Appendable buf, CharSequence s, boolean quot, boolean apos) throws IOException {
        int len = s.length();
        for(int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if(c=='&') buf.append("&amp;");
            else if(c=='<') buf.append("&lt;");
            else if(c=='>') buf.append("&gt;");
            else if(c=='"' && quot) buf.append("&quot;");
            else if(c=='\'' && apos) buf.append("&#39;");
            else if(c==0x9) buf.append("&#x9;");
            else if(c==0xD) buf.append("&#xD;");
            else if(c==0xA && (quot || apos)) buf.append("&#xA;"); // escape newlines in attributes
            else buf.append(c);
        }
    }

    /**
     * Allows arbitrary text to be embedded safely in HTML or XML.  Deals with whitespace normalization.  The 'attribute' parameter determines which conversions are performed.
     */
    public static String xmlEscape(CharSequence s, boolean attribute) {
        StringBuilder sb = new StringBuilder();
        try {
            xmlEscape(sb,s,attribute,attribute);
        }
        catch(IOException e) {
            throw new AssertionError(e);
        }
        return sb.toString();
    }

    /**
     * Allows arbitrary text to be embedded safely in HTML or XML.  Deals with whitespace normalization.  This version does all conversions (e.g. as for an attribute).
     * Note that this escapes all newlines.
     */
    public static String xmlEscape(CharSequence s) {
        return xmlEscape(s,true);
    }

    /**
     * {@literal Escapes <>"'&, replaces newlines with <br>, and tabs with a series of four &nbsp;.}
     */
    public static final String htmlEscapeWhitespace(String str) {
        return htmlEscapeWhitespace(str,false);
    }

    /**
     * {@literal Escapes <>"'&, replaces newlines with <br>, spaces with &nbsp;, and tabs with a series of four &nbsp;.}
     */
    public static final String htmlEscapeWhitespaceNonBreakingSpaces(String str) {
        return htmlEscapeWhitespace(str,true);
    }

    private static final String htmlEscapeWhitespace(String str,boolean noBreaks) {
        if(str==null) return "null";
        StringBuilder sb = new StringBuilder();
        int sz = str.length();
        for(int i=0; i<sz; i++) {
            char ch = str.charAt(i);
            if(ch=='&') sb.append("&amp;");
            else if(ch=='<') sb.append("&lt;");
            else if(ch=='>') sb.append("&gt;");
            else if(ch=='"') sb.append("&quot;");
            else if(ch=='\'') sb.append("&#39;");
            else if(ch=='\n' || ch=='\r') sb.append("<br />");
            else if(ch=='\t') sb.append("&nbsp;&nbsp;&nbsp&nbsp;");
            else if(ch==' ' && noBreaks) sb.append("&nbsp;");
            else sb.append(ch);
        }
        return sb.toString();
    }

    public static String sqlEscape(String str) {
        StringBuilder sb = new StringBuilder("");
        for(int i=0;i<str.length();i++) {
            char ch = str.charAt(i);
            if(ch=='\'')
                sb.append("''");
            else if(ch=='\n')
                sb.append("\\n");
            else if(ch=='\r')
                sb.append("\\r");
            else
                sb.append(ch);
        }
        return sb.toString();
    }

    /** 
     * Encodes a URL query string component.  Spaces are replaced with %20 rather than +.
     */
    public static String encodeURLComponent(String s) {
        try {
            return URLEncoder.encode(s,"UTF-8").replace("+","%20");
        }
        catch(UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    private static BitSet safeURL = new BitSet(256);
    private static BitSet safeURLPath;
    private static BitSet safeURLComponentMinimal;
    static {
        for(char ch = 'a'; ch<='z'; ch++) {
            safeURL.set(ch);
        }
        for(char ch = 'A'; ch<='Z'; ch++) {
            safeURL.set(ch);
        }
        for(char ch = '0'; ch<='9'; ch++) {
            safeURL.set(ch);
        }
        String unreserved = "-._~";
        String pathGenDelims = ":@/";
        String safeSubDelims1 = "!$()*,";
        String safeSubDelims2 = ";=";
        String unsafeSubDelims = "+&'"; // generally makes life easier to encode in path
        String fullURIOnlyGenDelims = "?#[]";
        String s;
        s = unreserved + safeSubDelims1 + pathGenDelims;
        for(int i = 0; i < s.length(); i++) {
            safeURL.set(s.charAt(i));
        }
        safeURLComponentMinimal = (BitSet)safeURL.clone();
        s = safeSubDelims2;
        for(int i = 0; i < s.length(); i++) {
            safeURL.set(s.charAt(i));
        }
        safeURLPath = (BitSet)safeURL.clone();
        s = unsafeSubDelims + fullURIOnlyGenDelims;
        for(int i = 0; i < s.length(); i++) {
            safeURL.set(s.charAt(i));
        }
        safeURL.set('%',true);
    }

    private static String encodeURL(String s, BitSet safeChars) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes) {
            if(safeChars.get(b & 0xFF)) {
                sb.append((char)b);
            }
            else {
                sb.append("%");
                sb.append(encodeHexChar(b));
            }
        }
        return sb.toString();
    }

    /** 
     * Encodes a URL, leaving most characters alone.  It assumes all special characters present are intended to be special, including %.
     * It will only encode characters which are not legal in URLs.
     */
    public static String encodeURL(String s) {
        return encodeURL(s,safeURL);
    }

    /**
     * Encodes a URL plus replace {@literal & and ' with &amp; and &#39;}
     */
    public static String encodeURLForAttr(String s) {
        return cgiEscape(encodeURL(s));
    }

    /** 
     * Encodes a URL, leaving most characters alone, but encoding ? and #.
     * Also encodes +, {@literal &}, ' so that it automatically can be embedded as data anywhere in HTML.
     * Intended to use on the path component of a URI.
     */
    public static String encodeURLPath(String s) {
        return encodeURL(s,safeURLPath);
    }

    /** 
     * Encodes a URL, leaving most characters alone, but encoding ? and #, as well as ; = {@literal &}.
     * Also encodes +, {@literal &}, ' so that it automatically can be embedded as data anywhere in HTML.
     * Intended to use for component of a query string of a URI.
     */
    public static String encodeURLComponentMinimal(String s) {
        return encodeURL(s,safeURLComponentMinimal);
    }
}