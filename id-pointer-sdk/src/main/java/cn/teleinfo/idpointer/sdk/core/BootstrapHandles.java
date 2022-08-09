/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class BootstrapHandles {
    public static final int MAX_DEPTH = 20;

    public long lastUpdate;
    public Map<String, HandleRecord> handles;

    public BootstrapHandles() {
    }

    public BootstrapHandles(Map<String, HandleRecord> handles) {
        this.handles = handles;
        lastUpdate = System.currentTimeMillis();
    }

    public Set<SiteInfo> getSites(String preferredRoot) {
        if (preferredRoot == null) preferredRoot = "0.NA/0.NA";
        Set<SiteInfo> result = new LinkedHashSet<>();
        addSitesToSet(preferredRoot, result, new HashSet<String>(), 0);
        return result;
    }

    private void addSitesToSet(String handle, Set<SiteInfo> sites, Set<String> handlesVisited, int depth) {
        if (!handlesVisited.add(handle)) return;
        HandleRecord record = handles.get(handle);
        if (record == null || record.getValues() == null) return;
        SiteInfo[] rootSites = Util.getSitesAndAltSitesFromValues(record.getValuesAsArray());
        if (rootSites != null) {
            for (SiteInfo site : rootSites) {
                sites.add(site);
            }
        }

        if (depth >= MAX_DEPTH) return;

        HandleValue[] serviceValues = Util.filterValues(record.getValuesAsArray(), null, Common.SERVICE_HANDLE_TYPES);
        for (HandleValue serviceValue : serviceValues) {
            String serviceHandle = serviceValue.getDataAsString();
            addSitesToSet(serviceHandle, sites, handlesVisited, depth + 1);
        }
    }
}
