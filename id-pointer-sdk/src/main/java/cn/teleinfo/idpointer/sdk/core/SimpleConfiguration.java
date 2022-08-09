/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.core.stream.util.StreamUtil;

import java.io.InputStream;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleConfiguration extends Configuration {
    private HandleValue globalValues[] = null;
    private SiteInfo globalSites[] = null;
    private NamespaceInfo globalNamespace = null;
    private BootstrapHandles bootstrapHandles;
    private List<PublicKey> rootKeys;

    private SiteInfo cacheSites[] = null;
    private int resolutionMethod = RM_GLOBAL;

    private Map<String, SiteInfo[]> localSites = null;
    private boolean useCacheSitesForAll = false;

    public SimpleConfiguration() {
        try {
            InputStream in = getClass().getResourceAsStream("/net/handle/etc/bootstrap_handles");
            try {
                String bootstrapHandlesJson = Util.decodeString(StreamUtil.readFully(in));
                BootstrapHandles newBootstrapHandles = GsonUtility.getGson().fromJson(bootstrapHandlesJson, BootstrapHandles.class);
                setBootstrapHandles(newBootstrapHandles);
            } finally {
                in.close();
            }
            localSites = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public SiteInfo[] getGlobalSites() {
        return globalSites;
    }

    @Override
    public HandleValue[] getGlobalValues() {
        return globalValues;
    }

    @Override
    public void setGlobalSites(SiteInfo[] globalSites) {
        this.globalSites = globalSites;
        for (int i = this.globalSites.length - 1; i >= 0; i--) {
            this.globalSites[i].isRoot = true;
        }
    }

    @Override
    public NamespaceInfo getGlobalNamespace() {
        return globalNamespace;
    }

    @Override
    @Deprecated
    public void setGlobalValues(HandleValue[] globalValues) {
        this.globalValues = globalValues;
        this.globalNamespace = Util.getNamespaceFromValues(globalValues);
        SiteInfo newSites[] = Util.getSitesAndAltSitesFromValues(globalValues);
        if (newSites != null) {
            setGlobalSites(newSites);
        }
    }

    @Override
    public void persist() {
        // ignored
    }

    @Override
    public int getResolutionMethod() {
        return resolutionMethod;
    }

    @Override
    public void setResolutionMethod(int resolutionMethod) {
        this.resolutionMethod = resolutionMethod;
    }

    @Override
    public void setCacheSites(SiteInfo cacheSites[]) {
        this.cacheSites = cacheSites;
    }

    /**
     * Returns the sites to be used for all resolutions.
     */
    @Override
    public SiteInfo[] getCacheSites() {
        return cacheSites;
    }

    @Override
    public void setLocalSites(String na, SiteInfo sites[]) {
        localSites.put(na.toUpperCase(java.util.Locale.ENGLISH), sites);
    }

    /**
     * Returns the sites to be used for all administrative operations under the given prefix.
     *
     * @param na the prefix for which administrative sites are being retrieved
     * @return the sites to be used for all administrative operations under the given prefix
     */
    @Override
    public SiteInfo[] getLocalSites(String na) {
        SiteInfo[] res = localSites.get(na.toUpperCase(java.util.Locale.ENGLISH));
        if (res == null && useCacheSitesForAll) return cacheSites;
        return res;
    }

    public void setUseCacheSitesForAll(boolean useCacheSitesForAll) {
        this.useCacheSitesForAll = useCacheSitesForAll;
    }

    @Override
    public List<PublicKey> getRootKeys() {
        return rootKeys;
    }

    @Override
    public void setRootKeys(List<PublicKey> rootKeys) {
        this.rootKeys = rootKeys;
    }

    @Override
    public BootstrapHandles getBootstrapHandles() {
        return bootstrapHandles;
    }

    @Override
    public void setBootstrapHandles(BootstrapHandles bootstrapHandles) {
        this.bootstrapHandles = bootstrapHandles;
        Set<SiteInfo> sites = bootstrapHandles.getSites(null);
        if (sites != null && !sites.isEmpty()) {
            setGlobalSites(sites.toArray(new SiteInfo[sites.size()]));
        }
        HandleRecord rootOfResolution = this.bootstrapHandles.handles.get("0.NA/0.NA");
        this.globalValues = rootOfResolution.getValuesAsArray();
        this.globalNamespace = Util.getNamespaceFromValues(rootOfResolution.getValuesAsArray());
        HandleRecord trustRoot = this.bootstrapHandles.handles.get("0.0/0.0");
        @SuppressWarnings("hiding")
        List<PublicKey> rootKeys = Util.getPublicKeysFromValues(trustRoot.getValuesAsArray());
        if (rootKeys != null && !rootKeys.isEmpty()) {
            setRootKeys(rootKeys);
        }
    }

    @Override
    public boolean isAutoUpdateRootInfo() {
        return true;
    }

    @Override
    public void setAutoUpdateRootInfo(boolean enabled) {
        if (!enabled) throw new UnsupportedOperationException();
    }
}
