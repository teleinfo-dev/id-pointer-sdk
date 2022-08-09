/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.core.sample.LocalInfoConverter;
import cn.teleinfo.idpointer.sdk.core.sample.SiteInfoConverter;
import cn.teleinfo.idpointer.sdk.core.stream.AtomicFile;
import cn.teleinfo.idpointer.sdk.core.stream.StreamTable;
import cn.teleinfo.idpointer.sdk.core.stream.StreamVector;
import cn.teleinfo.idpointer.sdk.core.stream.util.StreamUtil;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.security.PublicKey;
import java.util.*;

public class FilesystemConfiguration extends Configuration {
    public static final String AUTO_UPDATE_ROOT_INFO = "auto_update_root_info";
    @Deprecated
    private static final String PREFERRED_ROOT = "preferred_root";
    public static final String PREFERRED_GLOBAL_SERVICE_HANDLE = "preferred_global_service_handle";
    public static final String SITE_FILTER_KEYWORDS_ATT_NAME = "site_filter_keywords";

    private HandleValue globalValues[] = null;
    private SiteInfo globalSites[] = null;
    private NamespaceInfo globalNamespace = null;
    private File globalValuesFile = null;
    private File bootstrapHandlesFile = null;

    private HandleValue cacheValues[] = null;
    private SiteInfo cacheSites[] = null;
    private boolean useCacheSitesForAll = false;
    private Map<String, SiteInfo[]> localSites = null;
    private Map<String, InetAddress> localAddresses = null;
    private int resolutionMethod = RM_GLOBAL;
    private File configDir = null;
    private final StreamTable configTable = new StreamTable();

    private BootstrapHandles bootstrapHandles;
    private List<PublicKey> rootKeys;

    FilesystemConfiguration() {
        // set the directory where all of the generic config files (including
        // the global site values) will be stored. By default, $HOME/.handle/
        try {
            String configDirName = System.getProperty("net.handle.configDir");
            if (configDirName != null) {
                configDir = new File(configDirName);
            } else {
                configDir = getDefaultConfigDir();
            }
            configDir.mkdirs();
            init();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.err.println("Had trouble finding your \"home\" directory and/or your " + "configuration settings.  Will use \"" + configDir + ".\"  ");
        }
    }

    public static File getDefaultConfigDir() {
        File userDir;
        String userDirName = System.getProperty("user.home");
        if (userDirName == null) {
            // if the user has no "home" directory, use the current directory..
            userDir = new File(System.getProperty("user.dir", File.separator));
        } else {
            userDir = new File(fixVariablesIfWindows(userDirName));
        }

        // create a file object for our config dir based on the "home" directory
        return new File(userDir, ".handle");
    }

    public FilesystemConfiguration(File configDir) {
        this.configDir = configDir;
        init();
    }

    private void init() {
        loadConfigDct();
        loadResolverServiceFile();
        loadResolverSiteFile();
        loadLocalNasFile();
        loadLocalInfoFile();
        loadLocalAddressesFile();
        loadBootstrapHandles();
    }

