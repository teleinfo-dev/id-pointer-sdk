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

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Object containing information about the set of handles
 * beginning with a prefix.
 */
public class NamespaceInfo {
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";

    public static final String CONTACT_TAG = "contact";
    public static final String STATUS_MSG_TAG = "statusmsg";
    public static final String STATUS_TAG = "status";
    public static final String TEMPLATE_TAG = "template";
    public static final String LOCATIONS_TAG = "locs";
    public static final String TEMPLATE_DELIMITER_ATT = "delimiter";

    private static XParser xmlParser = new XParser();

    private final XTag xmlInfo;
    private NamespaceInfo parentNamespace = null;

    public NamespaceInfo(HandleValue namespaceValue) throws HandleException {
        this(namespaceValue.getData());
    }

    public NamespaceInfo(byte rawInfo[]) throws HandleException {
        try {
            xmlInfo = xmlParser.parse(new InputStreamReader(new ByteArrayInputStream(rawInfo), "UTF8"), false);
        } catch (Exception e) {
            if (e instanceof HandleException) throw (HandleException) e;
            throw new HandleException(HandleException.INVALID_VALUE, "Error parsing namespace information: " + e);
        }
    }

    /** Construct a new namespace information record, with the default settings */
    public NamespaceInfo() {
        this.xmlInfo = new XTag("NAMESPACE");
    }

    /** Set the parent for this namespace.  This should be called when resolving
     * an identifier that is contained within multiple nested namespaces.
     */
    public void setParentNamespace(NamespaceInfo parent) {
        this.parentNamespace = parent;
    }

    /** Get the parent for this namespace.  If there is no higher
     * level namespace, then this will return null.
     */
    public NamespaceInfo getParentNamespace() {
        return this.parentNamespace;
    }

    /** Return an email address for the person or company that is responsible for
     * this namespace.
     */
    public String getResponsiblePartyContactAddress() {
        return xmlInfo.getStrSubTag(CONTACT_TAG, null);
    }

    /** Return a message that can be presented to a user who tries to resolve
     * an identifier under this namespace if the namespace status is not active.
     */
    public String getStatusMessage() {
        return xmlInfo.getStrSubTag(STATUS_MSG_TAG, null);
    }

    /** Return the status of this namespace as a String.  Currently known values
     * are "active" and "inactive" although it is possible that other values will
     * be used in the future.
     */
    public String getNamespaceStatus() {
        return xmlInfo.getStrSubTag(STATUS_TAG, STATUS_ACTIVE);
    }

    /**
     * Return the handle containing the 10320/loc values that provide a set of
     * locations for all handles under this prefix.
     */
    public List<String> getLocationTemplateHandles() {
        List<String> res = new ArrayList<>();
        boolean suppressParents = false;
        for (XTag subtag : xmlInfo.getSubTags()) {
            if (LOCATIONS_TAG.equals(subtag.getName())) {
                res.add(subtag.getStrValue());
                suppressParents = suppressParents || subtag.getBoolAttribute("no_nslocs", false);
            }
        }
        if (parentNamespace != null && !suppressParents) {
            res.addAll(parentNamespace.getLocationTemplateHandles());
        }
        if (res.isEmpty()) return null;
        return res;
    }

    /** Return whether or not handles under this prefix can be templated */
    public String templateDelimiter() {
        XTag templateTag = xmlInfo.getSubTag(TEMPLATE_TAG);
        if (templateTag != null) return templateTag.getAttribute(TEMPLATE_DELIMITER_ATT);
        if (parentNamespace != null) return parentNamespace.templateDelimiter();
        return null;
    }

    public HandleValue[] templateConstruct(HandleValue[] origvals, String handle, String base, String extension, boolean caseSensitive, HandleResolver resolver, short recursionCount) {
        TemplateBuilder tb = new TemplateBuilder(origvals, handle, base, extension, caseSensitive, resolver, recursionCount);
        setupTemplateBuilderForNamespace(tb);
        return tb.templateConstruct();
    }

    void setupTemplateBuilderForNamespace(TemplateBuilder tb) {
        tb.setXml(xmlInfo);
        tb.setParentNamespace(parentNamespace);
    }

    public XTag getInheritedTag(String name) {
        XTag tag = xmlInfo.getSubTag(TEMPLATE_TAG);
        if (tag != null) return tag;
        if (parentNamespace != null) return parentNamespace.getInheritedTag(name);
        return null;
    }

    @Override
    public String toString() {
        return (xmlInfo == null ? "<null>" : xmlInfo.toString()) + "Parent: " + (parentNamespace == null ? "<null>" : String.valueOf(parentNamespace));
    }

}
