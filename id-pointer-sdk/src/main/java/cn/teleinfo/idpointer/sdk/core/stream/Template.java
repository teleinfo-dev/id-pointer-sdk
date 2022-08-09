/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
         http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.stream;

import cn.teleinfo.idpointer.sdk.core.stream.util.StringUtils;

import java.io.*;
import java.util.List;
import java.util.Map;

/** Class used to merge complex data with a web page or other text document.
 * Can perform recursive iteration over specially formatted test.
 */
public class Template {

    /** This function returns the index of the next matching closed brace "}}".
     * It assumes that the start brace is *not* a part of the string.
     * getMatchingBraceLocation("Hello{{xxx}} }}") will return 13.
     */
    public static int getMatchingBraceLocation(String str) {
        int count = 0;
        int leftIdx = -1, rightIdx = -1;
        int totIdx = 0;
        String str2 = str;
        while (true) {
            leftIdx = str2.indexOf("{{");
            rightIdx = str2.indexOf("}}");
            if (rightIdx < 0) {
                return -1;
            }
            if (leftIdx >= 0 && leftIdx < rightIdx) {
                // A left bracket came first - {{
                count++;
                str2 = str2.substring(leftIdx + 2, str2.length());
                totIdx += leftIdx + 2;
                continue;
            } else {
                // We found a right bracket - }}
                str2 = str2.substring(rightIdx + 2, str2.length());
                totIdx += rightIdx + 2;
                if (count == 0) {
                    return totIdx - 2;
                } else {
                    count--;
                }
            }
        }
    }

    private static int getMatchingBraceLocation(StringBuilder s, int index) {
        int count = 0;
        int curr = index;
        while (true) {
            int leftIdx = s.indexOf("{{", curr);
            int rightIdx = s.indexOf("}}", curr);
            if (rightIdx < 0) {
                return -1;
            } else if (leftIdx < 0 || leftIdx > rightIdx) {
                if (count == 0) {
                    return rightIdx;
                } else {
                    count--;
                    curr = rightIdx + 2;
                }
            } else {
                count++;
                curr = leftIdx + 2;
            }
        }
    }

    /** Merge the hashtable data with the text from the specified file */
    public static String subDictIntoFile(String filename, Map<?, ?> dict) throws IOException, TemplateException {
        return subDictIntoFile(new File(filename), dict);
    }

