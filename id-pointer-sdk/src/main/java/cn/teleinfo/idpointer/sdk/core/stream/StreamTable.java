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
import java.util.Hashtable;

/** Subclass of Hashtable that can read and write itself to a stream.
 */
public class StreamTable extends Hashtable<String, Object> implements StreamObject, DeepClone {

    @Override
    public boolean isStreamTable() {
        return true;
    }

    @Override
    public boolean isStreamVector() {
        return false;
    }

    public char startingDelimiter() {
        return '{';
    }

    /************************************************************
     * copy a (reference to) all values in this table to the
     * specified table.
     ************************************************************/
    public void merge(Hashtable<String,?> ht) {
        for (Enumeration<String> e = ht.keys(); e.hasMoreElements();) {
            String key = e.nextElement();
            put(key, ht.get(key));
        }
    }

    @Override
    public Object deepClone() {
        StreamTable newTable = new StreamTable();
        for (Enumeration<String> e = keys(); e.hasMoreElements();) {
            String key = e.nextElement();
            Object value = get(key);
            try {
                if (value instanceof DeepClone) {
                    value = ((DeepClone) value).deepClone();
                }
            } catch (Exception ex) {
                System.out.println("Exception cloning value in StreamTable: " + ex);
            }
            newTable.put(key, value);
        }
        return newTable;
    }

    public Object get(Object key, Object defaultVal) {
        Object o = super.get(key);
        if (o == null) return defaultVal;
        return o;
    }

    public String getStr(Object key, String defaultVal) {
        Object o = super.get(key);
        if (o == null) return defaultVal;
        return String.valueOf(o);
    }

    public String getStr(Object key) {
        return getStr(key, null);
    }

    public boolean getBoolean(Object key, boolean defaultVal) {
        String val = getStr(key, defaultVal ? "yes" : "no").toLowerCase();
        if (val.startsWith("y") || val.startsWith("t")) {
            return true;
        } else if (val.startsWith("n") || val.startsWith("f")) {
            return false;
        } else {
            return defaultVal;
        }
    }

    public boolean getBoolean(Object key) {
        return getBoolean(key, false);
    }

    public long getLong(Object key, long defaultVal) {
        Object val = get(key);
        if (val == null) return defaultVal;
        try {
            return Long.parseLong(String.valueOf(val));
        } catch (Exception e) {
            System.err.println("Invalid long value: " + val);
        }
        return defaultVal;
    }

    public int getInt(Object key, int defaultVal) {
        Object val = get(key);
        if (val == null) return defaultVal;
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (Exception e) {
            System.err.println("Invalid int value: " + val);
        }
        return defaultVal;
    }

    public void readFrom(InputStream in) throws StringEncodingException, IOException {
        readFrom(new InputStreamReader(in));
    }

    @Override
    public void readFrom(String str) throws StringEncodingException {
        Reader in = new StringReader(str);
        try {
            readFrom(in);
        } catch (IOException e) {
            throw new StringEncodingException("IO exception: " + e.toString());
        }
    }

    @Override
    public void readFrom(Reader str) throws StringEncodingException, IOException {
        //Read the whitespace.
        //Get first character.  If it is not a '{' throw and exception.
        //If it is, call readTheRest on it.
        int ch = StreamObjectUtil.getNonWhitespace(str);
        if (ch == '{') {
            readTheRest(str);
        } else if (ch == -1) {
            throw new StringEncodingException("Unexpected end of input in StreamTable.");
        } else {
            throw new StringEncodingException("Expected {, got " + (char) ch);
        }
    }

    public void readFromFile(File file) throws StringEncodingException, IOException {
        AtomicFile atomicFile = new AtomicFile(file, false);
        Reader in = new InputStreamReader(atomicFile.openRead(), "UTF-8");
        readFrom(in);
        in.close();
    }

    /** Returns all of the keys to the hashtable that are java.lang.String objects. */
    public String[] getStringKeys() {
        return keySet().toArray(new String[0]);
    }

    public void readFromFile(String fileName) throws StringEncodingException, IOException {
        readFromFile(new File(fileName));
    }

    public void writeToFile(String fileName) throws IOException {
        writeToFile(fileName, true);
    }

    public void writeToFile(String fileName, boolean sync) throws IOException {
        writeToFile(new File(fileName), sync);
    }

    public void writeToFile(File file) throws IOException {
        writeToFile(file, true);
    }

    @SuppressWarnings("resource")
    public void writeToFile(File file, boolean sync) throws IOException {
        AtomicFile atomicFile = new AtomicFile(file, sync);
        FileOutputStream outStream = atomicFile.startWrite();
        try {
            Writer out = new OutputStreamWriter(outStream, "UTF-8");
            writeTo(out);
            out.flush();
            atomicFile.finishWrite(outStream);
        } catch (IOException e) {
            atomicFile.failWrite(outStream);
            throw e;
        }
    }

    @Override
    public void readTheRest(Reader str) throws StringEncodingException, IOException {
        int ch;
        clear();
        String key;
        while (true) {
            ch = StreamObjectUtil.getNonWhitespace(str);
            if (ch == '"') {
                key = StreamObjectUtil.readString(str);
            } else if (ch == '}') {
                return;
            } else if (ch == -1) {
                throw new StringEncodingException("Unexpected end of input in " + "StreamTable.");
            } else {
                key = StreamObjectUtil.readUndelimitedString(str, ch);
            }

            ch = StreamObjectUtil.getNonWhitespace(str);
            if (ch != '=') {
                throw new StringEncodingException("Expected \"=\" ");
            }

            Object obj;
            ch = StreamObjectUtil.getNonWhitespace(str);
            if (ch == '"') {
                obj = StreamObjectUtil.readString(str);
            } else if (ch == '{') {
                StreamTable valTable = new StreamTable();
                valTable.readTheRest(str);
                obj = valTable;
            } else if (ch == '(') {
                StreamVector vector = new StreamVector();
                vector.readTheRest(str);
                obj = vector;
            } else if (ch == -1) {
                throw new StringEncodingException("Unexpected end of input: " + "Expected value for key: '" + key + "'");
            } else {
                obj = StreamObjectUtil.readUndelimitedString(str, ch);
            }
            put(key, obj);
        }
    }

    public void put(String key, boolean boolVal) {
        put(key, boolVal ? "yes" : "no");
    }

    public void put(String key, int intVal) {
        put(key, String.valueOf(intVal));
    }

    public void put(String key, long longVal) {
        put(key, String.valueOf(longVal));
    }

    @Override
    public synchronized String toString() {
        return writeToString();
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
    public void writeTo(Writer out) throws IOException {
        writeTo(out, 0);
    }

    @Override
    public void writeTo(Writer out, int indentLevel) throws IOException {
        int numSpaces = indentLevel * 2;
        char spaces[] = new char[numSpaces];

        Arrays.fill(spaces, ' ');
        String indentation = new String(spaces);

        out.write("{\n");
        for (Enumeration<String> en = this.keys(); en.hasMoreElements();) {
            out.write(indentation);
            out.write("  ");
            String key = en.nextElement();
            //Output the key as a string.
            StreamObjectUtil.writeEncodedString(out, key);
            out.write(" = ");

            Object val = get(key);
            //-- Let's see if the object will respond to the encoding methods.
            //-- If so, we will tell it to encode itself, if not, we will
            //-- encode it as a String.
            if (val instanceof StreamObject) ((StreamObject) val).writeTo(out, indentLevel + 1);
            else StreamObjectUtil.writeEncodedString(out, String.valueOf(val));
            out.write("\n");
        }

        out.write(indentation);
        out.write("}\n");
    }

}
