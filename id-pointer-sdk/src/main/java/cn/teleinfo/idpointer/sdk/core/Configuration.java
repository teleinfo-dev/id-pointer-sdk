/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.core.trust.ChainBuilder;
import cn.teleinfo.idpointer.sdk.core.trust.ChainVerifier;
import cn.teleinfo.idpointer.sdk.core.trust.HandleRecordTrustVerifier;

import java.io.IOException;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.*;

public abstract class Configuration {
    public static final int RM_GLOBAL = 0;
    public static final int RM_WITH_CACHE = 1;

    private static Configuration defaultConfig = null;

    /** Get the current default configuration. */
    public static final synchronized Configuration defaultConfiguration() {
        if (defaultConfig == null) {
            defaultConfig = new FilesystemConfiguration();
        }

        return defaultConfig;
    }

    private volatile boolean startedAutoUpdate;
    private final Object synchronizeUpdateLock = new Object();
    private volatile boolean updatingRootInfo = false;
    private Vector<RootInfoListener> rootInfoNotifications = null;
    private final Object synchronizeRootInfoNotifications = new Object();

    @SuppressWarnings("unused")
    public void setResolutionMethod(int resolutionMethod) {
        throw new UnsupportedOperationException();
    }

    public int getResolutionMethod() {
        return RM_GLOBAL;
    }

    public SiteInfo[] getCacheSites() {
        return null;
    }

    public void setCacheSites(@SuppressWarnings("unused") SiteInfo cacheSites[]) {
        throw new UnsupportedOperationException();
    }

    public abstract SiteInfo[] getGlobalSites();

    public abstract void setGlobalSites(SiteInfo globalSites[]);

    public abstract NamespaceInfo getGlobalNamespace();

    public abstract HandleValue[] getGlobalValues();

    @Deprecated
    public abstract void setGlobalValues(HandleValue globalValues[]);

    public abstract List<PublicKey> getRootKeys();

    public abstract void setRootKeys(List<PublicKey> rootKeys);

    public abstract BootstrapHandles getBootstrapHandles();

    public abstract void setBootstrapHandles(BootstrapHandles bootstrapHandles);

    public abstract void persist();

    public SiteInfo[] getLocalSites(byte na[]) {
        return getLocalSites(Util.decodeString(na));
    }

    @SuppressWarnings("unused")
    public SiteInfo[] getLocalSites(String na) {
        return null;
    }

    public InetAddress mapLocalAddress(InetAddress addr) {
        return addr;
    }

    public Map<String, String> getLocalAddressMap() {
        return null;
    }

    @SuppressWarnings("unused")
    public void saveLocalAddressMap() throws IOException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public void setLocalAddressMap(Map<String, String> localAddrMap) {
        throw new UnsupportedOperationException();
    }

    public void setLocalSites(byte na[], SiteInfo sites[]) {
        setLocalSites(Util.decodeString(na), sites);
    }

    @SuppressWarnings("unused")
    public void setLocalSites(String na, SiteInfo sites[]) {
        throw new UnsupportedOperationException();
    }

    public String getPreferredGlobalServiceHandle() {
        return null;
    }

    @SuppressWarnings("unused")
    public void configureResolver(HandleResolverInterface resolver) {
        // no-op
    }

    public abstract boolean isAutoUpdateRootInfo();

    public abstract void setAutoUpdateRootInfo(boolean enabled);

    public void addRootInfoListener(RootInfoListener listener) {
        synchronized (synchronizeRootInfoNotifications) {
            if (rootInfoNotifications == null) rootInfoNotifications = new Vector<>();
            rootInfoNotifications.addElement(listener);
        }
    }

    public void removeRootInfoListener(RootInfoListener listener) {
        synchronized (synchronizeRootInfoNotifications) {
            if (rootInfoNotifications == null) return;
            rootInfoNotifications.removeElement(listener);
        }
    }

