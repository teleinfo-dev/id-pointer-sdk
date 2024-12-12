/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import cn.teleinfo.idpointer.sdk.core.stream.xml.XParser;
import cn.teleinfo.idpointer.sdk.core.stream.xml.XTag;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TemplateBuilder {
    public static final String TEMPLATE_TAG = "template";
    public static final String TEMPLATE_DELIMITER_ATT = "delimiter";
    public static final String REF_ATT = "ref";

    public static final int RECURSION_LIMIT = 10;

    private static XParser xmlParser = new XParser();

    HandleValue[] origvals;
    String handle;
    String base;
    String extension;
    boolean caseSensitive;
    HandleResolver resolver;
    short recursionCount;

    public TemplateBuilder(HandleValue[] origvals, String handle, String base, String extension, boolean caseSensitive, HandleResolver resolver, short recursionCount) {
        this.origvals = origvals;
        this.handle = handle;
        this.base = base;
        this.extension = extension;
        this.caseSensitive = caseSensitive;
        this.resolver = resolver;
        this.recursionCount = recursionCount;
    }

    private NamespaceInfo parentNamespace;
    private XTag xmlInfo = null;

    public void setXml(XTag tag) {
        this.xmlInfo = tag;
    }

    public void setParentNamespace(NamespaceInfo parentNamespace) {
        this.parentNamespace = parentNamespace;
    }

    /** adds values to parameter resvals, or returns true if a not found result is called for */
    private boolean templateConstructViaSubtags(HandleValue origval, List<HandleValue> resvals, XTag tag, TemplateReplacementMap map) {
        String valueKind = "extension";
        String parameter = "extension";
        String test = null;
        boolean negate = false;
        String expression = null;

        int count = tag.getSubTagCount();
        for (int i = 0; i < count; i++) {
            XTag subtag = tag.getSubTag(i);
            if (subtag.getName().equals("value")) {
                int index = subtag.getIntAttribute("index", -1);
                String type = subtag.getAttribute("type", null);
                String data = subtag.getAttribute("data", null);
                if (data == null) {
                    String xmlValueStr = subtag.getValue() == null ? null : String.valueOf(subtag.getValue());
                    if (xmlValueStr != null && xmlValueStr.length() > 0) {
                        data = xmlValueStr;
                    } else if (subtag.getSubTagCount() > 0) {
                        try {
                            Writer out = new StringWriter();
                            for (int j = 0; j < subtag.getSubTagCount(); j++) {
                                subtag.getSubTag(j).write(out);
                            }
                            data = out.toString();
                        } catch (IOException e) {
                        }
                    }
                }
                if (origval == null && (type == null || data == null)) continue;
                Collections.sort(resvals, handleValueIndexComparator);
                HandleValue resval;
                if (origval != null) {
                    resval = origval.duplicate();
                    if (index < 0) index = resval.index;
                } else {
                    resval = new HandleValue();
                    index = 1;
                }
                for (int j = 0; j < resvals.size(); j++) {
                    if (index == resvals.get(j).index) index++;
                }
                try {
                    resval.setIndex(index);
                    if (type != null) resval.setType(map.performReplace(type).getBytes("UTF-8"));
                    if (data != null) resval.setData(map.performReplace(data).getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                }
                resvals.add(resval);
            } else if (subtag.getName().equals("if") || subtag.getName().equals("else")) {
                if (subtag.getName().equals("if")) {
                    valueKind = subtag.getAttribute("value", "extension");
                    parameter = subtag.getAttribute("parameter", valueKind);
                    test = subtag.getAttribute("test");
                    negate = subtag.getAttribute("negate", "").equals("true");
                    expression = subtag.getAttribute("expression");
                } else {
                    negate = !negate;
                }
                if (test == null && expression == null && !negate) {
                    boolean notfound = templateConstructViaSubtags(origval, resvals, subtag, map);
                    if (notfound) return true;
                    continue;
                }
                if (expression == null) continue;
                String value = map.get(valueKind);

                if (test == null || test.equals("equals")) {
                    if (negate != value.equals(expression)) {
                        boolean notfound = templateConstructViaSubtags(origval, resvals, subtag, map.put(parameter, value));
                        if (notfound) return true;
                        continue;
                    } else continue;
                } else if (test.equals("matches")) {
                    Pattern cre = Pattern.compile(expression, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                    Matcher m = cre.matcher(value);
                    if (negate != m.matches()) {
                        TemplateReplacementMap newMap;
                        if (negate) newMap = map.put(parameter, value);
                        else newMap = map.put(parameter, m);
                        boolean notfound = templateConstructViaSubtags(origval, resvals, subtag, newMap);
                        if (notfound) return true;
                        continue;
                    } else continue;
                }
            } else if (subtag.getName().equals("notfound")) {
                return true;
            } else if (subtag.getName().equals("foreach")) {
                if (origval != null) continue;
                for (HandleValue val : origvals) {
                    boolean notfound = templateConstructViaSubtags(val, resvals, subtag, map.put(val));
                    if (notfound) return true;
                }
            } else if (subtag.getName().equals("def")) {
                String param = subtag.getAttribute("parameter");
                if (param != null) {
                    List<HandleValue> defvals = new ArrayList<>();
                    boolean notfound = templateConstructViaSubtags(origval, defvals, subtag, map);
                    if (notfound) return true;
                    if (defvals.size() > 0) {
                        map = map.put(param, defvals.get(0).getDataAsString());
                    }
                }
            }
        }
        return false;
    }

    private static final int NO_TEMPLATE = 0;
    private static final int NOT_FOUND = 1;
    private static final int HAS_TEMPLATE = 2;

    public HandleValue[] templateConstruct() {
        TemplateReplacementMap map = new TemplateReplacementMap(handle, base, extension);
        List<HandleValue> resvals = new ArrayList<>();
        int res = NO_TEMPLATE;
        while (res == NO_TEMPLATE) {
            res = templateConstruct(xmlInfo, resvals, map);
            if (res == NO_TEMPLATE) {
                if (parentNamespace == null) return null;
                parentNamespace.setupTemplateBuilderForNamespace(this);
            }
        }
        if (res == NOT_FOUND) return null;
        return resvals.toArray(new HandleValue[0]);
    }

    private int templateConstruct(XTag tag, List<HandleValue> resvals, TemplateReplacementMap map) {
        if (tag == null) return NO_TEMPLATE;
        if (tag.getName().equals(TEMPLATE_TAG)) {
            String ref = tag.getAttribute(REF_ATT);
            if (ref != null) {
                if (recursionCount >= RECURSION_LIMIT) {
                    // TODO: what should this behavior be?
                    return NOT_FOUND;
                }

                int colon = ref.indexOf(':');
                if (colon < 0) return NO_TEMPLATE;
                int index;
                try {
                    index = Integer.parseInt(ref.substring(0, colon));
                } catch (NumberFormatException e) {
                    return NO_TEMPLATE;
                }
                @SuppressWarnings("hiding")
                String handle = ref.substring(colon + 1, ref.length());
                try {
                    ResolutionIdRequest req = new ResolutionIdRequest(Util.encodeString(handle), null, new int[] { index }, null);
                    recursionCount++;
                    req.recursionCount = recursionCount;
                    AbstractIdResponse response = resolver.processRequest(req);
                    if (response.responseCode == AbstractMessage.RC_HANDLE_NOT_FOUND || response instanceof ErrorIdResponse) {
                        // TODO: what should this behavior be?
                        return NOT_FOUND;
                    }
                    HandleValue values[] = ((ResolutionIdResponse) response).getHandleValues();
                    if (values == null || values.length == 0) return NOT_FOUND;
                    for (HandleValue value : values) {
                        if (value.index == index) {
                            try {
                                XTag citedTag = xmlParser.parse(new InputStreamReader(new ByteArrayInputStream(value.data), "UTF-8"), false);
                                return templateConstruct(citedTag, resvals, map);
                            } catch (Exception e) {
                                // TODO: what should this behavior be?
                                return NO_TEMPLATE;
                            }
                        }
                    }
                    return NOT_FOUND;
                } catch (HandleException e) {
                    e.printStackTrace();
                    return NOT_FOUND;
                }
            } else {
                boolean notfound = templateConstructViaSubtags(null, resvals, tag, map);
                if (notfound) return NOT_FOUND;
                return HAS_TEMPLATE;
            }
        } else {
            int count = tag.getSubTagCount();
            boolean hasTemplate = false;
            for (int i = 0; i < count; i++) {
                XTag templateTag = tag.getSubTag(i);
                int res = NO_TEMPLATE;
                if (templateTag.getName().equals(TEMPLATE_TAG)) res = templateConstruct(templateTag, resvals, map);
                if (res == NOT_FOUND) return NOT_FOUND;
                if (res == HAS_TEMPLATE) hasTemplate = true;
            }
            if (hasTemplate) return HAS_TEMPLATE;
            return NO_TEMPLATE;
        }
    }

    private class TemplateReplacementMap {
        private final Map<String,String> stringMap;
        private final Map<String,Matcher> matcherMap;
        private String defaultKey = "extension";

        TemplateReplacementMap(TemplateReplacementMap map) {
            stringMap = new HashMap<>(map.stringMap);
            matcherMap = new HashMap<>(map.matcherMap);
            defaultKey = map.defaultKey;
        }

        TemplateReplacementMap(String handle, String base, String extension) {
            stringMap = new HashMap<>();
            matcherMap = new HashMap<>();

            stringMap.put("handle", handle);
            stringMap.put("base", base);
            stringMap.put("extension", extension);
        }

        TemplateReplacementMap put(String key, String val) {
            TemplateReplacementMap res = new TemplateReplacementMap(this);
            res.stringMap.put(key, val);
            res.matcherMap.remove(key);
            //          res.defaultKey = key;
            return res;
        }

        TemplateReplacementMap put(String key, Matcher val) {
            TemplateReplacementMap res = new TemplateReplacementMap(this);
            res.stringMap.put(key, val.group());
            res.matcherMap.put(key, val);
            //          res.defaultKey = key;
            return res;
        }

        TemplateReplacementMap put(HandleValue val) {
            TemplateReplacementMap res = new TemplateReplacementMap(this);
            res.stringMap.put("type", val.getTypeAsString());
            res.stringMap.put("data", val.getDataAsString());
            res.stringMap.put("index", String.valueOf(val.getIndex()));
            return res;
        }

        String get(String key, int group) {
            Matcher matcher = matcherMap.get(key);
            if (matcher != null) {
                if (group >= 0 && group <= matcher.groupCount()) {
                    return nz(matcher.group(group));
                } else return "";
            } else {
                String res = stringMap.get(key);
                if (res != null) {
                    if (group == 0) return res;
                    else return "";
                }
            }
            return "";
        }

        private String nz(String a) {
            if (a == null) return "";
            return a;
        }

        String get(String key) {
            if (key.equals("")) return nz(stringMap.get(key));
            char ch = key.charAt(0);
            if (ch >= '0' && ch <= '9') {
                try {
                    int group = Integer.parseInt(key);
                    return nz(get(defaultKey, group));
                } catch (NumberFormatException e) {
                }
            }

            if (key.charAt(key.length() - 1) == ']') {
                int bracket = key.lastIndexOf('[');
                if (bracket >= 0) {
                    try {
                        int group = Integer.parseInt(key.substring(bracket + 1, key.length() - 1));
                        key = key.substring(0, bracket);
                        return nz(get(key, group));
                    } catch (NumberFormatException e) {
                    }
                }
            }

            return nz(stringMap.get(key));
        }

        String performReplace(String a) {
            int strLen = a.length();
            StringBuffer buf = new StringBuffer(strLen);
            for (int i = 0; i < strLen; i++) {
                char ch = a.charAt(i);
                if (ch != '\\' && ch != '$') buf.append(ch);
                else if (ch == '\\') {
                    i++;
                    if (i == strLen) {
                        buf.append('\\');
                        break;
                    } else {
                        ch = a.charAt(i);
                        buf.append(ch);
                    }
                } else if (ch == '$') {
                    i++;
                    if (i == strLen) {
                        buf.append('$');
                        break;
                    }
                    ch = a.charAt(i);
                    if (ch == '{') {
                        int j = a.indexOf('}', i);
                        if (j < 0) {
                            buf.append("${");
                        } else {
                            buf.append(get(a.substring(i + 1, j)));
                            i = j;
                        }
                    } else if (ch >= '0' && ch <= '9') {
                        int num = ch - '0';
                        for (i++; i < strLen; i++) {
                            ch = a.charAt(i);
                            if (ch >= '0' && ch <= '9') {
                                num = num * 10 + (ch - '0');
                            } else {
                                i--;
                                break;
                            }
                        }
                        buf.append(get(defaultKey, num));
                    } else {
                        buf.append('$');
                        buf.append(ch);
                    }
                }
            }
            return buf.toString();
        }
    }

    private static final Comparator<HandleValue> handleValueIndexComparator = (o1, o2) -> Integer.compare(o1.index, o2.index);

}
