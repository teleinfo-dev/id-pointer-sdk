/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
         http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.stream.xml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Stack;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XParser {

    private static boolean debug = false;
    private final boolean stripNewLine = true;
    private final boolean strictMode = false;

    public XParser() {
    }

    private class ParseTag {
        StringBuffer name = new StringBuffer();
        HashMap attributes = new HashMap();
        boolean isComment;
        boolean isEndTag;
        boolean isAggregateEnd;

        ParseTag() {
            resetValues();
        }

        void resetValues() {
            name.setLength(0);
            attributes.clear();
            isComment = false;
            isEndTag = false;
            isAggregateEnd = false;
        }

        @Override
        public String toString() {
            return name == null ? "null" : name.toString();
        }
    }

    private void parseTagAttributes(PushbackReader reader, ParseTag tag) throws IOException {
        HashMap attributes = tag.attributes;
        String attName = null;
        String attVal = null;
        while (true) {
            int ci = reader.read();
            if (ci < 0) throw new IOException("Unexpected end of input reading attributes");
            char ch = (char) ci;

            switch (ch) {
            case '>':
                if (attName != null) {
                    attributes.put(attName, (attVal == null) ? "NULL" : attVal);
                }
                return;
            case '=':
                if (attName == null) attName = "";
                attVal = readToken(reader);
                break;
            case '/':
                if (attName != null) {
                    attributes.put(attName, (attVal == null) ? "NULL" : attVal);
                    attName = null;
                    attVal = null;
                }
                tag.isEndTag = true;
                break;
            case '\t':
            case '\r':
            case '\n':
            case ' ':
                continue;
            case '"':
            case '\'':
                // put the last value read into the attributes, and read a string...
                if (attName != null) {
                    if (attributes == null) attributes = new HashMap();
                    attributes.put(attName, (attVal == null) ? "NULL" : attVal);
                    attVal = null;
                }
                attName = readRestOfString(ch, reader);
                break;
            default:
                if (attName != null) {
                    attributes.put(attName, (attVal == null) ? "NULL" : attVal);
                    attName = null;
                    attVal = null;
                }
                reader.unread(ch);
                attName = readToken(reader);
            }
        }
    }

    private String readToken(PushbackReader reader) throws IOException {
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ci = reader.read();
            if (ci < 0) throw new IOException("Unexpected end of input reading token: " + sb);
            char ch = (char) ci;
            if (Character.isWhitespace(ch)) if (sb.length() <= 0) continue;
            else return XUtil.decodeString(sb.toString());
            if (ch == '"' || ch == '\'') {
                // this char can either terminate a token, or begin a new string token
                if (sb.length() <= 0) {
                    return readRestOfString(ch, reader);
                } else {
                    reader.unread(ch);
                    return XUtil.decodeString(sb.toString());
                }
            } else if (ch == '=') {
                // this char can either terminate a token, or exist as a token on its own
                if (sb.length() <= 0) {
                    return String.valueOf(ch);
                } else {
                    reader.unread(ch);
                    return XUtil.decodeString(sb.toString());
                }
            } else if (ch == '>' || ch == '<') {
                // this char can either terminate a token, or exist as a token on its own
                if (sb.length() <= 0) {
                    return String.valueOf(ch);
                } else {
                    reader.unread(ch);
                    return XUtil.decodeString(sb.toString());
                }
            } else {
                sb.append(ch);
            }
        }
    }

    private String readRestOfString(char strBeginChar, PushbackReader reader) throws IOException {
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ci = reader.read();
            if (ci < 0) throw new IOException("Unexpected end of input reading string: " + sb);
            char ch = (char) ci;
            if (ch == strBeginChar) return XUtil.decodeString(sb.toString());
            else sb.append(ch);
        }
    }

    /** Read the rest of the tag, after the '<' tag has already been read. */
    private boolean readTagToken(PushbackReader reader, ParseTag tag) throws IOException {
        tag.resetValues();

        int ci;
        char ch;
        // read up to the first start-tag character..
        while ((ci = reader.read()) != '<') {
            if (ci < 0) return false;
        }
        while ((ci = reader.read()) != '>') {
            if (ci < 0) throw new IOException("Unexpected end of input reading tag");
            ch = (char) ci;
            if (ch == '/') {
                if (tag.name.length() <= 0) tag.isAggregateEnd = true;
                else tag.isEndTag = true;
            } else if (Character.isWhitespace(ch)) {
                if (tag.name.length() <= 0) // whitespace before tag name
                    continue;
                if (tag.name.toString().startsWith("!--")) {
                    // comment tag... ignore the rest of it
                    tag.name.setLength(0);
                    tag.name.append("*********");
                    tag.isComment = true;
                    String comment = "";
                    while (!comment.toString().endsWith("-->")) {
                        ci = reader.read();
                        if (ci < 0) throw new IOException("Unexpected end of input reading comment tag");
                        comment += (char) ci;
                    }
                    return true;
                } else {
                    parseTagAttributes(reader, tag);
                }
                break;
            } else {
                tag.name.append(ch);
            }
        }
        return true;
    }

    private String readTagValue(PushbackReader reader) throws IOException {
        StringBuffer sb = new StringBuffer();
        int ci;
        while ((ci = reader.read()) != '<') {
            if (ci < 0) throw new IOException("Unexpected end of input reading tag value");
            sb.append((char) ci);
        }
        reader.unread('<');
        String s = sb.toString();

        if (stripNewLine && s.endsWith("\r\n")) {
            return XUtil.decodeString(s.substring(0, s.length() - 2));
        } else if (stripNewLine && s.endsWith("\n")) {
            return XUtil.decodeString(s.substring(0, s.length() - 1));
        }
        return XUtil.decodeString(s);
    }

    private XTag parseNonRecursive(PushbackReader reader) throws IOException {
        Stack parseStack = new Stack(); // the stack of parsed tags
        Stack aggregateStack = new Stack(); // the temporary stack of sub-tags
        ParseTag tag = new ParseTag();
        while (true) {
            // read until we find a non-comment tag
            tag.resetValues();
            String tagname;

            // read the next non-comment tag
            do {
                if (!readTagToken(reader, tag)) {
                    // there are no more tags... return what we have
                    if (parseStack.size() <= 0) return null;

                    // if there are multiple tags, add all but the first as
                    // sub-tags of the first, and return that.
                    Stack tmpStack = new Stack();
                    while (parseStack.size() > 1)
                        tmpStack.push(parseStack.pop());
                    XTag xtag = (XTag) parseStack.pop();
                    while (tmpStack.size() > 0) {
                        xtag.addSubTag((XTag) tmpStack.pop());
                    }
                    return xtag;
                }
            } while (tag.isComment);

            tagname = tag.name.toString().trim();

            if (debug) System.err.println("read tag: " + tag);

            if (tag.isAggregateEnd) {
                // this is an ending aggregate tag.  Gather up all of the tags
                // that have been read since the beginning of the aggregate, and
                // put them in the beginning tag

                aggregateStack.clear();
                XTag subtag = null;
                while (true) {
                    if (parseStack.size() <= 0) {
                        // if we are in strict mode, throw an exception
                        if (strictMode) throw new IOException("Got end tag " + tagname + " with no matching begin tag");

                        // otherwise, push everything back on the stack and break,
                        // setting subtag to null to indicate that no matching parent tag was found
                        while (aggregateStack.size() > 0)
                            parseStack.push(aggregateStack.pop());
                        subtag = null;
                        break;
                    }
                    subtag = (XTag) parseStack.pop();
                    if (subtag.getName().equals(tagname) && subtag.getSubTagCount() <= 0) break;
                    aggregateStack.push(subtag);
                }

                // at this point 'subtag' is the matching beginning tag, and all
                // of the subtags are in the aggregateStack

                if (subtag == null) continue;

                // Put the sub-tags under the beginning aggregate tag
                while (aggregateStack.size() > 0) {
                    subtag.addSubTag((XTag) aggregateStack.pop());
                }

                // set the aggregate tags' values to null so that they are not
                // identified as non-aggregate tags.  This only happens if the number
                // of sub-tags are greater than zero in order to avoid rejecting the
                // value of all XML (instead of SGML) style tags.
                if (subtag.getSubTagCount() > 0) subtag.setValue(null);

                // if the beginning tag was a first-level tag, then we
                // are done parsing and can return that tag as the result.
                if (parseStack.size() <= 0) {
                    return subtag;
                }

                // and push the beginning aggregate tag back onto the stack
                parseStack.push(subtag);
            } else if (tag.isEndTag) {
                // tag is an end tag
                XTag xtag = new XTag(tagname);
                xtag.setAttributes(tag.attributes);
                parseStack.push(xtag);
            } else {
                // this is not an ending aggregate tag, just a normal value tag or
                // beginning aggregate tag (but we won't know if it is a beginning
                // aggregate tag until we read the corresponding end tag
                XTag xtag = new XTag(tagname, readTagValue(reader));
                xtag.setAttributes(tag.attributes);
                parseStack.push(xtag);
            }
        }

    }

    /**
     * @param strict This parameter is ignored.
     */
    public XTag parse(Reader reader, boolean strict) throws Exception {
        return parseNonRecursive(new PushbackReader(reader));
    }

    public static void main(String argv[]) throws Exception {
        debug = true;
        XParser parser = new XParser();
        XTag xTag = parser.parse(new InputStreamReader(System.in, "UTF8"), false);
        System.err.println("parsed: " + xTag);
    }

}