    private void loadConfigDct() {
        try {
            // Load configTable from the config file
            File configFile = new File(configDir, HSG.CONFIG_FILE_NAME);
            if (configFile.exists()) {
                try {
                    configTable.readFromFile(configFile);
                } catch (Exception e) {
                    System.err.println("Error reading configuration: " + e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.err.println("Error loading config.dct");
        }
    }

    private synchronized void loadBootstrapHandles() {
        boolean loadSuccess = false;
        try {
            globalValuesFile = new File(configDir, "root_info");
            bootstrapHandlesFile = new File(configDir, "bootstrap_handles");

            AtomicFile bootstrapHandlesAtomicFile = new AtomicFile(bootstrapHandlesFile);
            if (bootstrapHandlesAtomicFile.exists()) {
                // load the values from the users' config file
                String bootstrapHandlesJson = Util.decodeString(bootstrapHandlesAtomicFile.readFully());
                BootstrapHandles newBootstrapHandles = GsonUtility.getGson().fromJson(bootstrapHandlesJson, BootstrapHandles.class);
                setBootstrapHandles(newBootstrapHandles);
                loadSuccess = true;
                if (getPreferredGlobalServiceHandle() != null && !getBootstrapHandles().handles.containsKey(getPreferredGlobalServiceHandle()) && isAutoUpdateRootInfo()) {
                    // trigger an update for a new preferred MPA GHR service handle
                    bootstrapHandles.lastUpdate = 0;
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading bootstrap_handles, trying default");
            e.printStackTrace(System.err);
        }
        if (!loadSuccess) {
            try {
                loadDefaultBootstrapHandles();
                loadSuccess = true;
            } catch (Exception e) {
                System.err.println("Error loading default bootstrap_handles!");
                e.printStackTrace(System.err);
            }
        }
    }

    private void loadDefaultBootstrapHandles() throws IOException {
        // load the default global values that comes with this code
        InputStream in = getClass().getResourceAsStream("/net/handle/etc/bootstrap_handles");
        try {
            String bootstrapHandlesJson = Util.decodeString(StreamUtil.readFully(in));
            BootstrapHandles newBootstrapHandles = GsonUtility.getGson().fromJson(bootstrapHandlesJson, BootstrapHandles.class);
            setBootstrapHandles(newBootstrapHandles);
        } finally {
            if (in != null) try { in.close(); } catch (Exception e) { }
        }
    }

    private void loadResolverServiceFile() {
        try { // try to load the local cache/resolver service information

            // the file that holds the global service information
            File cacheValuesFile = new File(configDir, "resolver_service");

            if (cacheValuesFile.exists() && cacheValuesFile.canRead()) {
                // load the values from the users' config file
                FileInputStream in = new FileInputStream(cacheValuesFile);
                try {
                    cacheValues = Encoder.decodeGlobalValues(in);
                } finally {
                    try {
                        in.close();
                    } catch (Exception e) {
                    }
                }
            }

            if (cacheValues != null) {
                cacheSites = Util.getSitesAndAltSitesFromValues(cacheValues);
                if (cacheSites != null && cacheSites.length > 0) {
                    resolutionMethod = RM_WITH_CACHE;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.err.println("Unable to load local cache/resolver service information:  " + e);
        }
    }

    private void loadResolverSiteFile() {
        try { // try to load the local cache/resolver site information
            if (cacheSites == null || cacheSites.length <= 0) {
                // no local service was loaded, let's see if there is a siteinfo that
                // points to a local cache/resolver
                File cacheSiteFile = new File(configDir, "resolver_site");
                if (cacheSiteFile.exists() && cacheSiteFile.canRead()) {
                    FileInputStream in = new FileInputStream(cacheSiteFile);
                    try {
                        byte siteBuf[] = new byte[(int) cacheSiteFile.length()];
                        int r, n = 0;
                        while (n < siteBuf.length && (r = in.read(siteBuf, n, siteBuf.length - n)) >= 0) {
                            n += r;
                        }
                        SiteInfo cacheSite = new SiteInfo();
                        if (Util.looksLikeBinary(siteBuf)) {
                            Encoder.decodeSiteInfoRecord(siteBuf, 0, cacheSite);
                        } else {
                            cacheSite = SiteInfoConverter.convertToSiteInfo(new String(siteBuf, "UTF-8"));
                        }
                        SiteInfo altCacheSite = Util.getAltSiteInfo(cacheSite);
                        if (altCacheSite != null) {
                            cacheSites = new SiteInfo[] { cacheSite, altCacheSite };
                        } else {
                            cacheSites = new SiteInfo[] { cacheSite };
                        }
                        resolutionMethod = RM_WITH_CACHE;
                    } finally {
                        try {
                            in.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.err.println("Unable to load local cache/resolver site information.  " + "Will use global resolution");
        }
    }

    private void loadLocalNasFile() {
        // Try to load a list of prefixes for which all
        // messages (including admin messages) should go through the
        // cache/resolver site.
        try {
            if (cacheSites != null && cacheSites.length > 0) {
                File localNAFile = new File(configDir, "local_nas");
                if (localNAFile.exists() && localNAFile.canRead()) {
                    BufferedReader rdr = new BufferedReader(new InputStreamReader(new FileInputStream(localNAFile), "UTF8"));
                    try {
                        String line;
                        while (true) {
                            line = rdr.readLine();
                            if (line == null) {
                                break;
                            }
                            line = line.trim();
                            if (line.length() <= 0) {
                                continue;
                            }
                            if ("*".equals(line)) {
                                useCacheSitesForAll = true;
                            } else {
                                if (localSites == null) {
                                    localSites = new HashMap<>();
                                }
                                localSites.put(line.toUpperCase(java.util.Locale.ENGLISH).trim(), cacheSites);
                            }
                        }
                    } finally {
                        rdr.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.err.println("Error loading local prefix list.");
        }
    }

    private void loadLocalInfoFile() {
        // the file that holds the local service information
        File localSitesFile = new File(configDir, "local_info");
        if (!localSitesFile.exists()) {
            localSitesFile = new File(configDir, "local_info.json");
        }
        InputStream localSitesIn = null;
        try {
            if (localSitesFile.exists() && localSitesFile.canRead()) {
                try {
                    localSitesIn = Files.newInputStream(localSitesFile.toPath());
                } catch (IOException e) {
                    // ignore
                }
            }
            if (localSitesIn == null) {
                // try to find local_info in the jar file
                localSitesIn = getClass().getResourceAsStream("/net/handle/etc/local_info");
                if (localSitesIn == null) {
                    localSitesIn = getClass().getResourceAsStream("/net/handle/etc/local_info.json");
                }
            }
            if (localSitesIn != null) {
                try {
                    byte[] bytes = StreamUtil.readFully(localSitesIn);
                    if (Util.looksLikeBinary(bytes)) {
                        localSites = Encoder.decodeLocalSites(new ByteArrayInputStream(bytes));
                    } else {
                        localSites = LocalInfoConverter.convertFromJson(Util.decodeString(bytes));
                    }
                } catch (Exception e) {
                    System.err.println("Error reading " + localSitesFile.getPath());
                }
            }
        } finally {
            if (localSitesIn != null) {
                try {
                    localSitesIn.close();
                } catch (IOException e) {
                }
            }
        }
        if (localSites == null) {
            localSites = new HashMap<>(0);
        }
    }

    // The local_addresses file maps public IP addresses to local IP addresses, as a workaround for certain NAT/firewall configurations
    private void loadLocalAddressesFile() {
        File localAddressesFile = new File(configDir, "local_addresses");
        if (localAddressesFile.exists() && localAddressesFile.canRead()) {
            try (FileInputStream fi = new FileInputStream(localAddressesFile)) {
                setLocalAddressMap(Encoder.decodeLocalAddresses(fi));
            } catch (Exception e) {
                System.err.println("Error reading local address map: " + e);
                e.printStackTrace(System.err);
                setLocalAddressMap(null);
            }
        }
    }

    @Override
    public void configureResolver(HandleResolverInterface resolver) {
        configureResolverUsingKeys(resolver, configTable);
    }

    public static void configureResolverUsingKeys(HandleResolverInterface resolver, StreamTable configTable) {
        if (configTable.containsKey("tcp_timeout")) {
            int timeout = Integer.parseInt((String) configTable.get("tcp_timeout"));
            resolver.setTcpTimeout(timeout);
        }


        resolver.setTraceMessages(configTable.getBoolean("trace_resolution") || configTable.getBoolean("trace_outgoing_messages"));
//        resolver.traceMessages = ;

        if (configTable.getBoolean(HSG.NO_UDP, false)) {
            resolver.setPreferredProtocols(new int[] { Interface.SP_HDL_TCP, Interface.SP_HDL_HTTP });
        }

        if (configTable.containsKey(SITE_FILTER_KEYWORDS_ATT_NAME)) {
            Object obj = configTable.get(SITE_FILTER_KEYWORDS_ATT_NAME);
            if (obj instanceof StreamVector) {
                StreamVector keywordVector = (StreamVector) obj;
                resolver.setSiteFilter(new KeywordSiteFilter(asStringArray(keywordVector)));
            } else {
                resolver.setSiteFilter(new KeywordSiteFilter(splitAtWhitespace(String.valueOf(obj))));
            }
        }

        if (configTable.containsKey("ipv6_fast_fallback")) {
            resolver.setUseIPv6FastFallback(configTable.getBoolean("ipv6_fast_fallback", true));
        }
    }

    private static String[] asStringArray(StreamVector keywordVector) {
        String[] res = new String[keywordVector.size()];
        int i = 0;
        for (Object obj : keywordVector) {
            res[i] = (String) obj;
        }
        return res;
    }

    private static class KeywordSiteFilter implements SiteFilter {
        List<String> keywords;

        public KeywordSiteFilter(String[] keywords) {
            this.keywords = Arrays.asList(keywords);
        }

        @Override
        public boolean apply(SiteInfo site) {
            if (site.attributes == null) {
                return false;
            }
            List<String> thisSiteKeywords = new ArrayList<>();
            for (Attribute att : site.attributes) {
                if (SITE_FILTER_KEYWORDS_ATT_NAME.equals(Util.decodeString(att.name))) {
                    String[] thisAttKeywords = splitAtWhitespace(Util.decodeString(att.value));
                    for (String keyword : thisAttKeywords) {
                        thisSiteKeywords.add(keyword);
                    }
                }
            }
            return !Collections.disjoint(keywords, thisSiteKeywords);
        }
    }

    static String[] splitAtWhitespace(String s) {
        return s.trim().split("\\s++");
    }

    @Override
    public void setLocalSites(String na, SiteInfo sites[]) {
        localSites.put(na.toUpperCase(java.util.Locale.ENGLISH), sites);
    }

    /**
     * Sets a map that converts IP addresses to alternate addresses. This is used to map IP addresses that are viewable outside of firewalls to IP
     * addresses that are accessibile from inside a firewall. This is needed for certain NAT firewall/routers. The localAddrMap parameter should map
     * String representations.
     */
    @Override
    public synchronized void setLocalAddressMap(Map<String, String> localAddrMap) {
        if (localAddrMap == null) {
            localAddresses = null;
        } else {
            // go through the mapping and convert the keys to Strings,
            // and the values to InetAddress objects (if necessary)

            Map<String, InetAddress> tmp = new HashMap<>();
            for (Iterator<String> it = localAddrMap.keySet().iterator(); it.hasNext();) {
                String key = it.next();
                if (key == null) {
                    continue;
                }
                String val = localAddrMap.get(key);
                if (val == null) {
                    continue;
                }

                try {
                    tmp.put(key, InetAddress.getByName(String.valueOf(val)));
                } catch (Exception e) {
                    System.err.println("Invalid local address: " + key + " -> " + val);
                }
            }
            localAddresses = tmp;
        }
    }

    static String fixVariablesIfWindows(String s) {
        if (!System.getProperty("os.name", "").startsWith("Windows")) {
            return s;
        } else {
            return fixForWindows(s);
        }
    }

    static String fixForWindows(String s) {
        int start = s.indexOf('%');
        if (start < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s);
        while (start >= 0) {
            int end = sb.indexOf("%", start + 1);
            if (end < 0) {
                break;
            }
            String var = sb.substring(start + 1, end);
            String value = System.getenv(var);
            if (value == null) {
                value = getenvCaseInsensitive(var);
            }
            sb.replace(start, end + 1, value);
            start = sb.indexOf("%", end + 1);
        }
        return sb.toString();
    }

    static String getenvCaseInsensitive(String var) {
        for (String name : System.getenv().keySet()) {
            if (name.equalsIgnoreCase(var)) {
                return System.getenv(name);
            }
        }
        return null;
    }

    /**
     * Saves the local address map to the appropriate configuration file
     */
    @Override
    public synchronized void saveLocalAddressMap() throws IOException {
        Map<String, String> localAddr = getLocalAddressMap();
        File localAddressesFile = new File(configDir, "local_addresses");
        if (localAddr == null) {
            if (localAddressesFile.exists()) {
                localAddressesFile.delete();
            }
        } else {
            try (FileOutputStream fo = new FileOutputStream(localAddressesFile)) {
                Encoder.writeLocalAddresses(localAddr, fo);
            } catch (Exception e) {
                System.err.println("Error saving local address map: " + e);
            }
        }
    }

    /**
     * Gets the mapping of addresses to local addresses
     */
    @Override
    public synchronized Map<String, String> getLocalAddressMap() {
        Map<String, String> res = new HashMap<>();
        for (Map.Entry<String, InetAddress> entry : localAddresses.entrySet()) {
            res.put(entry.getKey(), Util.rfcIpRepr(entry.getValue()));
        }
        return res;
    }

    /**
     * If the given address appears in the local address map return the address to which it is mapped. Otherwise, return the given parameter.
     */
    @Override
    public InetAddress mapLocalAddress(InetAddress addr) {
        Map<String, InetAddress> local = localAddresses;
        if (local == null) {
            return addr;
        }

        InetAddress val = local.get(Util.rfcIpRepr(addr));
        if (val != null) {
            try {
                return val;
            } catch (Exception e) {
                System.err.println("Invalid address map: " + addr + " -> " + val);
            }
        }
        return addr;
    }

    @Override
    public SiteInfo[] getLocalSites(String na) {
        SiteInfo[] res = localSites.get(na.toUpperCase(java.util.Locale.ENGLISH));
        if (res == null && useCacheSitesForAll) {
            return cacheSites;
        }
        return res;
    }

    @Override
    @Deprecated
    public synchronized void setGlobalValues(HandleValue globalValues[]) {
        this.globalValues = globalValues;
        this.globalNamespace = Util.getNamespaceFromValues(globalValues);

        SiteInfo newSites[] = Util.getSitesAndAltSitesFromValues(globalValues);
        if (newSites != null) {
            setGlobalSites(newSites);
        }
    }

    public void setGlobalValuesFile(File newGlobalValuesFile) {
        this.globalValuesFile = newGlobalValuesFile;
    }

    @Override
    public void setBootstrapHandles(BootstrapHandles bootstrapHandles) {
        this.bootstrapHandles = bootstrapHandles;
        Set<SiteInfo> sites = bootstrapHandles.getSites(getPreferredGlobalServiceHandle());
        if (sites != null && !sites.isEmpty()) {
            setGlobalSites(sites.toArray(new SiteInfo[sites.size()]));
        } else if (getPreferredGlobalServiceHandle() != null) {
            sites = bootstrapHandles.getSites(null);
            if (sites != null && !sites.isEmpty()) {
                System.err.println("No sites at preferred global service handle " + getPreferredGlobalServiceHandle() + ", using 0.NA/0.NA");
                setGlobalSites(sites.toArray(new SiteInfo[sites.size()]));
            }
        }
        if (sites == null || sites.isEmpty()) {
            System.err.println("No global sites found!");
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

    /********************************************************************
     * save the default global values in the users' config directory
     *********************************************************************/
    @Override
    public synchronized void persist() {
        try {
            new AtomicFile(globalValuesFile).writeFully(Encoder.encodeGlobalValues(globalValues));
        } catch (Exception e) {
            System.err.println("Error saving global values to: " + globalValuesFile);
            e.printStackTrace();
        }
        System.err.println("Saving bootstrap handles to: " + bootstrapHandlesFile);
        try {
            String bootstrapHandlesJson = GsonUtility.getPrettyGson().toJson(bootstrapHandles);
            new AtomicFile(bootstrapHandlesFile).writeFully(Util.encodeString(bootstrapHandlesJson));
        } catch (Exception e) {
            System.err.println("Error saving bootstrap handles values to: " + bootstrapHandlesFile);
            e.printStackTrace();
        }
    }

    public void setConfigDir(File newConfigDir) {
        this.configDir = newConfigDir;
    }

    public File getConfigDir() {
        return configDir;
    }

    @Override
    public HandleValue[] getGlobalValues() {
        return globalValues;
    }

    @Override
    public NamespaceInfo getGlobalNamespace() {
        return globalNamespace;
    }

    @Override
    public void setGlobalSites(SiteInfo globalSites[]) {
        this.globalSites = globalSites;
        for (int i = this.globalSites.length - 1; i >= 0; i--) {
            this.globalSites[i].isRoot = true;
        }
    }

    @Override
    public SiteInfo[] getGlobalSites() {
        return globalSites;
    }

    @Override
    public void setCacheSites(SiteInfo cacheSites[]) {
        this.cacheSites = cacheSites;
    }

    @Override
    public SiteInfo[] getCacheSites() {
        return cacheSites;
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
    public boolean isAutoUpdateRootInfo() {
        return configTable.getBoolean(AUTO_UPDATE_ROOT_INFO, true);
    }

    @Override
    public void setAutoUpdateRootInfo(boolean enabled) {
        configTable.put(AUTO_UPDATE_ROOT_INFO, enabled);
    }

    @Override
    public String getPreferredGlobalServiceHandle() {
        String res = configTable.getStr(PREFERRED_GLOBAL_SERVICE_HANDLE, null);
        if (res != null) return res;
        return configTable.getStr(PREFERRED_ROOT, null);
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

}
