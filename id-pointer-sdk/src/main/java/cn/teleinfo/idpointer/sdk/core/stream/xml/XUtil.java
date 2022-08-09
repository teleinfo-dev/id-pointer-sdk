/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
         http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.stream.xml;

public abstract class XUtil {
    public static final String encodeString(String str) {
        StringBuilder sb = new StringBuilder();
        int n = str.length();
        for (int i = 0; i < n; i++) {
            char ch = str.charAt(i);
            if (ch == '&') {
                sb.append("&amp;");
            } else if (ch == '<') {
                sb.append("&lt;");
            } else if (ch == '>') {
                sb.append("&gt;");
            } else if (ch == '"') {
                sb.append("&quot;");
            } else if (ch == '\'') {
                sb.append("&#39;");
            } else if (ch >= 255) { // encode all non-ascii characters
                sb.append("&#");
                sb.append((int) ch);
                sb.append(';');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static final String decodeString(String str) {
        StringBuilder sb = new StringBuilder();
        int n = str.length();
        for (int i = 0; i < n; i++) {
            char ch = str.charAt(i);
            if (ch == '&') {
                String entity = readEntity(str, i);
                i += entity.length() - 1;
                sb.append(decodeEntity(entity));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String decodeEntity(String entity) {
        String upper = entity.toUpperCase();
        if (upper.equals("&AMP;")) {
            return "&";
        } else if (upper.equals("&LT;")) {
            return "<";
        } else if (upper.equals("&GT;")) {
            return ">";
        } else if (upper.equals("&QUOT;")) {
            return "\"";
        } else if (upper.equals("&APOS;")) {
            return "\'";
        } else if (upper.startsWith("&#X")) {
            try {
                return String.valueOf(Character.toChars(Integer.parseInt(entity.substring(3, entity.length() - 1), 16)));
            } catch (Exception e) {
                //          System.err.println("Error: invalid character encoding: "+entity.substring(2));
            }
        } else if (entity.startsWith("&#")) {
            try {
                return String.valueOf(Character.toChars(Integer.parseInt(entity.substring(2, entity.length() - 1))));
            } catch (Exception e) {
                //        System.err.println("Error: invalid character encoding: "+entity.substring(2));
            }
        }
        //    System.err.println("Error: unidentified entity: "+entity);
        return entity;

    }

    private static String readEntity(String str, int index) {
        int eIndex = index + 1;
        while (eIndex < str.length() && str.charAt(eIndex) != ';')
            eIndex++;
        eIndex = Math.min(str.length() - 1, eIndex);
        return str.substring(index, eIndex + 1);
    }
}
