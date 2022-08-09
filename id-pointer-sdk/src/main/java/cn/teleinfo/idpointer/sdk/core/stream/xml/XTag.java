/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
         http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.stream.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** This represents an SGML/XML tag.  This can either have a set of
 * sub-tags, or have a single value. */
public class XTag {
    private final String name;
    private Object value;
    private ArrayList<XTag> subTags = null;
    private HashMap<String, String> attributes = null;
    private boolean suppressNewLine = false;

    public XTag(String name, Object value) {
        this.name = name.trim();
        if (value != null && value instanceof String) {
            this.value = ((String) value).trim();
        } else {
            this.value = value;
        }
    }

    public XTag(String name) {
        this.name = name;
        this.value = null;
    }

    public XTag shallowCloneTag() {
        XTag cloneTag = new XTag(name, value);
        for (String attributeKey : attributes.keySet()) {
            cloneTag.setAttribute(attributeKey, attributes.get(attributeKey));
        }
        return cloneTag;
    }

    public boolean isContainer() {
        return value == null;
    }

    public void addSubTag(XTag subTagValue) {
        this.value = null;
        if (subTags == null) subTags = new ArrayList<>();
        subTags.add(subTagValue);
    }

    public int getSubTagCount() {
        if (subTags == null) return 0;
        return subTags.size();
    }

    public XTag getSubTag(int i) {
        if (subTags == null) return null;
        if (i < 0 || i >= subTags.size()) return null;
        return subTags.get(i);
    }

    public XTag getSubTag(String tagName) {
        if (subTags == null) return null;
        for (int i = 0; i < subTags.size(); i++) {
            XTag subtag = subTags.get(i);
            if (subtag.getName().equals(tagName)) return subtag;
        }
        return null;
    }

    public XTag removeSubTag(int index) {
        if (index < 0 || index >= subTags.size()) return null;
        XTag rmTag = getSubTag(index);
        subTags.remove(index);
        return rmTag;
    }

    public List<XTag> getSubTags() {
        return subTags;
    }

    /** Remove the given subtag from the list of subtags.  If the given tag was
     * in the list of subtags, return true.  Otherwise this returns false.  */
    public boolean removeSubTag(XTag subtag) {
        if (subtag == null) return false;
        return subTags.remove(subtag);
    }

    public boolean hasSubTag(String tagName) {
        if (subTags == null) return false;
        for (XTag subtag : subTags) {
            if (subtag.getName().equals(tagName)) return true;
        }
        return false;
    }

    public void setSuppressNewLine(boolean suppressNewLine) {
        this.suppressNewLine = suppressNewLine;
    }

    public String getName() {
        return name;
    }

    //  public void setValue(Object value) {
    //    if(value!=null && value instanceof String) {
    //      value = ((String)value).trim();
    //    }
    //    this.value = value;
    //  }

    public Object getValue() {
        return this.value;
    }

    public String getStrValue() {
        return String.valueOf(getValue());
    }

    public int getIntValue() throws Exception {
        return Integer.parseInt(getStrValue().trim());
    }

    public long getLongValue() throws Exception {
        return Long.parseLong(getStrValue().trim());
    }

    public double getDoubleValue() throws Exception {
        return Double.parseDouble(getStrValue().trim());
    }

    public java.net.URL getUrlValue() throws Exception {
        return new java.net.URL(getStrValue());
    }

    public boolean getBoolValue() {
        return getStrValue().toUpperCase().trim().startsWith("Y");
    }

    public boolean getBoolSubTag(String tagName, boolean defaultVal) {
        XTag subtag = getSubTag(tagName);
        if (subtag == null) return defaultVal;
        try {
            return subtag.getBoolValue();
        } catch (Exception e) {
        }
        return defaultVal;
    }

    public String getStrSubTag(String tagName, String defaultVal) {
        XTag subtag = getSubTag(tagName);
        if (subtag == null) return defaultVal;
        return subtag.getStrValue();
    }

    public int getIntSubTag(String tagName, int defaultVal) {
        XTag subtag = getSubTag(tagName);
        if (subtag == null) return defaultVal;
        try {
            return subtag.getIntValue();
        } catch (Exception e) {
        }
        return defaultVal;
    }

    public long getLongSubTag(String tagName, long defaultVal) {
        XTag subtag = getSubTag(tagName);
        if (subtag == null) return defaultVal;
        try {
            return subtag.getLongValue();
        } catch (Exception e) {
        }
        return defaultVal;
    }

    public double getDoubleSubTag(String tagName, double defaultVal) {
        XTag subtag = getSubTag(tagName);
        if (subtag == null) return defaultVal;
        try {
            return subtag.getDoubleValue();
        } catch (Exception e) {
        }
        return defaultVal;
    }