    public void notifyRootInfoOutdated(final HandleResolverInterface resolver) {
        synchronized (synchronizeRootInfoNotifications) {
            if (rootInfoNotifications != null && rootInfoNotifications.size() > 0) {
                for (Enumeration<RootInfoListener> enumeration = rootInfoNotifications.elements(); enumeration.hasMoreElements();) {
                    enumeration.nextElement().rootInfoOutOfDate(this);
                }
                return;
            }
        }
        if (!isAutoUpdateRootInfo()) return;
        if (updatingRootInfo) return;
        synchronized (synchronizeUpdateLock) {
            if (updatingRootInfo) return;
            updatingRootInfo = true;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    refreshRootInfoFromNet(resolver);
                } catch (Exception e) {
                    System.err.println("Error refreshing root info: " + e);
                    e.printStackTrace(System.err);
                }
            }
        }.start();
    }

    private void refreshRootInfoFromNet(HandleResolverInterface resolver) throws HandleException {
        try {
            if (!isAutoUpdateRootInfo()) return;
            updatingRootInfo = true;
            resolveVerifyUpdateAndPersistBootstrapHandles(resolver);
        } finally {
            updatingRootInfo = false;
        }
    }

    private void resolveVerifyUpdateAndPersistBootstrapHandles(HandleResolverInterface resolver) throws HandleException {
        resolver.setCheckSignatures(true); // should already be set, but just to be safe
        HandleValue[] trustRoot = resolveHandleCertified(resolver, Common.TRUST_ROOT_HANDLE);
        HandleValue[] rootOfResolution = resolveHandleCertified(resolver, Common.ROOT_HANDLE);
        Map<String, HandleRecord> map = new LinkedHashMap<>();
        map.put("0.0/0.0", new HandleRecord("0.0/0.0", trustRoot));
        map.put("0.NA/0.NA", new HandleRecord("0.NA/0.NA", rootOfResolution));
        addServiceHandles(resolver, rootOfResolution, map);
        String preferredGlobalServiceHandle = getPreferredGlobalServiceHandle();
        if (preferredGlobalServiceHandle != null && !map.containsKey(preferredGlobalServiceHandle)) {
            HandleValue[] preferredGlobalServiceHandleValues = null;
            try {
                preferredGlobalServiceHandleValues = resolveHandleCertified(resolver, Util.encodeString(preferredGlobalServiceHandle));
            } catch (HandleException e) {
                // ignore
            }
            if (preferredGlobalServiceHandleValues != null) {
                map.put(preferredGlobalServiceHandle, new HandleRecord(preferredGlobalServiceHandle, preferredGlobalServiceHandleValues));
                addServiceHandles(resolver, preferredGlobalServiceHandleValues, map);
            }
        }
        ChainBuilder chainBuilder = new ChainBuilder(map, resolver);
        ChainVerifier chainVerifier = new ChainVerifier(getRootKeys());
        HandleRecordTrustVerifier handleRecordTrustVerifier = new HandleRecordTrustVerifier(chainBuilder, chainVerifier);
        Iterator<HandleRecord> iter = map.values().iterator();
        while (iter.hasNext()) {
            HandleRecord record = iter.next();
            boolean valid = handleRecordTrustVerifier.validateHandleRecord(record);
            if (!valid) {
                if ("0.0/0.0".equals(record.getHandle()) || "0.NA/0.NA".equals(record.getHandle())) {
                    System.err.println("Unable to validate root handles");
                    return;
                } else {
                    System.err.println("Unable to validate " + record.getHandle() + ", so it will not be used for global sites");
                }
                iter.remove();
            }
        }

        BootstrapHandles bootstrapHandles = new BootstrapHandles(map);
        Set<SiteInfo> sites = bootstrapHandles.getSites(null);
        List<PublicKey> rootKeys = Util.getPublicKeysFromValues(trustRoot);
        if (sites == null || sites.isEmpty()) {
            System.err.println("Unable to extract root site information");
        } else if (rootKeys == null || rootKeys.isEmpty()) {
            System.err.println("Unable to extract root key information");
        } else {
            setBootstrapHandles(bootstrapHandles);
            persist();
        }
    }

    private void addServiceHandles(HandleResolverInterface resolver, HandleValue[] rootValues, Map<String, HandleRecord> map) {
        HandleValue[] serviceValues = Util.filterValues(rootValues, null, Common.SERVICE_HANDLE_TYPES);
        for (HandleValue serviceValue : serviceValues) {
            String serviceHandle = serviceValue.getDataAsString();
            addServiceHandleRecursivelyToMap(resolver, serviceHandle, map, 1);
        }
    }

    private void addServiceHandleRecursivelyToMap(HandleResolverInterface resolver, String handle, Map<String, HandleRecord> map, int depth) {
        if (map.containsKey(handle)) return;
        HandleValue[] values;
        try {
            values = resolveHandleCertified(resolver, Util.encodeString(handle));
        } catch (HandleException e) {
            return;
        }
        if (values == null) return;
        map.put(handle, new HandleRecord(handle, values));
        if (depth >= BootstrapHandles.MAX_DEPTH) return;
        HandleValue[] serviceValues = Util.filterValues(values, null, Common.SERVICE_HANDLE_TYPES);
        for (HandleValue serviceValue : serviceValues) {
            String serviceHandle = serviceValue.getDataAsString();
            addServiceHandleRecursivelyToMap(resolver, serviceHandle, map, depth + 1);
        }
    }

    private HandleValue[] resolveHandleCertified(HandleResolverInterface resolver, byte[] handle) throws HandleException {
        ResolutionRequest req = new ResolutionRequest(handle, null, null, null);
        // the request *must* be certified, otherwise all security is compromised
        // (actual signature checking solves the problem; but we use cert anyway)
        req.certify = true;
        AbstractResponse resp = resolver.processRequest(req);
        if (resp.responseCode != AbstractMessage.RC_SUCCESS) {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Unable to query root info");
        }
        HandleValue[] handleValues = ((ResolutionResponse) resp).getHandleValues();
        return handleValues;
    }

    public void checkRootInfoUpToDate(HandleResolverInterface resolver, String handle, HandleValue[] handleValues) {
        if (!isAutoUpdateRootInfo()) return;
        HandleValue[] knownValues;
        try {
            knownValues = getBootstrapHandles().handles.get(handle).getValuesAsArray();
        } catch (Exception e) {
            return;
        }
        if (HandleValue.unorderedEquals(handleValues, knownValues)) return;
        notifyRootInfoOutdated(resolver);
    }

    public boolean isBootstrapHandlesOld() {
        BootstrapHandles bootstrapHandles = getBootstrapHandles();
        long lastUpdate = 0;
        if (bootstrapHandles != null) {
            lastUpdate = bootstrapHandles.lastUpdate;
        }
        long now = System.currentTimeMillis();
        return (now - lastUpdate > 82800000L); // twenty-three hours
    }

    private void updateBootstrapHandlesIfOld(final HandleResolverInterface resolver) {
        if (!isAutoUpdateRootInfo()) return;
        if (!isBootstrapHandlesOld()) return;
        if (updatingRootInfo) return;
        synchronized (synchronizeUpdateLock) {
            if (updatingRootInfo) return;
            updatingRootInfo = true;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    refreshRootInfoFromNet(resolver);
                } catch (Exception e) {
                    System.err.println("Error refreshing root info: " + e);
                    e.printStackTrace(System.err);
                }
            }
        }.start();
    }

    public void startAutoUpdate(final HandleResolverInterface resolver) {
        if (startedAutoUpdate) return;
        synchronized (synchronizeUpdateLock) {
            if (startedAutoUpdate) return;
            startedAutoUpdate = true;
        }
        if (!isAutoUpdateRootInfo()) return;
        new Timer("resolver-config-auto-update", true).schedule(new TimerTask() {
            @Override
            public void run() {
                updateBootstrapHandlesIfOld(resolver);
            }
        }, 0, 86400000L);
    }
}