    /** Merge the hashtable data with the text from the specified file */
    public static String subDictIntoFile(File file, Map<?, ?> dict) throws IOException, TemplateException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        char ch[] = new char[256];
        int n = 0;
        while ((n = in.read(ch, 0, ch.length)) >= 0) {
            sb.append(ch, 0, n);
        }
        try {
            in.close();
        } catch (Exception e) {
        }
        return subDictIntoString(sb.toString(), dict);
    }

    public static String subDictIntoStream(InputStream in, Map<?, ?> dict) throws IOException, TemplateException {
        try {
            InputStreamReader rdr = new InputStreamReader(in, "UTF8");
            StringWriter sw = new StringWriter();
            char ch[] = new char[1024];
            int r;
            while ((r = rdr.read(ch, 0, ch.length)) >= 0)
                sw.write(ch, 0, r);
            return subDictIntoString(sw.toString(), dict);
        } finally {
            if (in != null) in.close();
        }
    }

    /** Merge the hashtable data with the specified text */
    public static String subDictIntoString(String str, Map<?, ?> dict) throws TemplateException {
        StringBuilder sb = new StringBuilder(str);
        int index = 0;
        while (true) { // re-merge the data and the template until there are no more tags
            int begTag = sb.indexOf("{{", index);
            if (begTag < 0) break;
            int endTag = getMatchingBraceLocation(sb, begTag + 2);
            if (endTag < 0) {
                throw new TemplateException("Unmatched {{.");
            }
            int doubleColonIdx = sb.indexOf("::", begTag + 2);
            if (doubleColonIdx >= endTag) doubleColonIdx = -1;
            String key;
            if (doubleColonIdx >= 0) {
                key = sb.substring(begTag + 2, doubleColonIdx);
            } else {
                key = sb.substring(begTag + 2, endTag);
            }

            Object obj = dict.get(key);
            if (doubleColonIdx < 0) {
                String replacement = obj == null ? "" : String.valueOf(obj);
                sb.replace(begTag, endTag + 2, replacement);
                index = begTag + replacement.length();
            } else {
                char ch = sb.charAt(doubleColonIdx + 2);
                if (ch == '?') {
                    index = conditionalInclude(sb, begTag, doubleColonIdx, endTag, obj, false);
                } else if (ch == '!') {
                    index = conditionalInclude(sb, begTag, doubleColonIdx, endTag, obj, true);
                } else if (obj == null) {
                    sb.delete(begTag, endTag + 2);
                    index = begTag;
                } else if (ch == '*') {
                    String extra = sb.substring(doubleColonIdx + 3, endTag);
                    sb.delete(begTag, endTag + 2);
                    index = begTag;
                    if (obj instanceof List) {
                        List<?> list = (List<?>) obj;
                        for (Object subObj : list) {
                            String toInsert;
                            if (subObj instanceof Map) {
                                toInsert = subDictIntoString(extra, (Map<?, ?>) subObj);
                            } else {
                                toInsert = String.valueOf(subObj);
                            }
                            sb.insert(index, toInsert);
                            index += toInsert.length();
                        }
                    } else if (obj instanceof Map) {
                        String toInsert = subDictIntoString(extra, (Map<?, ?>) obj);
                        sb.insert(index, toInsert);
                        index += toInsert.length();
                    } else {
                        String toInsert = subDictIntoString(extra, dict);
                        sb.insert(index, toInsert);
                        index += toInsert.length();
                    }
                } else {
                    String extra = sb.substring(doubleColonIdx + 2, endTag);
                    String replacement;
                    if (extra.equalsIgnoreCase("URLEncodeComponent")) {
                        replacement = StringUtils.encodeURLComponent(String.valueOf(obj));
                    } else if (extra.equalsIgnoreCase("URLEncodePath")) {
                        replacement = StringUtils.encodeURLPath(String.valueOf(obj));
                    } else if (extra.equalsIgnoreCase("CGIEscape")) {
                        replacement = StringUtils.cgiEscape(String.valueOf(obj));
                    } else if (extra.equalsIgnoreCase("SQLString")) {
                        replacement = StringUtils.sqlEscape(String.valueOf(obj));
                    } else {
                        replacement = String.valueOf(obj);
                    }
                    sb.replace(begTag, endTag + 2, replacement);
                    index = begTag + replacement.length();
                }
            }
        }
        return sb.toString();
    }

    /** Returns the contents of a tag only if the first part of the tag matches
     * (or doesn't match if the notEqual parameter is true) the given value.
     * Given the tag:  {{volunteer_coord::?Yes:CHECKED}} this method should be
     * used to handle the "Yes:CHECKED" part.  The expected format of
     * tagContents is:  <conditionalvalue>:<result>.
     * If the <conditionalvalue> matches (or doesn't, if notEquals is set) the
     * given value in a case-sensitive String comparison then the <result> will
     * be returned.
     */
    private static int conditionalInclude(StringBuilder sb, int begTag, int doubleColonIdx, int endTag, Object value, boolean notEqual) {
        int firstColon = sb.indexOf(":", doubleColonIdx + 3);
        if (firstColon >= endTag) firstColon = -1;
        if (firstColon < 0) {
            sb.delete(begTag, endTag + 2);
            return begTag;
        }
        boolean matches = false;
        if (notEqual) {
            matches = value == null || !sb.substring(doubleColonIdx + 3, firstColon).equals(String.valueOf(value));
        } else {
            matches = value != null && sb.substring(doubleColonIdx + 3, firstColon).equals(String.valueOf(value));
        }
        if (matches) {
            sb.delete(endTag, endTag + 2);
            sb.delete(begTag, firstColon + 1);
            return begTag;
        } else {
            sb.delete(begTag, endTag + 2);
            return begTag;
        }
    }
}
