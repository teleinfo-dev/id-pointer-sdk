/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
         http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.stream;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/** Utility functions used to ease the parsing and encoding of StreamObjects
 */
public abstract class StreamObjectUtil {

    /**************************************************************
     * Read from the specified reader until a non-whitespace
     * character is read.  When a non-whitespace character is read,
     * return it.
     **************************************************************/
    public static int getNonWhitespace(Reader in) throws IOException {
        int ch;
        while (true) {
            ch = in.read();
            if (ch == ' ' || ch == '\n' || ch == '\r' || ch == ',' || ch == ';' || ch == '\t') {
                continue;
            }
            return ch;
        }
    }

    /**************************************************************
     * This function reads in a string given that the string is
     * not delimited with a quote.  It will read in anything up to
     * but not including anything that might delimit a word.
     **************************************************************/
    public static String readUndelimitedString(Reader in, int firstChar) throws IOException {
        if (firstChar == -1) {
            return "";
        }
        StringBuffer sb = new StringBuffer("" + (char) firstChar);
        int ch;
        while (true) {
            ch = in.read();
            if (ch == ' ' || ch == 10 || ch == 13 || ch == ',' || ch == ';' || ch == -1 || ch == '=' || ch == '\t') {
                return sb.toString();
            } else {
                sb.append((char) ch);
            }
        }
    }

    /***********************************************************************
     * This function reads in a string token assuming the first qoute (")
     * has been read already.
     ***********************************************************************/
    public static String readString(Reader in) throws StringEncodingException, IOException {
        StringBuffer results = new StringBuffer("");
        int ch;
        while (true) {

            ch = in.read();
            if (ch == -1) {
                throw new StringEncodingException("Unexpected End of String");
            } else if (ch == '\\') {
                ch = in.read();
                if (ch == -1) {
                    throw new StringEncodingException("Unexpected End of String");
                } else if (ch == 'n') {
                    results.append('\n');
                } else {
                    results.append((char) ch);
                }
            } else if (ch == '"') {
                return results.toString();
            } else {
                results.append((char) ch);
            }
        }
    }

    public static void writeEncodedString(Writer out, String str) throws IOException {
        int n = str.length();
        char ch;
        out.write('"');
        for (int i = 0; i < n; i++) {
            ch = str.charAt(i);
            if (ch == '"' || ch == '\\') out.write('\\');
            out.write(ch);
        }
        out.write("\"");
    }

}