    public java.net.URL getUrlSubTag(String tagName, java.net.URL defaultVal) {
        XTag subtag = getSubTag(tagName);
        if (subtag == null) return defaultVal;
        try {
            return subtag.getUrlValue();
        } catch (Exception e) {
        }
        return defaultVal;
    }

    public String[] getStrListSubTag(String tagName) {
        if (subTags == null) return new String[0];
        ArrayList<String> strVect = new ArrayList<>();
        for (XTag subtag : subTags) {
            if (subtag.getName().equals(tagName)) {
                strVect.add(subtag.getStrValue());
            }
        }
        return strVect.toArray(new String[strVect.size()]);
    }

    public void setValue(String value) {
        this.value = value;
    }

    void setAttributes(HashMap<String, String> newAttributes) {
        if (newAttributes == null || newAttributes.size() <= 0) {
            attributes = null;
        } else {
            if (attributes == null) attributes = new HashMap<>();
            else attributes.clear();
            attributes.putAll(newAttributes);
        }
    }

    public void setAttribute(String name, String val) {
        if (attributes == null) attributes = new HashMap<>();
        attributes.put(name, val);
    }

    public void setAttribute(String name, boolean val) {
        if (attributes == null) attributes = new HashMap<>();
        attributes.put(name, val ? "yes" : "no");
    }

    public void setAttribute(String name, int val) {
        if (attributes == null) attributes = new HashMap<>();
        attributes.put(name, String.valueOf(val));
    }

    public String getAttribute(String attrName, String defaultVal) {
        String val = getAttribute(attrName);
        if (val == null) return defaultVal;
        return val;
    }

    public String getAttribute(String attrName) {
        if (attributes == null) return null;
        return attributes.get(attrName);
    }

    public boolean getBoolAttribute(String attrName, boolean defaultVal) {
        String val = getAttribute(attrName);
        if (val == null) return defaultVal;
        val = val.toLowerCase();
        if (val.equals("yes") || val.equals("true") || val.equals("1")) {
            return true;
        } else if (val.equals("no") || val.equals("false") || val.equals("0")) {
            return false;
        }
        return defaultVal;
    }

    public String getStrAttribute(String tagName, String defaultVal) {
        return getAttribute(tagName, defaultVal);
    }

    public int getIntAttribute(String attrName, int defaultVal) {
        String val = getAttribute(attrName);
        if (val == null) return defaultVal;
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
        }
        return defaultVal;
    }

    public long getLongAttribute(String attrName, long defaultVal) {
        String val = getAttribute(attrName);
        if (val == null) return defaultVal;
        try {
            return Long.parseLong(val.trim());
        } catch (Exception e) {
        }
        return defaultVal;
    }

    public double getDoubleAttribute(String attrName, double defaultVal) {
        String val = getAttribute(attrName);
        if (val == null) return defaultVal;
        try {
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
        }
        return defaultVal;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void write(java.io.OutputStream out) throws java.io.IOException {
        java.io.OutputStreamWriter w = new java.io.OutputStreamWriter(out, "UTF8");
        write(w);
        w.flush();
    }

    public void write(java.io.Writer out) throws java.io.IOException {
        write(out, true, "");
    }

    public void write(java.io.Writer out, boolean pretty, String prefix) throws java.io.IOException {
        String valueStr = this.value == null ? null : String.valueOf(this.value);

        boolean emptyValue = getSubTagCount() <= 0 && (valueStr == null || valueStr.length() <= 0);

        out.write("<");
        String tagName = XUtil.encodeString(name);
        out.write(tagName);

        // if there are attributes, write them out...
        HashMap<String, String> atts = attributes;
        if (atts != null) {
            for (String key : atts.keySet()) {
                String val = atts.get(key);
                out.write(" ");
                out.write(XUtil.encodeString(key));
                out.write("=\"");
                out.write(XUtil.encodeString(val));
                out.write("\"");
            }
        }

        if (emptyValue) out.write("/");
        out.write(">");

        if (!emptyValue && getSubTagCount() <= 0) {
            // this tag has a value and no sub tags
            out.write(XUtil.encodeString(valueStr));
        } else if (getSubTagCount() > 0) {
            // this tag is a container
            if (pretty) out.write("\n");
            for (int i = 0; i < subTags.size(); i++) {
                subTags.get(i).write(out, pretty, (pretty ? (prefix + "  ") : ""));
            }
        }

        if (!emptyValue) {
            out.write("</");
            out.write(tagName);
            out.write(">");
        }
        if (pretty && !suppressNewLine) out.write("\n");
    }

    @Override
    public String toString() {
        java.io.StringWriter w = new java.io.StringWriter();
        try {
            write(w, true, "");
        } catch (Exception e) {
            System.err.println("Error encoding tags: " + e);
            e.printStackTrace(System.err);
        }
        return w.toString();
    }

}
