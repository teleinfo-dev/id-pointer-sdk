/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
         http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.stream;

import java.io.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

/** Subclass of Vector that can read and write itself to a stream.
 */
public class StreamVector extends Vector<Object> implements StreamObject {

    @Override
    public boolean isStreamTable() {
        return false;
    }

    @Override
    public boolean isStreamVector() {
        return true;
    }

    public char startingDelimiter() {
        return '(';
    }

    @Override
    public void readFrom(String str) throws StringEncodingException {
        Reader in = new StringReader(str);
        try {
            readFrom(in);
        } catch (IOException e) {
            throw new StringEncodingException("IO exception: " + e.toString());
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    public Object deepClone() {
        StreamVector newVector = new StreamVector();
        for (Enumeration<?> e = elements(); e.hasMoreElements();) {
            Object item = e.nextElement();
            try {
                if (item instanceof DeepClone) {
                    item = ((DeepClone) item).deepClone();
                }
            } catch (Exception ex) {
                System.out.println("Exception cloning item in StreamVector: " + e);
            }
            newVector.addElement(item);
        }
        return newVector;
    }

    @Override
    public void readFrom(Reader str) throws StringEncodingException, IOException {
        //Read the whitespace.
        //Get first character.  If it is not a '{' throw an exception.
        //If it is, call readTheRest on it.
        int ch;
        while (true) {
            ch = StreamObjectUtil.getNonWhitespace(str);
            if (ch == '(') {
                readTheRest(str);
            } else if (ch == -1) {
                throw new StringEncodingException("Unexpected end of input in StreamVector.");
            } else {
                throw new StringEncodingException("Expected (, got '" + (char) ch + "'");
            }
        }
    }

    @Override
    public void writeTo(Writer out) throws IOException {
        writeTo(out, 0);
    }

    @Override
    public void writeTo(Writer out, int indentLevel) throws IOException {
        int numSpaces = indentLevel * 2;
        char spaces[] = new char[numSpaces];

        Arrays.fill(spaces, ' ');
        String indentation = new String(spaces);
        out.write("(\n");
        Object val;
        for (int i = 0; i < size(); i++) {
            val = elementAt(i);
            //-- Let's see if the object will respond to the encoding methods.
            //-- If so, we will tell it to encode itself, if not, we will
            //-- encode it as a String.
            if (val instanceof StreamObject) {
                ((StreamObject) val).writeTo(out, indentLevel + 1);
            } else {
                out.write(indentation);
                out.write("  ");
                StreamObjectUtil.writeEncodedString(out, String.valueOf(val));
                out.write("\n");
            }
        }
        out.write(indentation);
        out.write(")\n");
    }

    @Override
    public String writeToString() {
        StringWriter out = new StringWriter();
        try {
            writeTo(out);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return out.toString();
    }

    @Override
    public void readTheRest(Reader str) throws StringEncodingException, IOException {
        int ch;
        removeAllElements();
        while (true) {
            // Let's read the value.

            Object obj;
            ch = StreamObjectUtil.getNonWhitespace(str);
            switch (ch) {
            case '"':
                obj = StreamObjectUtil.readString(str);
                break;
            case '{':
                StreamTable valTable = new StreamTable();
                valTable.readTheRest(str);
                obj = valTable;
                break;
            case '(':
                StreamVector vector = new StreamVector();
                vector.readTheRest(str);
                obj = vector;
                break;
            case ')':
                return;
            case -1:
                throw new StringEncodingException("Unexpected end of input " + "while reading Vector.");
            default:
                obj = StreamObjectUtil.readUndelimitedString(str, ch);
            }
            addElement(obj);
        }
    }
}
