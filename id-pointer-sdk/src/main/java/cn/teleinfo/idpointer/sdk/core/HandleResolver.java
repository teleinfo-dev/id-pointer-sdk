/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
 All rights reserved.

 The HANDLE.NET software is made available subject to the
 Handle.Net Public License Agreement, which may be obtained at
 http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
 \**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.core.stream.util.StringUtils;
import cn.teleinfo.idpointer.sdk.core.util.LRUCacheTable;
import cn.teleinfo.idpointer.sdk.security.HdlSecurityProvider;

import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Responsible for locating and retrieving the value of handles using
 *  a caching server, or on the internet.
 */
public class HandleResolver implements RequestProcessor,HandleResolverInterface {
    public boolean traceMessages = false;
    @Override
    public void setTraceMessages(boolean traceMessages) {
        this.traceMessages = traceMessages;
    }

    @Override
    public int[] getPreferredProtocols() {
        return preferredProtocols;
    }

    @Override
    public LRUCacheTable<String, Long> getResponseTimeTbl() {
        return responseTimeTbl;
    }

    @Override
    public LRUCacheTable<String, Long> getPreferredPrimaryTbl() {
        return preferredPrimaryTbl;
    }


    private static final byte HTTP_ACCEPT_HEADER[] = Util.encodeString("Accept: " + Common.HDL_MIME_TYPE + "\r\n");
    private static final byte HTTP_AGENT_HEADER[] = Util.encodeString("User-Agent: CNRI-HCL 2.0\r\n");
    private static final byte HTTP_CONTENT_TYPE_HEADER[] = Util.encodeString("Content-Type: " + Common.HDL_MIME_TYPE + "\r\n");
    private static final byte HTTP_NEWLINE[] = Util.encodeString("\r\n");

    private Random messageIDRandom; // used for generating random message IDs

    private Configuration config;

    boolean useIPv6FastFallback = true;

    int preferredProtocols[] = { Interface.SP_HDL_UDP, Interface.SP_HDL_TCP, Interface.SP_HDL_HTTP, Interface.SP_HDL_HTTPS };
    private final int recursionCountLimit = 40;
    private ClientSessionTracker resolverSessions = null;
    private int maxUDPDataSize = Common.MAX_UDP_DATA_SIZE;
    private Cache secureCache = null;
    private Cache cache = null;
    private int udpRetryScheme[] = { 500, 1000, 1500 };

    // the default length of time that a not-found result will be cached
    private static final int CACHED_NOT_FOUND_TTL = 0; // 60*60;

    // timeout used for normal hdl-tcp and hdl-http connections(1 minute)
    private int tcpTimeout = 60000;

    // true if this resolver should check for and verify
    // signatures on responses to requests that have the
    // 'certify' bit set.
    private boolean checkSignatures = true;

    private SiteFilter siteFilter;

    // the length of time that not-found responses should be cached
    // TODO: get this result from the prefix
    private int notFoundCacheTTL = CACHED_NOT_FOUND_TTL;

    // keep performance data for sites
    LRUCacheTable<String, Long> responseTimeTbl = new LRUCacheTable<>(1024);
    LRUCacheTable<String, Long> preferredPrimaryTbl = new LRUCacheTable<>(1024);
    private static final long USE_SAME_PRIMARY_MILLIS = 60 * 60 * 1000;

    private static final short IP_VERSION_6 = 6;
    private static final short IP_VERSION_4 = 4;

    // for servers that support more than just DES (protocol >= 2.2) when the client generates the key
    private final int preferredEncryptionAlgorithm = HdlSecurityProvider.ENCRYPT_ALG_AES;

    private static boolean hasIPv4Interface = true;
    private static boolean hasIPv6Interface = true;
    private volatile ExecutorService execServ;

    public HandleResolver() {
        setCache(new MemCache());
        setCertifiedCache(new MemCache());

        setSessionTracker(new ClientSessionTracker(new SessionSetupInfo()));

        try {
            messageIDRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException nse) { // this should never happen
            //System.err.println("Warning:  using insecure random number generator for request IDs!");
            messageIDRandom = new SecureRandom();
        }
        messageIDRandom.setSeed(System.nanoTime());

        setConfiguration(Configuration.defaultConfiguration());
    }

    static {
        // Java before 1.7.0_60 has a memory leak in NetworkInterface.isLoopback(), so we do this statically to prevent leak
        try {
            checkInterfaces(NetworkInterface.getNetworkInterfaces());
            if (!hasIPv4Interface && !hasIPv6Interface) {
                hasIPv4Interface = true;
                hasIPv6Interface = true;
            }
        } catch (Exception e) {
            hasIPv4Interface = true;
            hasIPv6Interface = true;
        }
    }

    private static void checkInterfaces(Enumeration<NetworkInterface> intfs) throws SocketException {
        while (intfs.hasMoreElements()) {
            NetworkInterface intf = intfs.nextElement();
            if (intf.isLoopback()) continue;
            Enumeration<InetAddress> addresses = intf.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address instanceof Inet6Address) hasIPv6Interface = true;
                if (address instanceof Inet4Address) hasIPv4Interface = true;
                if (hasIPv6Interface && hasIPv4Interface) return;
            }
            checkInterfaces(intf.getSubInterfaces());
        }
    }

    private static class CachedThreadPoolHolder {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        static final ExecutorService execServ = Executors.newCachedThreadPool(new ThreadFactory() {
            private final String namePrefix = "resolver-" + poolNumber.getAndIncrement() + "-ipv4-thread-";
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
    }

    public ExecutorService getExecutorService() {
        if (execServ != null) return execServ;
        return CachedThreadPoolHolder.execServ;
    }

    public void setExecutorService(ExecutorService execServ) {
        this.execServ = execServ;
    }

    /*****************************************************************************
     *
     * Return a copy of preferredProtocols[], wherein protocols are listed in
     * order of preference.  For use by methods which do not have access to the
     * private int-array.
     *
     */

    public int[] protocolsByPreference() {
        int protocolList[] = new int[preferredProtocols.length];

        for (int i = 0; i < preferredProtocols.length; i++) {
            protocolList[i] = preferredProtocols[i];
        }

        return protocolList;
    }

    /** this method will retrieve the handle values by the given handle/index pair
     Now only used to retrieve public key data in veryfying the session setup
     data.
     */
    public byte[] retrieveHandleIndexData(byte handle[], int index) throws Exception {
        // first retrieve the public key (checking server signatures, of course)
        ResolutionRequest req = new ResolutionRequest(handle, null, new int[] { index }, null);
        req.certify = true;

        AbstractResponse response = this.processRequest(req);

        if (!(response instanceof ResolutionResponse)) throw new Exception("Unable to verify resolve the handle/index \n" + response);

        HandleValue values[] = ((ResolutionResponse) response).getHandleValues();
        if (values == null || values.length < 1) throw new Exception("The index specified does not exist\n");

        // return the value data
        return values[0].getData();
    }

    /** create a new session setup object using any existing session information
     that may be around.
     */
    SessionSetupRequest createSessionSetupRequest(AuthenticationInfo authInfo, SessionSetupInfo options, int majorProtocolVersion, int minorProtocolVersion) throws HandleException {
        if (options == null) {
            throw new HandleException(HandleException.INVALID_VALUE, "Cannot create session setup request with null SessionSetupInfo");
        }

        SessionSetupRequest ssreq = new SessionSetupRequest();
        ssreq.keyExchangeMode = options.keyExchangeMode;
        if (authInfo != null) {
            ssreq.identityHandle = authInfo.getUserIdHandle();
            ssreq.identityIndex = authInfo.getUserIdIndex();
        }

        if (options.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_HDL) {
            ssreq.exchangeKeyHandle = options.exchangeKeyHandle;
            ssreq.exchangeKeyIndex = options.exchangeKeyIndex;
        } else if (options.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_CLIENT || options.keyExchangeMode == Common.KEY_EXCHANGE_DH) {
            options.initDHKeys();
            ssreq.publicKey = options.publicExchangeKey;
        }

        ssreq.certify = true;
        ssreq.encrypt = false;
        ssreq.returnRequestDigest = true;

        ssreq.majorProtocolVersion = (byte) majorProtocolVersion;
        ssreq.minorProtocolVersion = (byte) minorProtocolVersion;
        ssreq.setSupportedProtocolVersion();

        // session set up request can not be encrypted, because there is no session
        // key setup yet encrypted? authenticated? timeout?
        ssreq.encryptAllSessionMsg = options.encrypted;
        ssreq.authAllSessionMsg = options.authenticated;
        if (options.timeout > 0) ssreq.timeout = options.timeout;
        return ssreq;
    }

    /** retrieve the session option for a specific authenticated user.
     if auth is null, the anonymous session option will be returned.
     public SessionSetupInfo retrieveUserSessionOption(AuthenticationInfo auth) {
     if (clientSessionMan != null) {
     return clientSessionMan.getUserSessionOption(auth);
     } else
     return null;
     }
     */

    /** Gives the resolver a Cache object to use when resolving.
     * When sending requests, the cache will be checked for the
     * handle instead of always using the network.  Setting the
     * cache object to null will cause the resolver to not use
     * any cache.
     */
    public void setCache(Cache cache) {
        if (this.secureCache != null && this.secureCache == cache) throw new RuntimeException("Error:  attempt to set the certified and regular cache to the same value");
        this.cache = cache;
    }

    /** Gives the resolver a Cache object to use for certified resolutions.
     * When sening certified resolution requests, this cache will be
     * checked for the handle instead of always using the network.  Setting the
     * cache object to null will cause the resolver to not use any cache for
     * certified resolutions.  Note:  It is important to never use the same
     * cache (or backing storage) for the certified and regular cache.  Doing
     * so could poison the certified cache with uncertified values.
     */
    public void setCertifiedCache(Cache cache) {
        if (this.cache != null && this.cache == cache) throw new RuntimeException("Error:  attempt to set the certified and regular cache to the same value");
        this.secureCache = cache;
    }

    /** Clear any caches being used by this resolver */
    public void clearCaches() throws Exception {
        Cache c;
        c = secureCache;
        if (c != null) c.clear();
        c = cache;
        if (c != null) c.clear();
    }

    /*************************************************************
     * Gives the resolver a session tracker object to use when
     * resolving.  When sending administrative requests and the
     * resolver's session tracker is non-null, it is used to
     * establish (or continue) a session with whatever server is
     * being communicated with.  Note: If there is a sessionInfo
     * or session tracker already associated with a request, then
     * the resolver's session tracker is ignored.
     * Warning:  If this resolver is going to be used in several
     * administrative contexts (ie with several different admin IDs)
     * because authenticated sessions could possibly be used by a
     * different administrator than was intended.
     *************************************************************/
    public void setSessionTracker(ClientSessionTracker sessionTracker) {
        this.resolverSessions = sessionTracker;
    }

    /** Returns the current default session tracker. */
    public ClientSessionTracker getSessionTracker() {
        return this.resolverSessions;
    }

    /*************************************************************
     * Set the configuration used for resolution.  This configuration
     * indicates whether requests are processed using a network
     * cache (ala DNS) or if we determine the appropriate server via global.
     *************************************************************/
    public void setConfiguration(Configuration config) {
        this.config = config;
        config.configureResolver(this);
        // Don't update root info on startup, in case updating gets turned off; update around first operation
        // updateRootInfoIfNeeded(config);
    }

    /********************************************************************
     * Set the protocols and the order of preference used for resolution
     * For every server that this resolver talks to, it attempts to
     * communicate via the given protocols either until it succeeds or
     * all attempts to communicate fail.  If a client is behind a firewall
     * and not using a caching server then it would be best to set the
     * preferred protocols to Interface.SP_HDL_TCP and Interface.SP_HDL_HTTP
     * since the Interface.SP_HDL_UDP will probably just get blocked by
     * firewalls and be a big waste of time.
     ********************************************************************/
    public void setPreferredProtocols(int prefProtocols[]) {
        this.preferredProtocols = new int[prefProtocols.length];
        System.arraycopy(prefProtocols, 0, this.preferredProtocols, 0, preferredProtocols.length);
    }

    /** Set the maximum size of the data part of a message before it is
     * split into multiple messages when using UDP. */
    public void setMaxUDPDataSize(int newMaxUDPDataSize) {
        this.maxUDPDataSize = newMaxUDPDataSize;
    }

    /** Return the maximum size of the data part of a message before it is
     * split into multiple messages when using UDP. */
    public int getMaxUDPDataSize() {
        return this.maxUDPDataSize;
    }

    /*************************************************************
     * Get the resolution configuration
     *************************************************************/
    public Configuration getConfiguration() {
        return this.config;
    }

    /*****************************************************************
     * Set how long to wait for responses to TCP and HTTP requests.
     *****************************************************************/
    public void setTcpTimeout(int newTcpTimeout) {
        this.tcpTimeout = newTcpTimeout;
    }

    /*****************************************************************
     * Get how long to wait for responses to TCP requests.
     *****************************************************************/
    public int getTcpTimeout() {
        return this.tcpTimeout;
    }

    public boolean isUseIPv6FastFallback() {
        return useIPv6FastFallback;
    }

    public void setUseIPv6FastFallback(boolean useIPv6FastFallback) {
        this.useIPv6FastFallback = useIPv6FastFallback;
    }

    public SiteFilter getSiteFilter() {
        return siteFilter;
    }

    public void setSiteFilter(SiteFilter siteFilter) {
        this.siteFilter = siteFilter;
    }

    /*******************************************************************
     * Get the array that specifies how long to wait for responses to
     * each UDP request.  The length of this array will indicate how
     * many UDP requests to send before giving up.  The default scheme
     * is something like {1000, 2000, 3000} which is 1 second, 2 seconds,
     * 3 seconds.
     *******************************************************************/
    public int[] getUdpRetryScheme() {
        int urs[] = new int[udpRetryScheme.length];
        System.arraycopy(udpRetryScheme, 0, urs, 0, urs.length);
        return urs;
    }

    /*******************************************************************
     * Set the array that specifies how long to wait for responses to
     * each UDP request.  The length of this array will indicate how
     * many UDP requests to send before giving up.  The default scheme
     * is something like {1000, 2000, 3000} which is 1 second, 2 seconds,
     * 3 seconds.
     *******************************************************************/
    public void setUdpRetryScheme(int[] newudpRetryScheme) {
        udpRetryScheme = null;
        udpRetryScheme = new int[newudpRetryScheme.length];
        System.arraycopy(newudpRetryScheme, 0, udpRetryScheme, 0, udpRetryScheme.length);
    }

    /*************************************************************
     * Set whether or not this object should check the signatures
     * of server responses to certified requests.  The default
     * is to check signatures and throw an exception if a signature
     * to any certified message is missing or invalid.
     *************************************************************/
    public void setCheckSignatures(boolean checkSigs) {
        this.checkSignatures = checkSigs;
    }

    /************************************************************************
     * Locate and return the values of the given handle that have the
     * specified types or indexes.  This method simply creates a
     * ResolutionRequest object from the given parameters and calls
     * processRequest with that request object.  The requested handle
     * values are then extracted from the response and returned (or an
     * exception is thrown if there was an error).
     *
     * Creating your own ResolutionRequest objects and calling processRequest
     * directly allows more flexibility since you can set the certified,
     * authoritative, and recursive flags of the request.
     *
     * When specifying both a set of types or indexes, a server will
     * return all handle values that have the requested types as well
     * as all handle values that have the requested indexes.  Essentially,
     * the union of the set of handles with the requested types and the
     * set of handles with the requested indexes is returned.  An empty
     * index or type list indicates that any index or type is acceptable.
     *
     * The following examples describe how the type and index lists are
     * used in resolutions:
     *<pre>
     *  Type-List       Index List       Returns
     *   [ URL ]         [ 0, 12 ]       Any URL values, as well as values
     *                                    with indexes 0 and 12 if they exist.
     *   [ ]             [ 1 ]           The value with index one only
     *   [ EMAIL ]       [ ]             Any values with type EMAIL
     *   [ ]             [ ]             All of the values associated with the
     *                                    given handle
     *</pre>
     *
     *********************************************************************************/
    public HandleValue[] resolveHandle(String sHandle, String sTypes[], int indexes[]) throws HandleException {
        if (sTypes == null) sTypes = new String[0];
        if (indexes == null) indexes = new int[0];

        // convert the types and handle to UTF8 byte-strings
        byte types[][] = new byte[sTypes.length][];
        byte handle[];

        handle = Util.encodeString(sHandle);
        for (int i = 0; i < sTypes.length; i++) {
            types[i] = Util.encodeString(sTypes[i]);
        }

        return resolveHandle(handle, types, indexes);
    }

    public HandleValue[] resolveHandle(byte[] handle, byte[][] types, int[] indexes) throws HandleException {
        if (types == null) types = new byte[0][];
        if (indexes == null) indexes = new int[0];
        AbstractResponse response = processRequest(new ResolutionRequest(handle, types, indexes, null));

        if (response.responseCode == AbstractMessage.RC_HANDLE_NOT_FOUND) {
            throw new HandleException(HandleException.HANDLE_DOES_NOT_EXIST);
        } else if (response.responseCode == AbstractMessage.RC_VALUES_NOT_FOUND) {
            return new HandleValue[0];
        } else if (response instanceof ErrorResponse) {
            String msg = Util.decodeString(((ErrorResponse) response).message);

            throw new HandleException(HandleException.INTERNAL_ERROR, AbstractMessage.getResponseCodeMessage(response.responseCode) + ": " + msg);
        }

        HandleValue values[] = ((ResolutionResponse) response).getHandleValues();
        if (values == null) return null;
        if (types.length <= 0 && indexes.length <= 0) return values;

        int numValues = values.length;
        for (int i = 0; i < values.length; i++) {
            if ((types.length > 0 && Util.isParentTypeInArray(types, values[i].type)) || (indexes.length > 0 && Util.isInArray(indexes, values[i].index))) continue;
            values[i] = null;
            numValues--;
        }

        if (numValues == values.length) {
            return values;
        }
        HandleValue filteredVals[] = new HandleValue[numValues];
        int j = 0;
        for (HandleValue value : values) {
            if (value != null) filteredVals[j++] = value;
        }
        return filteredVals;
    }

    public HandleValue[] resolveHandle(String sHandle) throws HandleException {
        return this.resolveHandle(sHandle, null, null);
    }

    public HandleValue[] resolveHandle(byte[] handle) throws HandleException {
        return this.resolveHandle(handle, null, null);
    }

    public HandleValue resolveValueReference(ValueReference valueReference) throws HandleException {
        HandleValue[] values = resolveHandle(valueReference.handle, null, new int[] { valueReference.index });
        if (values == null || values.length == 0) return null;
        return values[0];
    }

    public void listHandlesUnderPrefix(String prefixHandle, AuthenticationInfo authInfo, final ScanCallback callback) throws HandleException {
        byte[] prefixHandleBytes = Util.encodeString(prefixHandle);
        SiteInfo[] sites = findLocalSitesForNA(prefixHandleBytes);
        SiteInfo primarySite = Util.getPrimarySite(sites);
        listHandlesUnderPrefixAtSite(prefixHandle, primarySite, authInfo, callback);
    }

    public void listHandlesUnderPrefixAtSite(String prefixHandle, SiteInfo site, AuthenticationInfo authInfo, final ScanCallback callback) throws HandleException {
        byte[] prefixHandleBytes = Util.encodeString(prefixHandle);
        for (ServerInfo server : site.servers) {
            ListHandlesRequest listReq = new ListHandlesRequest(prefixHandleBytes, authInfo);
            sendRequestToServer(listReq, site, server, message -> {
                if (message instanceof ListHandlesResponse) {
                    for (byte[] handle : ((ListHandlesResponse) message).handles) {
                        callback.scanHandle(handle);
                    }
                } else {
                    throw HandleException.ofResponse(message);
                }
            });
        }
    }

    /************************************************************************
     * This method processes the given request using
     * the currently configured method (global resolution, resolution
     * against a caching server, etc), and returns the response.  If
     * a Cache object is available it will be used if the request is
     * a ResolutionRequest and the authoritative flag of the request
     * is not set.
     *
     * The AbstractResponse object that is returned can be either an ErrorResponse
     * or a ResolutionResponse object.  Check the responseCode of the AbstractResponse
     * (or use the instanceof keyword) to determine if the response can safely be casted
     * to a ResolutionResponse or not.  If you determine that the response is a
     * ResolutionResponse then you can cast the response and call getHandleValues() on
     * it.
     *
     * The following is an example that requests all of the URL values associated
     * with the handle 123/abc, and prints them to System.out.
     *
     * <pre>{@code
     * HandleResolver resolver = new HandleResolver();
     * AbstractResponse aResponse = resolver.resolveHandle("123/abc",
     *                                                    new String[]{"URL"},
     *                                                    null);
     * if(aResponse.responseCode==AbstractMessage.RC_SUCCESS) {
     *   ResolutionResponse response = (ResolutionResponse)aResponse;
     *   HandleValue values[] = response.getHandleValues();
     *   System.out.println("Received values: ");
     *   for(int i=0; i<values.length; i++) {
     *     System.out.println(String.valueOf(values[i]));
     *   }
     * }
     * }</pre>
     *
     *
     ************************************************************************/
    public AbstractResponse processRequest(AbstractRequest req, ResponseMessageCallback callback) throws HandleException {
        // need to send request here, based on current configuration
        switch (config.getResolutionMethod()) {
            case Configuration.RM_WITH_CACHE:
                if (!req.isAdminRequest && !req.requiresConnection && req.ignoreRestrictedValues) {
                    // only request that will definitely not result in authentication
                    // should go through local cache/resolver servers
                    SiteInfo cacheSites[] = config.getCacheSites();
                    if (cacheSites != null && cacheSites.length > 0) {
                        return sendRequestToService(req, config.getCacheSites(), true, callback);
                    }
                }
                return processRequestGlobally(req, callback);
            case Configuration.RM_GLOBAL:
            default:
                return processRequestGlobally(req, callback);
        }
    }

    /***********************************************************************
     * Shortcut to processRequest(req, null);
     ***********************************************************************/
    public AbstractResponse processRequest(AbstractRequest req) throws HandleException {
        return processRequest(req, (ResponseMessageCallback) null);
    }

    @Override
    public AbstractResponse processRequest(AbstractRequest req, InetAddress caller) throws HandleException {
        return processRequest(req);
    }

    @Override
    public void processRequest(AbstractRequest req, InetAddress caller, ResponseMessageCallback callback) throws HandleException {
        processRequest(req, callback);
    }

    /**********************************************************************
     * Process the following request (usually a resolution request).
     * First, a message is sent to the global
     * service to find the local service for the prefix.
     * Then that service is queried for the handle.  This bypasses
     * the use of a caching server.
     *
     **********************************************************************/
    private AbstractResponse processRequestGlobally(AbstractRequest req, ResponseMessageCallback callback) throws HandleException {
        return sendRequestToService(req, findLocalSites(req), true, callback);
    }

    /***********************************************************************
     * Shortcut to processRequestGlobally(req, null);
     ***********************************************************************/
    public AbstractResponse processRequestGlobally(AbstractRequest req) throws HandleException {
        return processRequestGlobally(req, null);
    }

    /** Information about a prefix. */
    private class ServiceInfo {
        /** Sites from HS_SITE.PREFIX values at the prefix handle */
        SiteInfo[] prefixSites;
        /** Sites from HS_SITE and HS_SITE.6 values at the prefix handle */
        SiteInfo[] sites;
        /** The HS_NAMESPACE stored in this prefix handle.  Does not store hierarchical namespace info but only that at this handle. */
        NamespaceInfo ns;
        /** The response returned by resolving the prefix handle (used in case of errors) */
        AbstractResponse response;
        /** The values at the prefix handle (unused) */
        @SuppressWarnings("unused")
        HandleValue[] values;
    }

    /**
     * Populates a ServiceInfo with global information.
     *
     * @param handle the handle expected to be in global, used to look up configured local sites (to use instead of global sites).
     * @return the input ServiceInfo
     */
    private ServiceInfo globalServiceInfo(byte[] handle) {
        ServiceInfo res = new ServiceInfo();
        res.response = null;
        res.sites = config.getLocalSites(handle);
        if (res.sites == null) res.sites = getGlobalSites();
        res.ns = config.getGlobalNamespace();
        res.values = config.getGlobalValues();
        return res;
    }

    private SiteInfo[] getGlobalSites() {
        SiteInfo[] res = config.getLocalSites(Common.ROOT_HANDLE);
        if (res != null) return res;
        return config.getGlobalSites();
    }

    /** Populate a ServiceInfo with the information for a individual ResolutionRequest for a prefix, optionally sent to a particular sites.
     *
     * @param resReq a ResolutionRequest to a prefix
     * @param sites the sites to process the request; if null, the GHR is used.
     * @param service the ServiceInfo to populate.
     * @param forceResolution if true, resolve (for HS_NAMESPACE info) even if local sites are configured
     * @throws HandleException
     */
    private void getServiceInfoForNA(ResolutionRequest resReq, SiteInfo[] sites, ServiceInfo service, boolean forceResolution, boolean findPrefixReferralSites) throws HandleException {
        resReq.clearBuffers();
        service.response = null;
        service.sites = null;
        service.ns = null;
        service.values = null;

        // Check for explicit local sites in configuration
        // TODO: include HS_NAMESPACE in localSites
        SiteInfo[] localSites = config.getLocalSites(resReq.handle);
        if (localSites != null) {
            service.sites = localSites;
            if (!forceResolution) return;
        }

        if (resReq.recursionCount >= recursionCountLimit) {
            throw new HandleException(HandleException.SERVICE_NOT_FOUND, "Encountered recursion limit looking for service for handle " + Util.decodeString(resReq.handle));
        }

        // the 'true' indicates to cache the result
        if (sites == null) sites = getGlobalSites();
        service.response = sendRequestToService(resReq, sites, true, null);

        if (service.response.responseCode == AbstractMessage.RC_SUCCESS) {
            HandleValue[] values = ((ResolutionResponse) service.response).getHandleValues();
            service.values = values;

            // extract any namespace information.
            service.ns = Util.getNamespaceFromValues(Util.decodeString(resReq.handle), values);
            if (service.sites == null) {
                // skip getting sites if already have local sites
                populateServiceInfoSites(resReq, service, values, findPrefixReferralSites);
            }
        }
    }

    private SiteInfo[] getSitesFromReferralValues(HandleValue[] values, boolean findPrefixReferralSites, AbstractRequest origRequest) throws HandleException {
        if (values == null || values.length == 0) return null;
        ResolutionRequest resReq = new ResolutionRequest(Common.BLANK_HANDLE, findPrefixReferralSites ? Common.SITE_INFO_AND_SERVICE_HANDLE_INCL_PREFIX_TYPES : Common.SITE_INFO_AND_SERVICE_HANDLE_TYPES, null, null);
        resReq.authoritative = false; // don't require authoritative resolution for NA and siteinfo
        resReq.sessionInfo = null; // don't use session for NA resolution
        resReq.sessionTracker = null; // don't use session for NA resolution
        resReq.encrypt = false; // don't use session for NA resolution
        resReq.requestedTypes = findPrefixReferralSites ? Common.SITE_INFO_AND_SERVICE_HANDLE_INCL_PREFIX_TYPES : Common.SITE_INFO_AND_SERVICE_HANDLE_TYPES;
        resReq.recursionCount = origRequest.recursionCount;
        ServiceInfo service = new ServiceInfo();
        populateServiceInfoSites(resReq, service, values, findPrefixReferralSites);
        origRequest.recursionCount = resReq.recursionCount;
        if (findPrefixReferralSites && service.prefixSites != null && service.prefixSites.length >= 0) return service.prefixSites;
        else return service.sites;
    }

    private void populateServiceInfoSites(ResolutionRequest resReq, ServiceInfo service, HandleValue[] values, boolean findPrefixReferralSites) throws HandleException {
        Set<SiteInfo> sites = new LinkedHashSet<>();
        Set<SiteInfo> prefixSites = findPrefixReferralSites ? new LinkedHashSet<>() : null;
        Set<String> visitedHandles = new HashSet<>();
        visitedHandles.add(Util.decodeString(resReq.handle));
        HandleException exception = populateServiceInfoSitesHelper(resReq, values, visitedHandles, sites, prefixSites, findPrefixReferralSites, false, 0);
        SiteInfo[] siteArray = sites.isEmpty() ? null : sites.toArray(new SiteInfo[sites.size()]);
        SiteInfo[] prefixSiteArray = (prefixSites == null || prefixSites.isEmpty()) ? null : prefixSites.toArray(new SiteInfo[prefixSites.size()]);
        service.prefixSites = prefixSiteArray;
        service.sites = siteArray;
        if (service.sites == null && (!findPrefixReferralSites || service.prefixSites == null) && exception != null) throw exception;
    }

    private HandleException populateServiceInfoSitesHelper(ResolutionRequest resReq, HandleValue[] values, Set<String> visitedHandles, Set<SiteInfo> sites, Set<SiteInfo> prefixSites, boolean findPrefixReferralSites, boolean prefixOnly,
                                                           int depth) {
        if (!prefixOnly) {
            SiteInfo[] siteArray = Util.getSitesAndAltSitesFromValues(values, Common.SITE_INFO_TYPES);
            if (siteArray != null) {
                for (SiteInfo site : siteArray) sites.add(site);
            }
        }
        if (findPrefixReferralSites) {
            SiteInfo[] prefixSiteArray = Util.getSitesAndAltSitesFromValues(values, Common.DERIVED_PREFIX_SITE_INFO_TYPES);
            if (prefixSiteArray != null) {
                for (SiteInfo site : prefixSiteArray) prefixSites.add(site);
            }
        }
        if (depth >= BootstrapHandles.MAX_DEPTH) return null;
        // extract, resolve and return any SiteInfo records referenced by
        // service handles.
        HandleException exception = null;
        if (!prefixOnly) {
            List<byte[]> serviceHandles = getServiceHandlesFromValues(values, false);
            exception = processServiceHandles(resReq, visitedHandles, sites, prefixSites, false, depth, serviceHandles);
        }
        if (findPrefixReferralSites) {
            List<byte[]> serviceHandles = getServiceHandlesFromValues(values, true);
            HandleException otherEx = processServiceHandles(resReq, visitedHandles, sites, prefixSites, true, depth, serviceHandles);
            if (exception == null) exception = otherEx;
        }
        return exception;
    }

    private HandleException processServiceHandles(ResolutionRequest resReq, Set<String> visitedHandles, Set<SiteInfo> sites, Set<SiteInfo> prefixSites, boolean isPrefixServiceHandle, int depth, List<byte[]> serviceHandles) {
        HandleException exception = null;
        if (serviceHandles != null) {
            for (byte[] serviceHandle : serviceHandles) {
                String serviceHandleString = Util.decodeString(serviceHandle);
                if (!visitedHandles.add(serviceHandleString)) continue;
                if (Util.equalsCI(Common.ROOT_HANDLE, serviceHandle)) {
                    // HS_SERV 0.NA/0.NA means use the global sites
                    for (SiteInfo site : getGlobalSites()) {
                        if (isPrefixServiceHandle) prefixSites.add(site);
                        else sites.add(site);
                    }
                    continue;
                }
                try {
                    // resolve the service handle
                    resReq.recursionCount++;
                    if (resReq.recursionCount >= recursionCountLimit) throw new HandleException(HandleException.SERVICE_REFERRAL_ERROR, "Recursion limit exceeded on service lookup of " + Util.decodeString(serviceHandle));
                    ResolutionRequest svcReq = new ResolutionRequest(serviceHandle, isPrefixServiceHandle ? Common.DERIVED_PREFIX_SITE_AND_SERVICE_HANDLE_TYPES : Common.SITE_INFO_AND_SERVICE_HANDLE_TYPES, null, null);
                    svcReq.takeValuesFrom(resReq);
                    svcReq.authoritative = false; // don't require authoritative resolution for NA and siteinfo
                    AbstractResponse svcRes = processRequest(svcReq);
                    if (svcRes.responseCode == AbstractMessage.RC_SUCCESS) {
                        // note:  namespace information in service handles are purposefully ignored
                        HandleValue[] serviceHandleValues = ((ResolutionResponse) svcRes).getHandleValues();
                        HandleException subEx = populateServiceInfoSitesHelper(resReq, serviceHandleValues, visitedHandles, sites, prefixSites, isPrefixServiceHandle, isPrefixServiceHandle, depth + 1);
                        if (exception == null) exception = subEx;
                    }
                } catch (HandleException e) {
                    if (exception == null) exception = e;
                }
            }
        }
        return exception;
    }

    private static List<byte[]> getServiceHandlesFromValues(HandleValue[] values, boolean findPrefixReferralSites) {
        List<byte[]> result = null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!findPrefixReferralSites && Util.equals(values[i].type, Common.SERVICE_HANDLE_TYPE)) {
                if (result == null) result = new ArrayList<>();
                result.add(values[i].data);
            } else if (findPrefixReferralSites && Util.equals(values[i].type, Common.DERIVED_PREFIX_SERVICE_HANDLE_TYPE)) {
                if (result == null) result = new ArrayList<>();
                result.add(values[i].data);
            }
        }
        return result;
    }

    public SiteInfo[] findLocalSitesForNA(byte[] naHandle) throws HandleException {
        return findLocalSitesForNA(naHandle, null);
    }

    private SiteInfo[] findLocalSitesForNA(byte[] naHandle, AbstractRequest origRequest) throws HandleException {
        ResolutionRequest resReq = new ResolutionRequest(naHandle, Common.SITE_INFO_AND_SERVICE_HANDLE_TYPES, null, null);
        resReq.authoritative = false; // don't require authoritative resolution for NA and siteinfo
        resReq.sessionInfo = null; // don't use session for NA resolution
        resReq.sessionTracker = null; // don't use session for NA resolution
        resReq.encrypt = false; // don't use session for NA resolution
        resReq.requestedTypes = Common.SITE_INFO_AND_SERVICE_HANDLE_TYPES;
        resReq.recursionCount = origRequest == null ? 0 : origRequest.recursionCount;
        ServiceInfo service = new ServiceInfo();
        getServiceInfoForNA(resReq, findLocalSites(resReq), service, false, false);
        if (origRequest != null) origRequest.recursionCount = resReq.recursionCount;
        return service.sites;
    }

    private SiteInfo[] findPrefixReferralSitesForNA(byte[] naHandle, AbstractRequest origRequest) throws HandleException {
        ResolutionRequest resReq = new ResolutionRequest(naHandle, Common.SITE_INFO_AND_SERVICE_HANDLE_INCL_PREFIX_TYPES, null, null);
        resReq.authoritative = false; // don't require authoritative resolution for NA and siteinfo
        resReq.sessionInfo = null; // don't use session for NA resolution
        resReq.sessionTracker = null; // don't use session for NA resolution
        resReq.encrypt = false; // don't use session for NA resolution
        resReq.requestedTypes = Common.SITE_INFO_AND_SERVICE_HANDLE_INCL_PREFIX_TYPES;
        resReq.recursionCount = origRequest == null ? 0 : origRequest.recursionCount;
        ServiceInfo service = new ServiceInfo();
        getServiceInfoForNA(resReq, findLocalSites(resReq), service, false, true);
        if (origRequest != null) origRequest.recursionCount = resReq.recursionCount;
        return service.prefixSites;
    }

    /**
     * Get the information for the service that is responsible for
     * this handle while at the same time populating the namespace in the request.
     *
     * @param req the request
     * @param forceResolution if true, resolve for namespace information even if local sites found in resolver config.
     * @throws HandleException
     */
    private ServiceInfo getServiceInfo(AbstractRequest req, boolean forceResolution) throws HandleException {
        // if the handle is under the global prefix (0) then it gets resolved by the global service
        if (Util.startsWithCI(req.handle, Common.GLOBAL_NA_PREFIX) || Util.startsWithCI(req.handle, Common.GLOBAL_NA) || !Util.hasSlash(req.handle)) {
            req.setNamespace(config.getGlobalNamespace());
            return globalServiceInfo(Util.getZeroNAHandle(req.handle));
        }

        // start by adding the namespace of the global service, since that is our starting point
        req.setNamespace(config.getGlobalNamespace());

        ServiceInfo service = new ServiceInfo();
        ResolutionRequest resReq = buildPrefixResolutionRequest(req);
        getServiceInfoForNA(resReq, null, service, forceResolution, false);
        if (service.sites != null) {
            if (service.ns != null) req.setNamespace(service.ns);
        } else {
            tryAuthGlobalServiceLookupAndThrowExceptionOnFailure(req, resReq, service);
        }
        return service;
    }

    private ResolutionRequest buildPrefixResolutionRequest(AbstractRequest req) {
        // We look for admin types to facilitate servers wanting to find the right admin for a create handle
        ResolutionRequest resReq = new ResolutionRequest(Util.getZeroNAHandle(req.handle), Common.SITE_INFO_AND_SERVICE_HANDLE_AND_NAMESPACE_TYPES, null, null);
        resReq.takeValuesFrom(req);
        resReq.ignoreRestrictedValues = true; // NA resolution is public (otherwise resReq needs auth info)
        resReq.authoritative = false; // don't require authoritative resolution for NA and siteinfo
        resReq.sessionInfo = null; // don't use session for NA resolution
        resReq.sessionTracker = null; // don't use session for NA resolution
        resReq.encrypt = false; // don't use session for NA resolution
        resReq.recursionCount = (short) (req.recursionCount + 1);
        resReq.requestedTypes = Common.SITE_INFO_AND_SERVICE_HANDLE_AND_NAMESPACE_TYPES;
        return resReq;
    }

    /**
     * Try a last authoritative lookup of the whole prefix, in case it was just created
     */
    private void tryAuthGlobalServiceLookupAndThrowExceptionOnFailure(AbstractRequest req, ResolutionRequest resReq, ServiceInfo service) throws HandleException {
        resReq.authoritative = true;
        resReq.handle = Util.getZeroNAHandle(req.handle);
        getServiceInfoForNA(resReq, null, service, false, false);
        if (service.ns != null) req.setNamespace(service.ns);
        if (service.sites == null) {
            throw new HandleException(HandleException.SERVICE_NOT_FOUND, "Unable to find service for prefix " + Util.decodeString(resReq.handle) + "; prefix resolution response: " + service.response);
        }
    }

    /**********************************************************************
     * Get the site information for the service that is responsible for
     * this handle while at the same time populating the namespace
     **********************************************************************/
    public SiteInfo[] findLocalSites(AbstractRequest req) throws HandleException {
        return getServiceInfo(req, false).sites;
    }

    /**********************************************************************
     * Find the prefix handle for a handle
     * @deprecated  Legacy of slash-based delegation; use Util.getZeroNAHandle
     **********************************************************************/
    @Deprecated
    public byte[] getNAHandle(byte[] handle) {
        return Util.getZeroNAHandle(handle);
    }

    @Deprecated
    public byte[] getNAHandle(ResolutionRequest resReq) {
        return Util.getZeroNAHandle(resReq.handle);
    }

    /* legacy usage of term "namespace" for template handles */
    public NamespaceInfo getNamespaceInfo(ResolutionRequest resReq) throws HandleException {
        return getServiceInfo(resReq, true).ns;
    }

    public final AbstractResponse sendRequestToService(AbstractRequest req, SiteInfo sites[], ResponseMessageCallback callback) throws HandleException {
        return sendRequestToService(req, sites, false, callback);
    }

    /***********************************************************************
     * Shortcut to sendRequestToService(AbstractRequest, SiteInfo[], null);
     ***********************************************************************/
    public AbstractResponse sendRequestToService(AbstractRequest req, SiteInfo sites[]) throws HandleException {
        return sendRequestToService(req, sites, null);
    }

    /**********************************************************************
     * Send the specified request to the given service and return
     * the result.  If the given request is a ResolutionRequest the
     * cache is checked for the requested values before the message
     * is sent.  If cacheResult is true and we receive a resolution
     * response, then the result is stored in the cache for later use.
     * The cacheResult parameter is supplied so that calling the public
     * method sendRequestToService doesn't cache results.  This is important
     * because if the wrong service is queried for a handle, we don't want
     * to cache the result because it could affect other queries.
     **********************************************************************/
    private AbstractResponse sendRequestToService(AbstractRequest req, SiteInfo sites[], boolean cacheResult, ResponseMessageCallback callback) throws HandleException {
        if (sites == null) throw new HandleException(HandleException.SERVICE_NOT_FOUND, "No sites found");
        boolean isCacheable = cacheResult && req.opCode == AbstractMessage.OC_RESOLUTION;
        SiteInfo ipv6Sites[] = null;
        SiteInfo ipv4Sites[] = null;

        // use the regular or certified cache, determined by the certify request flag
        @SuppressWarnings("hiding")
        Cache cache = req.certify ? this.secureCache : this.cache;

        //  check cache for resolution requests
        if (cache != null && req.opCode == AbstractMessage.OC_RESOLUTION) {
            AbstractResponse resp = resolveFromCache(req, cache);

            if (resp != null) {
                if (callback != null) {
                    callback.handleResponse(resp);
                }
                return resp;
            }
        } else if (req.opCode == AbstractMessage.OC_ADD_VALUE || req.opCode == AbstractMessage.OC_CREATE_HANDLE || req.opCode == AbstractMessage.OC_DELETE_HANDLE || req.opCode == AbstractMessage.OC_MODIFY_VALUE
                || req.opCode == AbstractMessage.OC_REMOVE_VALUE) {
            try {
                if (this.cache != null) this.cache.removeHandle(Util.upperCasePrefix(req.handle));
                if (this.secureCache != null && this.secureCache != this.cache) this.secureCache.removeHandle(Util.upperCasePrefix(req.handle));
            } catch (Throwable e) {
                System.err.println("Cache remove error: " + e);
                e.printStackTrace(System.err);
            }
        }

        // if the request is an admin message or is authoritative,
        // it should only be sent to a primary site.
        sites = filterSitesForRequest(sites, req);
        sites = adjustSitesForDomains(sites);

        ipv6Sites = getIpSites(sites, IP_VERSION_6);
        ipv4Sites = getIpSites(sites, IP_VERSION_4);

        // set response time value in sites
        // and check to see if there is a particular primary to use if there are multiple primary sites
        SiteInfo preferredPrimary = null;
        int primaries = 0;

        if (req.isAdminRequest || req.authoritative) {
            primaries = getNumPrimaries(sites);
            preferredPrimary = getPreferredPrimary(sites);
        }

        // reorder sites based on performance
        setResponseTimesOfSites(sites);
        ipv6Sites = Util.orderSitesByPreference(ipv6Sites);
        ipv4Sites = Util.orderSitesByPreference(ipv4Sites);

        HappyEyeballsResolver resolver6 = null;
        HappyEyeballsResolver resolver4 = null;
        Future<?> alternateThreadFuture = null;

        boolean preferIPv4Stack = Boolean.parseBoolean(System.getProperty("java.net.preferIPv4Stack"));

        AbstractResponse resp = null;
        try {
            // clean up any previous use of req for Happy Eyeballs
            req.multithread = false;
            req.completed.set(false);
            req.socketRef.set(null);

            if (preferIPv4Stack || !hasIPv6Interface || ipv6Sites.length == 0) {
                // Do not delay IPv4 resolution if there is no IPv6 interface
                // or the target service does not have IPv6 sites.
                resolver4 = new HappyEyeballsResolver(this, ipv4Sites, req, callback, primaries, preferredPrimary, 0, false);

                resolver6 = new HappyEyeballsResolver();

                resolver4.run();
            } else if (!hasIPv4Interface || ipv4Sites.length == 0) {
                resolver6 = new HappyEyeballsResolver(this, ipv6Sites, req, callback, primaries, preferredPrimary, 0, false);
                resolver4 = new HappyEyeballsResolver();

                resolver6.run();
            } else if (useIPv6FastFallback) {
                req.multithread = true;

                // The client has IPv6 and there is at least one IPv6 site.
                resolver6 = new HappyEyeballsResolver(this, ipv6Sites, req.clone(), callback, primaries, preferredPrimary, 0, false);
                resolver4 = new HappyEyeballsResolver(this, ipv4Sites, req.clone(), callback, primaries, preferredPrimary, 300, preferredPrimary != null);

                resolver6.siblingResolver = resolver4;
                resolver4.siblingResolver = resolver6;

                alternateThreadFuture = getExecutorService().submit(resolver4);
                resolver6.run();
            } else {
                SiteInfo[] allSites6First = new SiteInfo[ipv6Sites.length + ipv4Sites.length];
                System.arraycopy(ipv6Sites, 0, allSites6First, 0, ipv6Sites.length);
                System.arraycopy(ipv4Sites, 0, allSites6First, ipv6Sites.length, ipv4Sites.length);
                resolver6 = new HappyEyeballsResolver(this, allSites6First, req, callback, primaries, preferredPrimary, 0, false);
                resolver4 = new HappyEyeballsResolver();

                resolver6.run();
            }

            // Wait until both threads have finished their business.
            if (alternateThreadFuture != null) {
                try {
                    if (!alternateThreadFuture.isDone()) {
                        if (resolver6.resp != null) alternateThreadFuture.cancel(true);
                        else alternateThreadFuture.get();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                    if (cause instanceof Error) throw (Error) cause;
                    throw new AssertionError(cause);
                } catch (CancellationException e) {
                    // ignore
                }
            }

            if (resolver4.resp != null) {
                resp = resolver4.resp;
                if (req.multithread) req.takeValuesFromRequestActuallyUsed(resolver4.req);
            } else if (resolver6.resp != null) {
                resp = resolver6.resp;
                if (req.multithread) req.takeValuesFromRequestActuallyUsed(resolver6.req);
            }

            if (resp == null) {
                if (resolver4.publicException != null) {
                    if (req.multithread) {
                        req.takeValuesFromRequestActuallyUsed(resolver4.req);
                        throw new HandleException(resolver4.publicException.getCode(), resolver4.publicException.getMessage(), resolver4.publicException);
                    }
                    throw resolver4.publicException;
                } else if (resolver6.publicException != null) {
                    if (req.multithread) req.takeValuesFromRequestActuallyUsed(resolver6.req);
                    throw resolver6.publicException;
                }

                throw new HandleException(HandleException.NO_ACCEPTABLE_INTERFACES, "Cannot contact an acceptable interface");
            }
        } finally {
            // clean up any previous use of req for Happy Eyeballs
            req.multithread = false;
            req.completed.set(false);
            req.socketRef.set(null);
        }

        // only do if cacheResult is true, to avoid doing this when sending requests to a specific service intentionally
        if (cacheResult && resp.responseCode == AbstractMessage.RC_SERVICE_REFERRAL) {
            req = newRequestForReferral(req);
            req.recursionCount++;
            if (req.recursionCount >= recursionCountLimit) throw new HandleException(HandleException.SERVICE_REFERRAL_ERROR, "Recursion limit exceeded on service referral for " + Util.decodeString(req.handle));
            ServiceReferralResponse srresp = (ServiceReferralResponse) resp;
            SiteInfo[] referralSites = getSitesFromReferralValues(srresp.getHandleValues(), false, req);
            if (referralSites == null && srresp.handle.length > 0) {
                referralSites = findLocalSitesForNA(srresp.handle, req);
            }
            if (referralSites == null) throw new HandleException(HandleException.SERVICE_REFERRAL_ERROR, "Unable to find sites for service referral");
            return sendRequestToService(req, referralSites, cacheResult, callback);
        }
        if (cacheResult && (Util.startsWithCI(req.handle, Common.NA_HANDLE_PREFIX) || !Util.hasSlash(req.handle)) && resp.responseCode == AbstractMessage.RC_PREFIX_REFERRAL) {
            req = newRequestForReferral(req);
            req.recursionCount++;
            if (req.recursionCount >= recursionCountLimit) throw new HandleException(HandleException.SERVICE_REFERRAL_ERROR, "Recursion limit exceeded on prefix referral for " + Util.decodeString(req.handle));
            ServiceReferralResponse srresp = (ServiceReferralResponse) resp;
            HandleValue[] values = srresp.getHandleValues();
            SiteInfo[] prefixSites = getSitesFromReferralValues(values, true, req);
            if (prefixSites == null && srresp.handle.length > 0) {
                prefixSites = findPrefixReferralSitesForNA(srresp.handle, req);
            }
            if (prefixSites == null) throw new HandleException(HandleException.SERVICE_REFERRAL_ERROR, "Unable to find sites for prefix referral");
            return sendRequestToService(req, prefixSites, cacheResult, callback);
        }

        cacheResponse(resp, req, isCacheable, cache);

        // When a global server is retired, it should return
        // RC_OUT_OF_DATE_SITE_INFO for a while.  This will cause clients to
        // upgrade their root_info.
        //
        // Note: A less disruptive way to cause clients to update the root_info
        // is to increment the serial numbers of one of the global siteinfo records.
        if (resp.responseCode == AbstractMessage.RC_OUT_OF_DATE_SITE_INFO) config.notifyRootInfoOutdated(this);
        return resp;
    }

    private AbstractRequest newRequestForReferral(AbstractRequest req) {
        AbstractRequest result = req.clone();
        req.clearBuffers();
        result.sessionInfo = null;
        return result;
    }

    private SiteInfo[] filterSitesForRequest(SiteInfo[] sites, AbstractRequest req) {
        if (sites == null) return null;
        if (siteFilter == null) return sites;
        List<SiteInfo> filteredSites = new ArrayList<>(sites.length);
        for (SiteInfo site : sites) {
            if (siteFilter.apply(site)) filteredSites.add(site);
        }
        if (filteredSites.size() == sites.length || filteredSites.isEmpty()) return sites;
        SiteInfo[] res = filteredSites.toArray(new SiteInfo[filteredSites.size()]);
        if ((req.isAdminRequest || req.authoritative) && getNumPrimaries(res) == 0) {
            for (SiteInfo site : sites) {
                if (site.isPrimary) filteredSites.add(site);
            }
            res = filteredSites.toArray(new SiteInfo[filteredSites.size()]);
        }
        return res;
    }

    private static SiteInfo[] getIpSites(SiteInfo sites[], short protocol) {
        int arraySize = 0;
        int sitesFound = 0;
        SiteInfo matchingSites[] = null;
        if (sites == null) {
            return null;
        }

        for (SiteInfo site : sites) {
            boolean ipv6Site = false;
            for (ServerInfo server : site.servers) {
                if (!server.isIPv4()) {
                    ipv6Site = true;
                    break;
                }
            }
            if ((ipv6Site && protocol == IP_VERSION_6) || (!ipv6Site && protocol == IP_VERSION_4)) {
                arraySize += 1;
            }
        }
        if (arraySize == 0) {
            return null;
        }
        matchingSites = new SiteInfo[arraySize];
        for (SiteInfo site : sites) {
            boolean ipv6Site = false;
            for (ServerInfo server : site.servers) {
                if (!server.isIPv4()) {
                    ipv6Site = true;
                    break;
                }
            }
            if ((ipv6Site && protocol == IP_VERSION_6) || (!ipv6Site && protocol == IP_VERSION_4)) {
                matchingSites[sitesFound] = site;
                sitesFound++;
                if (sitesFound == arraySize) break;
            }
        }
        return matchingSites;
    }

    private AbstractResponse resolveFromCache(AbstractRequest req, @SuppressWarnings("hiding") Cache cache) {

        byte handle[] = null;
        byte types[][] = null;
        int indexes[] = null;
        ResolutionRequest rreq = (ResolutionRequest) req;
        handle = new byte[rreq.handle.length];
        System.arraycopy(rreq.handle, 0, handle, 0, rreq.handle.length);
        Util.upperCasePrefixInPlace(handle);
        types = rreq.requestedTypes;
        indexes = rreq.requestedIndexes;

        if (!req.authoritative && req.ignoreRestrictedValues) {
            // if the request has the authoritative flag, or is for
            // restricted values, bypass the cache
            try {
                AbstractResponse resp = null;
                byte cachedVals[][] = cache.getCachedValues(handle, types, indexes);
                if (cachedVals != null) {
                    if (cache.isCachedNotFound(cachedVals)) {
                        resp = new ErrorResponse(req, AbstractMessage.RC_HANDLE_NOT_FOUND, null);
                    } else if (cachedVals.length == 0) {
                        resp = new ErrorResponse(req, AbstractMessage.RC_VALUES_NOT_FOUND, null);
                    } else resp = new ResolutionResponse(req, req.handle, cachedVals);
                }

                if (resp == null && secureCache != null && secureCache != cache) {
                    cachedVals = secureCache.getCachedValues(handle, types, indexes);
                    if (cachedVals != null) {
                        if (secureCache.isCachedNotFound(cachedVals)) {
                            resp = new ErrorResponse(req, AbstractMessage.RC_HANDLE_NOT_FOUND, null);
                        } else if (cachedVals.length == 0) {
                            resp = new ErrorResponse(req, AbstractMessage.RC_VALUES_NOT_FOUND, null);
                        } else resp = new ResolutionResponse(req, req.handle, cachedVals);
                    }
                }
                return resp;
            } catch (Throwable e) {
                System.err.println("Cache get error: " + e);
                e.printStackTrace(System.err);
                return null;
            }
        }

        return null;
    }

    private static int getNumPrimaries(SiteInfo sites[]) {
        int primaries = 0;
        for (int i = 0; sites != null && i < sites.length; i++) {
            if (sites[i].isPrimary) {
                primaries++;
            }
        }

        return primaries;
    }

    private void setResponseTimesOfSites(SiteInfo sites[]) {
        for (int i = 0; sites != null && i < sites.length; i++) {
            if (sites[i].servers == null || sites[i].servers.length == 0) {
                continue;
            }

            String ip = sites[i].servers[0].getAddressString();
            Long cachedResponseTime = responseTimeTbl.get(ip);

            if (cachedResponseTime == null) {
                sites[i].responseTime = 0;
            } else {
                sites[i].responseTime = cachedResponseTime.longValue();
            }
        }
    }

    private SiteInfo getPreferredPrimary(SiteInfo sites[]) {
        SiteInfo preferredPrimary = null;
        for (int i = 0; sites != null && i < sites.length; i++) {
            if (sites[i].isPrimary) {
                if (sites[i].servers == null || sites[i].servers.length == 0) {
                    continue;
                }
                String ip = sites[i].servers[0].getAddressString();
                if (sites[i].servers[0].interfaces != null && sites[i].servers[0].interfaces.length > 0) {
                    String ipPort = ip + ":" + sites[i].servers[0].interfaces[0].port;
                    Long timeOfLastRequest = preferredPrimaryTbl.get(ipPort);
                    if (timeOfLastRequest != null) {

                        long now = System.currentTimeMillis();

                        if (now - timeOfLastRequest > USE_SAME_PRIMARY_MILLIS) {
                            preferredPrimaryTbl.remove(ipPort);
                        } else {
                            if (preferredPrimary == null) {
                                preferredPrimary = sites[i];
                            }
                        }
                    }
                }
            }
        }

        return preferredPrimary;
    }

    private void cacheResponse(AbstractResponse resp, AbstractRequest req, boolean isCacheable, @SuppressWarnings("hiding") Cache cache) {
        byte handle[] = null;
        byte types[][] = null;
        int indexes[] = null;

        if (req instanceof ResolutionRequest) {
            ResolutionRequest rreq = (ResolutionRequest) req;

            handle = new byte[rreq.handle.length];
            System.arraycopy(rreq.handle, 0, handle, 0, rreq.handle.length);
            Util.upperCasePrefixInPlace(handle);

            types = rreq.requestedTypes;
            indexes = rreq.requestedIndexes;
        }

        if (isCacheable && cache == null) {
            // cache certified responses to both caches
            if (cache != this.cache) {
                cache = this.cache;
            } else {
                isCacheable = false;
            }
        }

        if (isCacheable) {
            if ((resp.responseCode == AbstractMessage.RC_SUCCESS) && (resp instanceof ResolutionResponse)) {
                cacheSuccessfulResolutionResponse(resp, req, handle, types, indexes);
            }
            if (resp.responseCode == AbstractMessage.RC_VALUES_NOT_FOUND) {
                cacheValueNotFound(req, handle, types, indexes);
            }
            if ((resp.responseCode == AbstractMessage.RC_HANDLE_NOT_FOUND) && notFoundCacheTTL > 0) {
                cacheHandleNotFound(req, handle);
            }
        }
    }

    private void cacheSuccessfulResolutionResponse(AbstractResponse resp, AbstractRequest req, byte handle[], byte types[][], int indexes[]) {
        // Only cache publicly readable values.
        try {
            HandleValue[] origVals = ((ResolutionResponse) resp).getHandleValues();
            HandleValue[] vals = null;

            if (req.ignoreRestrictedValues) vals = origVals;

            else if (origVals != null) {
                int numPub = 0;
                for (HandleValue origVal : origVals) {
                    if (origVal.getAnyoneCanRead()) numPub++;
                }
                vals = new HandleValue[numPub];
                int j = 0;
                for (HandleValue origVal : origVals) {
                    if (j >= numPub) break;
                    if (origVal.getAnyoneCanRead()) vals[j++] = origVal;
                }
            }
            cache.setCachedValues(handle, vals, types, indexes);
            if (req.certify && (Util.equalsCI(handle, Common.ROOT_HANDLE) || Util.equalsCI(handle, Common.TRUST_ROOT_HANDLE)) && types == null && indexes == null) {
                config.checkRootInfoUpToDate(this, Util.decodeString(handle), vals);
            }
            if (req.certify && this.secureCache != null && this.secureCache != cache) {
                // Also update the non-secure code.
                this.secureCache.setCachedValues(handle, vals, types, indexes);
            }
        } catch (Throwable e) {
            System.err.println("Cache set error: " + e);
            e.printStackTrace(System.err);
        }
    }

    private void cacheValueNotFound(AbstractRequest req, byte handle[], byte types[][], int indexes[]) {
        try {
            cache.setCachedValues(handle, new HandleValue[0], types, indexes);
            if (req.certify && this.secureCache != null && this.secureCache != cache) {
                this.secureCache.setCachedValues(handle, new HandleValue[0], types, indexes);
            }
        } catch (Throwable e) {
            System.err.println("Cache set error: " + e);
            e.printStackTrace(System.err);
        }

    }

    private void cacheHandleNotFound(AbstractRequest req, byte handle[]) {
        try {
            if (cache != null) {
                cache.setCachedNotFound(handle, notFoundCacheTTL);
            }
            if (req.certify && this.secureCache != null && this.secureCache != cache) {
                // Also update the non-secure cache.
                this.secureCache.setCachedNotFound(handle, notFoundCacheTTL);
            }
        } catch (Throwable e) {
            System.err.println("Cache set error: " + e);
            e.printStackTrace(System.err);
        }
    }

    /**
     * Shortcut to sendRequestToSite(AbstractRequest, site, protocol, null);
     */
    public AbstractResponse sendRequestToSite(AbstractRequest req, SiteInfo site, int protocol) throws HandleException {
        return sendRequestToSite(req, site, protocol, null);
    }

    /*****************************************************************************
     */

    public AbstractResponse sendRequestToSite(AbstractRequest req, SiteInfo site, int protocol, ResponseMessageCallback callback) throws HandleException {
        return sendRequestToServerInSiteByProtocol(req, site, null, protocol, callback);
    }

    public AbstractResponse sendRequestToServerInSiteByProtocol(AbstractRequest req, SiteInfo site, ServerInfo server, int protocol, ResponseMessageCallback callback) throws HandleException {
        req.siteInfoSerial = site.serialNumber;

        // check if we're communicating with an older server.
        // If so, use its older protocol version

        req.setSupportedProtocolVersion(site);

        long time1 = System.currentTimeMillis();

        AbstractResponse response;

        try {
            int which = site.determineServerNum(req.handle);
            if (server == null) server = site.determineServer(req.handle);
            String domain = site.getAttributeForServer(SiteInfo.DOMAIN, which);
            String path = site.getAttributeForServer(SiteInfo.PATH, which);
            server = adjustServerIpAddressForDomain(server, domain);

            response = sendRequestToServerByProtocol(req, domain, path, server, protocol, callback);

            if (site.isRoot && response != null && (response.siteInfoSerial > req.siteInfoSerial)) {
                this.config.notifyRootInfoOutdated(this);
            }

            long time2 = System.currentTimeMillis();
            site.responseTime = time2 - time1;
            responseTimeTbl.put(site.servers[0].getAddressString(), Long.valueOf(time2 - time1));
        } catch (HandleException e) {
            // connection errors mean we will prefer other sites.
            // set its response time as being on the order of the tcp timeout
            if (e.getCode() == HandleException.CANNOT_CONNECT_TO_SERVER) {
                responseTimeTbl.put(site.servers[0].getAddressString(), Long.valueOf(tcpTimeout));
            } else if (e.getCode() != HandleException.OTHER_CONNECTION_ESTABLISHED) {
                long time2 = System.currentTimeMillis();
                site.responseTime = time2 - time1;
                responseTimeTbl.put(site.servers[0].getAddressString(), Long.valueOf(time2 - time1));
            }
            throw e;
        }

        return response;
    }

    private SiteInfo[] adjustSitesForDomains(SiteInfo[] sites) {
        if (!someSiteHasZeroAddressAndDomain(sites)) return sites;
        List<SiteInfo> result = new ArrayList<>();
        for (SiteInfo site : sites) {
            if (!site.hasZeroAddressServersAndDomain()) {
                result.add(site);
                continue;
            }
            if (site.servers.length > 1) {
                addV4AndV6DomainSites(result, site);
            } else {
                addAllDomainSitesForSingleServerSite(result, site);
            }
        }
        return result.toArray(new SiteInfo[result.size()]);
    }

    private boolean someSiteHasZeroAddressAndDomain(SiteInfo[] sites) {
        for (SiteInfo site : sites) {
            if (site.hasZeroAddressServersAndDomain()) {
                return true;
            }
        }
        return false;
    }

    private void addAllDomainSitesForSingleServerSite(List<SiteInfo> result, SiteInfo site) {
        String domain = site.getDomainForServer(0);
        if (domain == null) {
            result.add(site);
            return;
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(domain);
            for (InetAddress address : addresses) {
                SiteInfo newSite = new SiteInfo(site);
                newSite.servers[0].ipAddress = Util.fill16(address.getAddress());
                result.add(newSite);
            }
        } catch (UnknownHostException e) {
            result.add(site);
            return;
        }
    }

    private void addV4AndV6DomainSites(List<SiteInfo> result, SiteInfo site) {
        byte[][] v4Addresses = new byte[site.servers.length][];
        byte[][] v6Addresses = new byte[site.servers.length][];
        boolean foundIpv4 = false;
        boolean foundIpv6 = false;
        boolean foundBoth = false;
        for (int i = 0; i < site.servers.length; i++) {
            byte[][] twoAddresses = getV4AndV6DomainAddressesForServer(site, i);
            byte[] v4Address = twoAddresses[0];
            byte[] v6Address = twoAddresses[1];
            if (v4Address == null && v6Address == null) {
                result.add(site);
                return;
            } else if (v6Address == null) {
                v4Addresses[i] = v6Addresses[i] = v4Address;
                foundIpv4 = true;
            } else if (v4Address == null) {
                v4Addresses[i] = v6Addresses[i] = v6Address;
                foundIpv6 = true;
            } else {
                v4Addresses[i] = v4Address;
                v6Addresses[i] = v6Address;
                foundBoth = true;
            }
        }
        if (foundBoth || foundIpv6) {
            SiteInfo v6Site = new SiteInfo(site);
            for (int i = 0; i < site.servers.length; i++) {
                v6Site.servers[i].ipAddress = v6Addresses[i];
            }
            result.add(v6Site);
        }
        if (foundBoth || (foundIpv4 && !foundIpv6)) {
            SiteInfo v4Site = new SiteInfo(site);
            for (int i = 0; i < site.servers.length; i++) {
                v4Site.servers[i].ipAddress = v4Addresses[i];
            }
            result.add(v4Site);
        }
    }

    private byte[][] getV4AndV6DomainAddressesForServer(SiteInfo site, int which) {
        ServerInfo server = site.servers[which];
        byte[][] res = new byte[2][]; // a pair of addresses
        if (!server.hasAllZerosAddress()) {
            if (server.isIPv4()) res[0] = server.ipAddress;
            else res[1] = server.ipAddress;
            return res;
        }
        String domain = site.getDomainForServer(which);
        if (domain == null) return res; // {null,null}
        try {
            InetAddress[] addresses = InetAddress.getAllByName(domain);
            for (InetAddress address : addresses) {
                if (res[0] == null && address instanceof Inet4Address) {
                    res[0] = Util.fill16(address.getAddress());
                }
                if (res[1] == null && address instanceof Inet6Address) {
                    res[1] = Util.fill16(address.getAddress());
                }
            }
            return res;
        } catch (UnknownHostException e) {
            return res; // {null,null}
        }
    }

    private ServerInfo adjustServerIpAddressForDomain(ServerInfo server, String domain) {
        if (!server.hasAllZerosAddress() || domain == null) return server;
        try {
            byte[] addressBytes = InetAddress.getByName(domain).getAddress();
            ServerInfo newServer = server.cloneServerInfo();
            newServer.ipAddress = Util.fill16(addressBytes);
            return newServer;
        } catch (UnknownHostException e) {
            return server;
        }
    }

    /***********************************************************************
     * Shortcut to sendRequestToServer(AbstractRequest, ServerInfo, null);
     ***********************************************************************/

    public AbstractResponse sendRequestToServer(AbstractRequest req, ServerInfo server) throws HandleException {
        return sendRequestToServer(req, server, null);
    }

    private AbstractResponse sendRequestToServer(AbstractRequest req, String domain, String path, ServerInfo server) throws HandleException {
        return sendRequestToServer(req, domain, path, server, null);
    }

    public AbstractResponse sendRequestToServer(AbstractRequest req, SiteInfo site, ServerInfo server) throws HandleException {
        return sendRequestToServer(req, site, server, null);
    }

    /*****************************************************************************
     *
     * Wrapper around sendRequestToServerByProtocol(), which used to have this
     * name and signature.  This is invoked by the code which formerly invoked
     * the older method.
     *
     */

    public AbstractResponse sendRequestToServer(AbstractRequest req, ServerInfo server, ResponseMessageCallback callback) throws HandleException {
        return sendRequestToServer(req, null, null, server, callback);
    }

    private AbstractResponse sendRequestToServer(AbstractRequest req, String domain, String path, ServerInfo server, ResponseMessageCallback callback) throws HandleException {
        AbstractResponse response = null;
        Exception exception = null;

        for (int preferredProtocol : preferredProtocols) {
            try {
                response = sendRequestToServerByProtocol(req, domain, path, server, preferredProtocol, callback); // Might throw exception
                if (response != null) return response;
            } catch (Exception e) {
                exception = e;
                // Ignore the exception except on the last time we can try.
            }
        }
        if (exception == null) throw new HandleException(HandleException.NO_ACCEPTABLE_INTERFACES, "There were no acceptable interfaces to server: " + server);
        else if (exception instanceof HandleException) throw (HandleException) exception;
        else throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, "Unable to contact site on any interfaces", exception);
    }

    public AbstractResponse sendRequestToServer(AbstractRequest req, SiteInfo site, ServerInfo server, ResponseMessageCallback callback) throws HandleException {
        AbstractResponse response = null;
        Exception exception = null;

        for (int preferredProtocol : preferredProtocols) {
            try {
                response = sendRequestToServerInSiteByProtocol(req, site, server, preferredProtocol, callback); // Might throw exception
                if (response != null) return response;
            } catch (Exception e) {
                exception = e;
                // Ignore the exception except on the last time we can try.
            }
        }
        if (exception == null) throw new HandleException(HandleException.NO_ACCEPTABLE_INTERFACES, "There were no acceptable interfaces to server: " + server);
        else if (exception instanceof HandleException) throw (HandleException) exception;
        else throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, "Unable to contact site on any interfaces", exception);
    }

    /**
     * Sends the given request to the appropriate server in the given site and
     * returns the response.  This will try to contact the appropriate server by
     * trying each of the preferred protocols, in order.
     */
    public AbstractResponse sendRequestToSite(AbstractRequest req, SiteInfo site) throws HandleException {
        return sendRequestToSite(req, site, null);
    }

    /**
     * Sends the given request to the appropriate server in the given site and
     * returns the response.  This will try to contact the appropriate server by
     * trying each of the preferred protocols, in order.
     */
    public AbstractResponse sendRequestToSite(AbstractRequest req, SiteInfo site, ResponseMessageCallback callback) throws HandleException {
        AbstractResponse resp = null;
        Exception lastException = null;

        for (int preferredProtocol : preferredProtocols) {
            try {
                resp = sendRequestToSite(req, site, preferredProtocol, callback);
                if (resp != null) break;
            } catch (Exception e) {
                // Ignore the exception except on the last time we can try.
                lastException = e;
            }
        }

        if (resp == null) {
            if (lastException instanceof HandleException) throw (HandleException) lastException;
            else if (lastException != null) throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, "Unable to contact site on any interface", lastException);
            else throw new HandleException(HandleException.NO_ACCEPTABLE_INTERFACES, "Cannot contact an acceptable interface", lastException);
        }
        return resp;
    }

    /*****************************************************************************
     *
     * Sends the given request to the specified server by the given protocol,
     * if supported.  Also, if there is a ClientSideSessionInfo or
     * ClientSessionTracker object associated with the request then a session
     * will be created (if necessary) and used to send the message.
     *
     * This was formerly sendRequestToServer(), with the 3-argument signature
     * of the "wrapper" function above.  The "for each protocol" loop has been
     * moved to the wrapper (and replaced with a passed-in specific protocol)
     * so that this function can be called without the loop by methods having
     * the loop themselves.
     *
     */

    private AbstractResponse sendRequestToServerByProtocol(AbstractRequest req, String domain, String path, ServerInfo server, int protocolToUse, ResponseMessageCallback callback) throws HandleException {
        return sendRequestToServerByProtocol(req, domain, path, server, protocolToUse, callback, false);
    }

    private AbstractResponse sendRequestToServerByProtocol(AbstractRequest req, String domain, String path, ServerInfo server, int protocolToUse, ResponseMessageCallback callback, boolean forceSessionForNonAdminRequest) throws HandleException {
        // if we haven't set our version, do
        if (req.majorProtocolVersion <= 0) {
            req.majorProtocolVersion = Common.COMPATIBILITY_MAJOR_VERSION;
            req.minorProtocolVersion = Common.COMPATIBILITY_MINOR_VERSION;
        }

        AbstractResponse response = null;
        Exception exception = null;
        Interface interfce = null;
        ClientSideSessionInfo sessionInfo = req.sessionInfo;
        ClientSessionTracker sessionTracker = null;

        // Get the server's interface that can handle the request
        // by the desired protocol.  Assume there's only 1.
        interfce = server.interfaceWithProtocol(protocolToUse, req);

        if (interfce == null) return null;

        // If there is a session tracker associated with the request, use it.

        sessionTracker = req.sessionTracker;

        // If not, and there is a session tracker for this resolver, use it.
        if (sessionTracker == null && (req.isAdminRequest || !req.ignoreRestrictedValues)) sessionTracker = resolverSessions;

        if (sessionInfo != null && sessionInfo.hasExpired()) {
            if (sessionTracker != null) sessionTracker.removeSession(sessionInfo);
            sessionInfo = null;
        }

        // Avoid starting sessions with resolution requests, since they may not authenticate; but put them in existing sessions
        boolean sessionToBeInitializedButNotAdminRequest = sessionTracker != null && req.sessionInfo == null && req.sessionTracker == null && !req.isAdminRequest && !forceSessionForNonAdminRequest;

        boolean sessionIsNew = false;
        boolean oldUnauthenticatedSession = false;

        // Figure out if we should be using a session to send this message or not.
        // if this is a session-setup request, skip this part to avoid a stack overflow
        if (sessionInfo == null && sessionTracker != null && req.hasEqualOrGreaterVersion(2, 1) && req.opCode != AbstractMessage.OC_SESSION_SETUP) {
            // There is a session tracker for this request, so get
            // the session for this request, if one already exists.

            SessionSetupInfo setupInfo = sessionTracker.getSessionSetupInfo();
            //      if(sessionInfo!=null && setupInfo != null && ClientSessionTracker.sessionOptionChanged(sessionInfo, setupInfo)) {
            //          sessionTracker.removeSession(sessionInfo);
            //          sessionInfo = null;
            //      }
            //      if(sessionInfo==null)
            sessionInfo = sessionTracker.getSession(server, req.authInfo);
            if (sessionInfo == null) {
                // only want one thread at a time using an unauthenticated session
                sessionInfo = sessionTracker.getAndRemoveSession(server, null);
                if (sessionInfo != null) oldUnauthenticatedSession = true;
            }

            if (sessionInfo == null && !sessionToBeInitializedButNotAdminRequest) {
                // No session has been setup yet...set up
                // a session now, if the setup info exists

                if (setupInfo != null) {
                    try {
                        sessionInfo = setupSessionWithServer(req, setupInfo, domain, path, server);
                        sessionIsNew = true;
                    } catch (Exception e) {
                        String msg = "Error setting up session";
                        throw new HandleException(HandleException.INTERNAL_ERROR, msg, e);
                    }
                }
            } else if (sessionInfo != null) {
                // Session already exists; if there is a change in
                // session setup, modify session attribute with server

                if (setupInfo != null && ClientSessionTracker.sessionOptionChanged(sessionInfo, setupInfo)) {
                    if (sessionToBeInitializedButNotAdminRequest) {
                        sessionInfo = null;
                    } else {
                        try {
                            ClientSideSessionInfo newSessionInfo;
                            newSessionInfo = setupSessionWithServer(req, setupInfo, domain, path, server, sessionInfo);
                            sessionTracker.removeSession(sessionInfo);
                            sessionInfo = newSessionInfo;
                            sessionIsNew = true;
                        } catch (Exception e) {
                            String msg = "Error modifying session";
                            throw new HandleException(HandleException.INTERNAL_ERROR, msg, e);
                        }
                    }
                } // option changed
            } // has a session info
        }

        // if there is a session, store it with the
        // request so that the response can be verified
        req.sessionInfo = sessionInfo;

        if (sessionInfo != null) {
            // if there is a session, sign the message after obtaining
            // the session id and sessionKey, process the original request
            do {
                req.requestId = Math.abs(messageIDRandom.nextInt());
            } while (req.requestId <= 0);
            req.sessionId = sessionInfo.sessionId;

            // Make sure that the message has the certify and encrypt flag set if
            // the session requires them
            req.certify = req.certify || sessionInfo.getAuthenticateMessageFlag();
            req.encrypt = req.encrypt || sessionInfo.getEncryptedMesssageFlag();

            req.majorProtocolVersion = sessionInfo.getMajorProtocolVersion();
            req.minorProtocolVersion = sessionInfo.getMinorProtocolVersion();
            req.setSupportedProtocolVersion();

            // attach MAC code for request
            req.signMessageForSession();
        } else {
            // not in a session
            req.sessionId = 0;
        }

        // Try to resolve using the given protocol

        boolean sessionProblems = false;
        boolean sessionTimeout = false;
        ResponseMessageCallback wrappedCallback;
        if (callback == null || callback instanceof AvoidTransientErrorResponseMessageCallbackWrapper) {
            wrappedCallback = callback;
        } else {
            wrappedCallback = new AvoidTransientErrorResponseMessageCallbackWrapper(callback);
        }

        try {
            response = sendRequestToInterface(req, domain, path, server, interfce, wrappedCallback);
            sessionProblems = response != null && response.responseCode >= AbstractMessage.RC_SESSION_TIMEOUT;
            sessionTimeout = response != null && response.responseCode == AbstractMessage.RC_SESSION_TIMEOUT;
        } catch (HandleException e) {
            if (sessionInfo != null && e.getCode() == HandleException.MISSING_OR_INVALID_SIGNATURE) {
                exception = e;
                sessionProblems = true;
            } else {
                exception = e;
            }
        } catch (Exception e) {
            exception = e;
        }

        if (sessionTracker != null && (sessionIsNew || oldUnauthenticatedSession) && response != null && response.getClass() != ChallengeResponse.class) {
            sessionTracker.putSession(sessionInfo, server, null);
        }

        // allow a second challenge response in order to downgrade protocol for a third verifying server
        for (int numChallenges = 0; numChallenges < 2; numChallenges++) {
            if (response != null && response.getClass() == ChallengeResponse.class) {
                // Got challenge, must authenticate
                if (req.authInfo == null) {
                    if (callback != null) {
                        callback.handleResponse(response);
                    }
                    throw new HandleException(HandleException.UNABLE_TO_AUTHENTICATE, "No authentication info provided");
                }
                //if (traceMessages)
                //  System.err.println("got challenge: " + response);

                // if we avoided starting a session because we thought we wouldn't have to authenticate, upgrade to a session now.
                if (sessionToBeInitializedButNotAdminRequest && sessionInfo == null) {
                    return sendRequestToServerByProtocol(req, domain, path, server, protocolToUse, callback, true);
                }

                // get the authInfo object to sign the challenge

                ChallengeResponse challResponse = (ChallengeResponse) response;
                byte sig[] = req.authInfo.authenticate(challResponse, req);

                // set the response to null again so that the challenge
                // doesn't get sent back to the caller if we get an
                // exception while responding to the challenge.

                int challengeSessionID = response.sessionId;

                response = null;
                try { // send a response to the challenge back to the same interface
                    ChallengeAnswerRequest answer = new ChallengeAnswerRequest(req.authInfo.getAuthType(), req.authInfo.getUserIdHandle(), req.authInfo.getUserIdIndex(), sig, req.authInfo);
                    answer.takeValuesFrom(req);
                    answer.originalRequest = req;
                    answer.majorProtocolVersion = challResponse.majorProtocolVersion;
                    answer.minorProtocolVersion = challResponse.minorProtocolVersion;
                    answer.setSupportedProtocolVersion();
                    // if(req.multithread) answer.multithread = true; // not needed since if we get here we already have the connection

                    // link our response with the server's challenge

                    answer.sessionId = challengeSessionID;
                    answer.sessionInfo = req.sessionInfo;
                    try {
                        response = sendRequestToInterface(answer, domain, path, server, interfce, wrappedCallback);
                        sessionProblems = response != null && response.responseCode >= AbstractMessage.RC_SESSION_TIMEOUT;
                        sessionTimeout = response != null && response.responseCode == AbstractMessage.RC_SESSION_TIMEOUT;
                    } catch (HandleException e) {
                        if (sessionInfo != null && e.getCode() == HandleException.MISSING_OR_INVALID_SIGNATURE) {
                            exception = e;
                            sessionProblems = true;
                        } else {
                            exception = e;
                        }
                    }
                    if (sessionTracker != null && response != null && sessionInfo != null && (sessionIsNew || oldUnauthenticatedSession) && !isAuthenticationOrSessionErrorResponse(response)) {
                        sessionTracker.putSession(sessionInfo, server, req.authInfo);
                    }
                } catch (Exception e) {
                    exception = e;
                }
                // Got challenge
            } else break; // no challenge
        }

        // If there was a session timeout,
        // remove the session and continue
        if (sessionProblems) {
            if (sessionInfo != null) {
                if (sessionTracker != null) sessionTracker.removeSession(sessionInfo);
                sessionInfo = null;
                req.sessionInfo = null;
                if (!sessionIsNew) {
                    return sendRequestToServerByProtocol(req, domain, path, server, protocolToUse, callback, forceSessionForNonAdminRequest);
                } else if (sessionTimeout) {
                    if (response != null && callback != null && isPotentiallyTransientErrorResponse(response)) {
                        callback.handleResponse(response);
                    }
                    throw new HandleException(HandleException.SERVER_ERROR, "Unexpected session timeout response on new session");
                }
            } else if (sessionTimeout) {
                if (response != null && callback != null && isPotentiallyTransientErrorResponse(response)) {
                    callback.handleResponse(response);
                }
                throw new HandleException(HandleException.SERVER_ERROR, "Unexpected session timeout response when not using a session");
            }
        }

        if (response != null && callback != null && isPotentiallyTransientErrorResponse(response)) {
            callback.handleResponse(response);
        }
        if (response != null) {
            return response;
        }

        if (exception != null) {
            if (exception instanceof HandleException) throw (HandleException) exception;
            // else...
            exception.printStackTrace();
            throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, "Got Exception: " + exception);
        }

        return null;
    }

    private static boolean isAuthenticationOrSessionErrorResponse(AbstractResponse response) {
        return response.responseCode >= 400;
    }

    private static boolean isPotentiallyTransientErrorResponse(AbstractResponse response) {
        return response.responseCode == AbstractMessage.RC_AUTHENTICATION_NEEDED || response.responseCode >= AbstractMessage.RC_SESSION_TIMEOUT;
    }

    private static class AvoidTransientErrorResponseMessageCallbackWrapper implements ResponseMessageCallback {
        private final ResponseMessageCallback delegate;

        public AvoidTransientErrorResponseMessageCallbackWrapper(ResponseMessageCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handleResponse(AbstractResponse message) throws HandleException {
            if (!isPotentiallyTransientErrorResponse(message)) {
                delegate.handleResponse(message);
            }
        }
    }

    //============================================================================

    /**
     * Create a new session to handle the given request.
     **/
    public ClientSideSessionInfo setupSessionWithServer(AbstractRequest req, SessionSetupInfo sessionOptions, ServerInfo server) throws Exception {
        return setupSessionWithServer(req.authInfo, req, sessionOptions, null, null, server, null, req.majorProtocolVersion, req.minorProtocolVersion);
    }

    private ClientSideSessionInfo setupSessionWithServer(AbstractRequest req, SessionSetupInfo sessionOptions, String domain, String path, ServerInfo server) throws Exception {
        return setupSessionWithServer(req.authInfo, req, sessionOptions, domain, path, server, null, req.majorProtocolVersion, req.minorProtocolVersion);
    }

    private ClientSideSessionInfo setupSessionWithServer(AbstractRequest req, SessionSetupInfo sessionOptions, String domain, String path, ServerInfo server, ClientSideSessionInfo currSession) throws Exception {
        return setupSessionWithServer(req.authInfo, req, sessionOptions, domain, path, server, currSession, req.majorProtocolVersion, req.minorProtocolVersion);
    }

    private ClientSideSessionInfo setupSessionWithServer(AuthenticationInfo authInfo, AbstractRequest origReq, SessionSetupInfo sessionOptions, String domain, String path, ServerInfo server, ClientSideSessionInfo currSession, int majorProtocolVersion,
                                                                           int minorProtocolVersion) throws Exception {
        AbstractResponse response = null;

        byte[] sessionKey = null;
        int sessionKeyAlg;

        byte[] identityHandle = null;
        int identityIndex = -1;
        if (authInfo != null) {
            identityHandle = authInfo.getUserIdHandle();
            identityIndex = authInfo.getUserIdIndex();
        }

        // send a new session set up request to server
        SessionSetupRequest sessionsetupReq = createSessionSetupRequest(authInfo, sessionOptions, majorProtocolVersion, minorProtocolVersion);

        if (currSession != null) {
            sessionsetupReq.sessionId = currSession.sessionId;
            sessionsetupReq.sessionInfo = currSession;
        }
        if (origReq != null && origReq.multithread) {
            sessionsetupReq.multithread = true;
            sessionsetupReq.connectionLock = origReq.connectionLock;
            sessionsetupReq.completed = origReq.completed;
            sessionsetupReq.socketRef = origReq.socketRef;
        }
        // Session setup request must be certified if any later request is to be certified.
        // Otherwise the later message would be signed with the session key, not the server's public key,
        // and the client would never get the chance to authenticate the server.
        sessionsetupReq.certify = true;

        HdlSecurityProvider provider = HdlSecurityProvider.getInstance();

        // send the session setup request.  If the server doesn't support
        // sessions an exception will be thrown here, and the resolve should
        // revert to using regular challenge/response authentication.
        response = sendRequestToServer(sessionsetupReq, domain, path, server);

        if (response == null || !(response instanceof SessionSetupResponse) || response.responseCode != AbstractMessage.RC_SUCCESS) {
            throw new HandleException(HandleException.SERVER_CANNOT_PROCESS_SESSION, String.valueOf(response));
        }

        SessionSetupResponse ssresp = (SessionSetupResponse) response;
        if (ssresp.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_CLIENT || ssresp.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_HDL) {
            // decrypt the session key here using the private key corresponding to
            // the public key that we just sent the server
            sessionKey = Util.decrypt(sessionOptions.privateExchangeKey, ssresp.data, ssresp.majorProtocolVersion, ssresp.minorProtocolVersion);

            if (ssresp.hasEqualOrGreaterVersion((byte) 2, (byte) 2)) {
                // the server is capable of encoding the encryption algorithm
                sessionKeyAlg = Encoder.readInt(sessionKey, 0);
                byte tmp[] = new byte[sessionKey.length - Encoder.INT_SIZE];
                System.arraycopy(sessionKey, Encoder.INT_SIZE, tmp, 0, tmp.length);
                sessionKey = tmp;
            } else {
                sessionKeyAlg = HdlSecurityProvider.ENCRYPT_ALG_DES;
            }
        } else if (ssresp.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_SERVER) {
            // the server has responded with its public key.  we should
            // generate a secret key and send it to the server encrypted with the
            // public key.

            byte[] pubExchangeKey = ssresp.data;
            byte[] encryptKey;

            // if the server doesn't support the 2.2 protocol or subsequent, the key algorithm
            // must be DES and the algorithm not encoded as part of the session key
            // message
            boolean oldServer = !ssresp.hasEqualOrGreaterVersion((byte) 2, (byte) 2);
            sessionKeyAlg = oldServer ? HdlSecurityProvider.ENCRYPT_ALG_DES : preferredEncryptionAlgorithm;
            sessionKey = HdlSecurityProvider.getInstance().generateSecretKey(sessionKeyAlg);
            encryptKey = Util.substring(sessionKey, Encoder.INT_SIZE);
            if (oldServer) sessionKey = encryptKey;

            PublicKey serverRSAPubKey = Util.getPublicKeyFromBytes(pubExchangeKey, 0);
            byte key[] = Util.encrypt(serverRSAPubKey, encryptKey, ssresp.majorProtocolVersion, ssresp.minorProtocolVersion);

            // send another session exchange key request to give the server the
            // secret key that was encrypted using the servers public key.
            SessionExchangeKeyRequest sekr = new SessionExchangeKeyRequest(key);
            sekr.takeValuesFrom(sessionsetupReq);
            sekr.majorProtocolVersion = ssresp.majorProtocolVersion;
            sekr.minorProtocolVersion = ssresp.minorProtocolVersion;
            sekr.setSupportedProtocolVersion();
            sekr.encrypt = false;
            sekr.sessionId = ssresp.sessionId;
            AbstractResponse rsp = sendRequestToServer(sekr, domain, path, server);

            if (rsp == null || rsp.responseCode != AbstractMessage.RC_SUCCESS) throw new HandleException(HandleException.SERVER_CANNOT_PROCESS_SESSION, "Server cipher key exchange failed.");

            if (ssresp.hasEqualOrGreaterVersion((byte) 2, (byte) 2)) {
                // trim the algorithm from the front of the session key for our own use
                sessionKey = Util.substring(sessionKey, Encoder.INT_SIZE);
            }
        } else if (ssresp.keyExchangeMode == Common.KEY_EXCHANGE_DH) {
            boolean legacy = !ssresp.hasEqualOrGreaterVersion(2, 4);
            DHPublicKey pub;
            if (legacy) {
                sessionKeyAlg = HdlSecurityProvider.ENCRYPT_ALG_DES;
                pub = (DHPublicKey) Util.getPublicKeyFromBytes(ssresp.data, 0);
                sessionKey = provider.getDESKeyFromDH(pub, (DHPrivateKey) sessionOptions.privateExchangeKey);
            } else {
                sessionKeyAlg = Encoder.readInt(ssresp.data, 0);
                pub = (DHPublicKey) Util.getPublicKeyFromBytes(ssresp.data, Encoder.INT_SIZE);
                sessionKey = provider.getKeyFromDH(pub, (DHPrivateKey) sessionOptions.privateExchangeKey, sessionKeyAlg);
                sessionKey = Util.substring(sessionKey, Encoder.INT_SIZE);
            }
        } else { // if we get here, then we don't understand the servers response
            throw new HandleException(HandleException.SERVER_CANNOT_PROCESS_SESSION, "Unknown key exchange mode");
        }

        // at this point the session key has been exchanged.
        // create a session info object here and return it
        ClientSideSessionInfo csinfo = new ClientSideSessionInfo(response.sessionId, sessionKey, identityHandle, identityIndex, sessionKeyAlg, server, response.majorProtocolVersion, response.minorProtocolVersion);
        // csinfo.setEncryptionAlgorithmCode(sessionKeyAlg);
        csinfo.takeValuesFromOption(sessionOptions);

        return csinfo;
    }

    /***********************************************************************
     * Shortcut to sendRequestToInterface(AbstractRequest, ServerInfo,
     *                                    Interface, null);
     ***********************************************************************/
    public AbstractResponse sendRequestToInterface(AbstractRequest req, ServerInfo server, Interface interfce) throws HandleException {
        return sendRequestToInterface(req, server, interfce, null);
    }

    public AbstractResponse sendRequestToInterface(AbstractRequest req, ServerInfo server, Interface interfce, ResponseMessageCallback callback) throws HandleException {
        return sendRequestToInterface(req, null, null, server, interfce, callback);
    }

    private AbstractResponse sendRequestToInterface(AbstractRequest req, String domain, String path, ServerInfo server, Interface interfce, ResponseMessageCallback callback) throws HandleException {
        // If the request is 'certified', then associate the servers' public
        // key with the request so that the response can be certified.
        req.serverPubKeyBytes = server.publicKey;

        InetAddress addr = server.getInetAddress();

        int port = interfce.port;
        AbstractResponse response = null;
        switch (interfce.protocol) {
            case Interface.SP_HDL_UDP:
                response = sendHdlUdpRequest(req, addr, port, callback);
                break;
            case Interface.SP_HDL_TCP:
                response = sendHdlTcpRequest(req, addr, port, callback);
                break;
            case Interface.SP_HDL_HTTP:
                if (req.hasEqualOrGreaterVersion(2, 8) && expectStreamingResponse(req) && !isDsaPublicKey(req.serverPubKeyBytes)) {
                    response = sendHttpsRequest(req, domain, path, addr, port, callback);
                } else {
                    response = sendHttpRequest(req, domain, path, addr, port, callback);
                }
                break;
            case Interface.SP_HDL_HTTPS:
                response = sendHttpsRequest(req, domain, path, addr, port, callback);
                break;
            default:
                throw new HandleException(HandleException.UNKNOWN_PROTOCOL, "unknown protocol: " + interfce.protocol);
        }

        if (response != null) {
            if (response.responseCode == AbstractMessage.RC_ERROR) {
                throw new HandleException(HandleException.SERVER_ERROR, Util.decodeString(((ErrorResponse) response).message));
            } else if (response.expiration < System.currentTimeMillis() / 1000) {
                throw new HandleException(HandleException.GOT_EXPIRED_MESSAGE);
            }
        }
        return response;
    }

    private boolean expectStreamingResponse(AbstractRequest req) {
        return req.opCode == AbstractMessage.OC_RETRIEVE_TXN_LOG || req.opCode == AbstractMessage.OC_DUMP_HANDLES || (req instanceof ChallengeAnswerRequest && expectStreamingResponse(((ChallengeAnswerRequest) req).originalRequest));
    }

    /**
     * Verify response message with the pre-established session key.
     */
    private final boolean verifyResponseWithSessionKey(AbstractRequest req, AbstractResponse response) throws HandleException {
        boolean veriPass = false;
        if (req == null || response == null) return false;

        try {
            veriPass = response.verifyMessage(req.sessionInfo.getSessionKey());
        } catch (HandleException e) {
            throw e;
        } catch (Exception e) {
            throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE, "Error verifying MAC code", e);
        }
        if (veriPass) {
            req.sessionInfo.addSessionCounter(response.sessionCounter, true);
        }
        return veriPass;
    }

    /** This function verifies the integrity of a response given the request
     that it is for.  The public key of the server is attached to the
     request so that this can verify the signature of the response.  This
     function also checks the digest of the request that was included
     (if requested) in the response. */
    private static final void verifyResponseWithServerPublicKey(AbstractRequest req, AbstractResponse response) throws HandleException {

        if (req.serverPubKeyBytes == null) {
            throw new HandleException(HandleException.SECURITY_ALERT, "Unable to verify certified message: no pubkey associated with request");
        }

        // the request was certified so we should verify the signature here
        PublicKey pubKey;
        try {
            pubKey = Util.getPublicKeyFromBytes(req.serverPubKeyBytes, 0);
        } catch (Exception e) {
            throw new HandleException(HandleException.INVALID_VALUE, "Unable to extract public key", e);
        }

        try {
            if (response.signature == null || response.signature.length <= 0) {
                throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE, "Verification failed, missing signature.");
            }
            if (!response.verifyMessage(pubKey)) {
                throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE, "Verification failed.");
            }
        } catch (Exception e) {
            // e.printStackTrace();
            throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE, "Unable to verify signature for message: " + response, e);
        }

        if (req.sessionInfo != null) req.sessionInfo.addSessionCounter(response.sessionCounter, true);
    }

    private static void verifyRequestDigestIfNeeded(AbstractRequest req, AbstractResponse response) throws HandleException {
        // Make sure that the server is responding to the request as we sent it.
        // This is because our request could have been modified on its way to
        // the server since requests aren't signed.  We get around that by having
        // the server include a digest of the original request with its response.
        if (req.returnRequestDigest) {
            byte requestDigest[] = Util.doDigest(response.rdHashType, req.getEncodedMessageBody());
            if (!Util.equals(requestDigest, response.requestDigest)) {
                throw new HandleException(HandleException.SECURITY_ALERT, "Message came back with invalid request digest.");
            }
        }
    }

    /***********************************************************************
     * Shortcut to sendHdlUdpRequest(req, addr, port, null);
     ***********************************************************************/
    public AbstractResponse sendHdlUdpRequest(AbstractRequest req, InetAddress addr, int port) throws HandleException {
        return sendHdlUdpRequest(req, addr, port, null);
    }

    private static void waitIfSiblingConnectedAndThrowHandleExceptionIfFinished(AbstractRequest req) throws HandleException {
        if (req.multithread) {
            try {
                waitIfSiblingConnectedAndThrowInterruptedExceptionIfFinished(req);
            } catch (InterruptedException e) {
                throw new HandleException(HandleException.OTHER_CONNECTION_ESTABLISHED, HandleException.OTHER_CONNECTION_ESTABLISHED_STRING);
            }
        }
    }

    private static void waitIfSiblingConnectedAndThrowInterruptedExceptionIfFinished(AbstractRequest req) throws InterruptedException {
        if (req.multithread) {
            if (req.completed.get()) throw new InterruptedException();
            if (!req.connectionLock.tryLock()) {
                req.connectionLock.lockInterruptibly();
            }
            req.connectionLock.unlock();
            if (req.completed.get()) throw new InterruptedException();
        }
    }

    private static void lockConnectionAndThrowHandleExceptionIfFinished(AbstractRequest req) throws HandleException {
        if (req.multithread) {
            if (req.completed.get()) throw new HandleException(HandleException.OTHER_CONNECTION_ESTABLISHED, HandleException.OTHER_CONNECTION_ESTABLISHED_STRING);
            try {
                req.connectionLock.lockInterruptibly();
            } catch (InterruptedException e) {
                throw new HandleException(HandleException.OTHER_CONNECTION_ESTABLISHED, HandleException.OTHER_CONNECTION_ESTABLISHED_STRING);
            }
            if (req.completed.get()) {
                req.connectionLock.unlock();
                throw new HandleException(HandleException.OTHER_CONNECTION_ESTABLISHED, HandleException.OTHER_CONNECTION_ESTABLISHED_STRING);
            }
        }
    }

    public AbstractResponse sendHdlUdpRequest(AbstractRequest req, InetAddress addr, int port, ResponseMessageCallback callback) throws HandleException {
        config.startAutoUpdate(this);
        addr = config.mapLocalAddress(addr);
        DatagramSocket socket = null;
        DatagramPacket[] packets = null;
        Exception lastException = null;

        // create the socket, set the timeout value
        try {
            try {
                socket = new DatagramSocket();
            } catch (Exception e) {
                if (socket != null) try {
                    socket.close();
                } catch (Exception e2) {
                }
                throw new HandleException(HandleException.INTERNAL_ERROR, e);
            }

            MessageEnvelope rcvEnvelope = new MessageEnvelope();
            long whenToTimeout;

            for (int element : udpRetryScheme) {

                if (packets == null) {
                    packets = getUdpPacketsForRequest(req, addr, port);
                } else {
                    // if sending UDP requests in a session, resign each attempt to avoid duplicate session counters
                    if (req.sessionInfo != null && req.authInfo != null && req.hasEqualOrGreaterVersion(2, 5)) {
                        req.signMessageForSession();
                        packets = getUdpPacketsForRequest(req, addr, port);
                    }
                }
                waitIfSiblingConnectedAndThrowHandleExceptionIfFinished(req);

                if (traceMessages) {
                    System.err.println("  sending HDL-UDP request (" + req + ") to " + Util.rfcIpPortRepr(addr, port));
                }

                // send out each request packet
                try {
                    socket.setSoTimeout(element);
                    for (DatagramPacket datagramPacket : packets) {
                        socket.send(datagramPacket);
                    }
                    whenToTimeout = System.currentTimeMillis() + element;
                } catch (Exception e) {
                    throw new HandleException(HandleException.INTERNAL_ERROR, String.valueOf(e) + " sending UDP request to " + Util.rfcIpRepr(addr));
                }

                // loop, waiting for packets until the timeout is reached or
                // we have all of the packets, whichever comes first.
                byte returnMessage[] = null;
                boolean packetsReceived[] = null;
                boolean haveAllPackets = false;
                while (!haveAllPackets && System.currentTimeMillis() <= whenToTimeout) {
                    DatagramPacket rspnsPkt = new DatagramPacket(new byte[maxUDPDataSize + Common.MESSAGE_ENVELOPE_SIZE], maxUDPDataSize + Common.MESSAGE_ENVELOPE_SIZE);

                    waitIfSiblingConnectedAndThrowHandleExceptionIfFinished(req);

                    try {
                        socket.receive(rspnsPkt);
                        if (rspnsPkt.getLength() <= 0) continue;

                        // need to decode the envelop data...
                        byte rspnsPktData[] = rspnsPkt.getData();
                        int rspnsPktDataLen = rspnsPkt.getLength();

                        Encoder.decodeEnvelope(rspnsPktData, rcvEnvelope);

                        // if we got someone else's packet, ignore it.
                        if (rcvEnvelope.requestId != req.requestId) continue;

                        if (packetsReceived == null) {
                            int numPkts = rcvEnvelope.messageLength / maxUDPDataSize;
                            if ((rcvEnvelope.messageLength % maxUDPDataSize) != 0) numPkts++;
                            packetsReceived = new boolean[numPkts];
                            for (int pr = 0; pr < packetsReceived.length; pr++) {
                                packetsReceived[pr] = false;
                            }
                            returnMessage = new byte[rcvEnvelope.messageLength];
                        }

                        packetsReceived[rcvEnvelope.messageId] = true;
                        System.arraycopy(rspnsPktData, Common.MESSAGE_ENVELOPE_SIZE, returnMessage, rcvEnvelope.messageId * maxUDPDataSize, rspnsPktDataLen - Common.MESSAGE_ENVELOPE_SIZE);
                        haveAllPackets = true;
                        for (int pr = 0; pr < packetsReceived.length; pr++) {
                            if (!packetsReceived[pr]) {
                                haveAllPackets = false;
                                pr = packetsReceived.length;
                            }
                        }

                        if (haveAllPackets) {

                            if (req.multithread) req.connectionLock.lockInterruptibly();

                            // decrypt the message using pre-established session information
                            if (rcvEnvelope.encrypted) {
                                // try session key decryption first ...
                                ClientSideSessionInfo sessionInfo = req.sessionInfo;
                                if (sessionInfo == null) throw new HandleException(HandleException.INCOMPLETE_SESSIONSETUP, "Cannot decrypt message without a session");

                                if (traceMessages) System.err.println("Decrypting UDP message: " + rcvEnvelope);
                                if (returnMessage == null) throw new AssertionError();
                                returnMessage = sessionInfo.decryptBuffer(returnMessage, 0, returnMessage.length);
                                rcvEnvelope.encrypted = false;
                                rcvEnvelope.messageLength = returnMessage.length;
                            }

                            // parse the message
                            AbstractResponse response = (AbstractResponse) Encoder.decodeMessage(returnMessage, 0, rcvEnvelope);

                            if (traceMessages) System.err.println("    received HDL-UDP response: " + response);

                            checkSignatureIfNeeded(req, response);

                            if (callback != null) {
                                callback.handleResponse(response);
                            }
                            return response;
                        }
                    } catch (InterruptedException e) {
                        throw new HandleException(HandleException.OTHER_CONNECTION_ESTABLISHED, HandleException.OTHER_CONNECTION_ESTABLISHED_STRING);
                    } catch (Exception e) {
                        lastException = e;
                    }
                }
            }
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                }
            }
        }

        if (lastException != null) {
            if (lastException instanceof HandleException) throw (HandleException) lastException;
            else throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, addr + ": " + lastException.toString());
        }
        throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, "Unable to connect to server: " + addr);
    }

    private void checkSignatureIfNeeded(AbstractRequest req, AbstractResponse response) throws HandleException {
        verifyRequestDigestIfNeeded(req, response);
        if (req.certify && checkSignatures) {
            if (response.signatureIsMac()) {
                if (response.sessionId <= 0) {
                    throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE, "Message signed with MAC but no session id given");
                }
                if (req.sessionInfo == null) {
                    throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE, "Message signed with MAC but no client-side session information");
                }
                if (!verifyResponseWithSessionKey(req, response)) {
                    throw new HandleException(HandleException.MISSING_OR_INVALID_SIGNATURE, "Verification of session MAC failed");
                }
            } else {
                verifyResponseWithServerPublicKey(req, response);
            }
        }
    }

    private DatagramPacket[] getUdpPacketsForRequest(AbstractRequest req, InetAddress addr, int port) throws HandleException {
        MessageEnvelope sndEnvelope = new MessageEnvelope();
        if (req.majorProtocolVersion > 0 && req.minorProtocolVersion >= 0) {
            sndEnvelope.protocolMajorVersion = req.majorProtocolVersion;
            sndEnvelope.protocolMinorVersion = req.minorProtocolVersion;
            sndEnvelope.suggestMajorProtocolVersion = req.suggestMajorProtocolVersion;
            sndEnvelope.suggestMinorProtocolVersion = req.suggestMinorProtocolVersion;
        }

        sndEnvelope.sessionId = req.sessionId;
        if (req.requestId <= 0) {
            sndEnvelope.requestId = Math.abs(messageIDRandom.nextInt());
            req.requestId = sndEnvelope.requestId;
        } else {
            sndEnvelope.requestId = req.requestId;
        }

        // get the encoded message (including header body and signature,
        // but not the envelope) and create a set of udp packets from it
        byte requestBuf[] = req.getEncodedMessage();

        // request may choose to encrypt itself here if session available.
        if (req.encrypt || (req.sessionInfo != null && req.shouldEncrypt())) {
            if (req.sessionInfo == null) throw new HandleException(HandleException.INCOMPLETE_SESSIONSETUP, "Cannot encrypt messages without a session");
            requestBuf = req.sessionInfo.encryptBuffer(requestBuf, 0, requestBuf.length);
            // req.encrypt could be just a request that the server encrypt the response;
            // whether to encrypt the request could be separate
            sndEnvelope.encrypted = true;
        }

        sndEnvelope.messageLength = requestBuf.length;

        int numPackets = sndEnvelope.messageLength / maxUDPDataSize;
        if ((sndEnvelope.messageLength % maxUDPDataSize) != 0) numPackets++;

        if (numPackets == 0) {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Cannot send empty request");
        }
        DatagramPacket packets[] = new DatagramPacket[numPackets];
        int bytesRemaining = sndEnvelope.messageLength;

        sndEnvelope.truncated = numPackets > 1;

        for (int packetNum = 0; packetNum < numPackets; packetNum++) {
            int thisPacketSize = Math.min(maxUDPDataSize, bytesRemaining);
            byte buf[] = new byte[thisPacketSize + Common.MESSAGE_ENVELOPE_SIZE];
            sndEnvelope.messageId = packetNum;
            Encoder.encodeEnvelope(sndEnvelope, buf);
            System.arraycopy(requestBuf, requestBuf.length - bytesRemaining, buf, Common.MESSAGE_ENVELOPE_SIZE, buf.length - Common.MESSAGE_ENVELOPE_SIZE);
            packets[packetNum] = new DatagramPacket(buf, buf.length, addr, port);
            bytesRemaining -= thisPacketSize;
        }
        return packets;
    }

    /***********************************************************************
     * Shortcut to sendHdlTcpRequest(req, addr, port, null);
     ***********************************************************************/
    public AbstractResponse sendHdlTcpRequest(AbstractRequest req, InetAddress addr, int port) throws HandleException {
        return sendHdlTcpRequest(req, addr, port, null);
    }

    @SuppressWarnings("resource") // complicated close logic; do not close streaming response
    public AbstractResponse sendHdlTcpRequest(AbstractRequest req, InetAddress addr, int port, ResponseMessageCallback callback) throws HandleException {
        config.startAutoUpdate(this);

        addr = config.mapLocalAddress(addr);

        MessageEnvelope sndEnvelope = new MessageEnvelope();
        if (req.majorProtocolVersion > 0 && req.minorProtocolVersion >= 0) {
            sndEnvelope.protocolMajorVersion = req.majorProtocolVersion;
            sndEnvelope.protocolMinorVersion = req.minorProtocolVersion;
            sndEnvelope.suggestMajorProtocolVersion = req.suggestMajorProtocolVersion;
            sndEnvelope.suggestMinorProtocolVersion = req.suggestMinorProtocolVersion;
        }

        sndEnvelope.sessionId = req.sessionId;
        if (req.requestId <= 0) {
            sndEnvelope.requestId = Math.abs(messageIDRandom.nextInt());
            req.requestId = sndEnvelope.requestId;
        } else {
            sndEnvelope.requestId = req.requestId;
        }

        // get the encoded message, including the header and signature but not the envelope
        byte requestBuf[] = req.getEncodedMessage();

        // todo ll:加密传输
        // request may choose to encrypt itself here if session available.
        if (req.encrypt || (req.sessionInfo != null && req.shouldEncrypt())) {
            if (req.sessionInfo == null) throw new HandleException(HandleException.INCOMPLETE_SESSIONSETUP, "Cannot encrypt messages without a session");
            requestBuf = req.sessionInfo.encryptBuffer(requestBuf, 0, requestBuf.length);
            // RS 2009-06 In principle, req.encrypt is just a request that the server encrypt the response;
            // whether to encrypt the request could be separate
            sndEnvelope.encrypted = true;
        }

        sndEnvelope.messageLength = requestBuf.length;

        waitIfSiblingConnectedAndThrowHandleExceptionIfFinished(req);

        if (traceMessages) {
            System.err.println("  sending HDL-TCP request (" + req + ") to " + Util.rfcIpPortRepr(addr, port));
        }

        AbstractResponse response = null;
        Socket socket = null;
        OutputStream out = null;
        InputStream in = null;
        try {
            // create the socket, set the timeout value
            try {
                socket = SocketChannel.open().socket();
                socket.setSoTimeout(tcpTimeout);
                socket.setSoLinger(false, 0);
                if (req.multithread) req.socketRef.set(socket);
                socket.connect(new InetSocketAddress(addr, port), tcpTimeout);
            } catch (Exception e) {
                req.socketRef.set(null);
                if (socket != null) try {
                    socket.close();
                } catch (Exception e2) {
                }
                throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, "" + addr, e);
            }

            lockConnectionAndThrowHandleExceptionIfFinished(req);


            MessageEnvelope rcvEnvelope = new MessageEnvelope();
            // todo ll:发送请求
            // send out the request...
            byte envBuf[] = new byte[Common.MESSAGE_ENVELOPE_SIZE];
            try {
                out = socket.getOutputStream();
                Encoder.encodeEnvelope(sndEnvelope, envBuf);
                out.write(Util.concat(envBuf, requestBuf));
                out.flush();
                in = new BufferedInputStream(socket.getInputStream());
            } catch (Exception e) {
                throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, String.valueOf(e) + " sending TCP request to " + Util.rfcIpRepr(addr), e);
            }

            int r, n;

            while (true) {
                n = 0;
                // read the envelope from the socket
                while (n < Common.MESSAGE_ENVELOPE_SIZE && (r = in.read(envBuf, n, Common.MESSAGE_ENVELOPE_SIZE - n)) > 0) {
                    n += r;
                }
                if (n == 0) throw new HandleException(HandleException.SERVER_ERROR, "Connection closed without response");
                if (n < Common.MESSAGE_ENVELOPE_SIZE) throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Connection closed partway through message envelope");

                // decode the envelope
                Encoder.decodeEnvelope(envBuf, rcvEnvelope);

                // if we got someone else's packet, there is something *really* wrong
                if (rcvEnvelope.requestId != req.requestId) throw new HandleException(HandleException.SECURITY_ALERT, "Message came back with different ID: " + req.requestId + "!=" + rcvEnvelope.requestId);

                // read the message body+header from the socket
                byte messageBuf[] = new byte[rcvEnvelope.messageLength];
                n = 0;
                while (n < rcvEnvelope.messageLength && (r = in.read(messageBuf, n, rcvEnvelope.messageLength - n)) > 0) {
                    n += r;
                }
                if (n < rcvEnvelope.messageLength) throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Connection closed partway through message");

                // decrypt the received message
                if (rcvEnvelope.encrypted) {
                    ClientSideSessionInfo csinfo = req.sessionInfo;
                    if (csinfo == null) throw new HandleException(HandleException.INCOMPLETE_SESSIONSETUP, "Cannot decrypt messages without a session");

                    if (traceMessages) System.err.println("Decrypting TCP message: " + rcvEnvelope);

                    messageBuf = csinfo.decryptBuffer(messageBuf, 0, messageBuf.length);
                    rcvEnvelope.encrypted = false;
                    rcvEnvelope.messageLength = messageBuf.length;
                }

                // parse the response message
                response = (AbstractResponse) Encoder.decodeMessage(messageBuf, 0, rcvEnvelope);

                if (response.streaming) {
                    response.stream = in;
                    response.socket = socket;
                }

                checkSignatureIfNeeded(req, response);

                if (traceMessages) System.err.println("    received HDL-TCP response: " + response);

                if (callback != null) {
                    callback.handleResponse(response);
                } else {
                    // there is no callback, just return this message
                    return response;
                }

                // If there are no more messages, notify the callback, and return
                // the current (ie the last) message.
                if (!response.continuous) {
                    return response;
                }
            }
        } catch (IOException e) {
            if (traceMessages) e.printStackTrace(System.err);
            throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, "Error talking to " + Util.rfcIpRepr(addr), e);
        } finally {
            req.socketRef.set(null);
            if (socket != null && (response == null || !response.streaming)) {
                if (in != null) try {
                    in.close();
                } catch (Exception e) {
                }
                if (out != null) try {
                    out.close();
                } catch (Exception e) {
                }
                try {
                    socket.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static final String encodeHandleAsUri(byte handle[]) {
        // considered encodeURLPath, but ParameterParser uses ! as a delimiter.
        return "/" + StringUtils.encodeURLComponent(Util.decodeString(handle));
    }

    /***********************************************************************
     * Shortcut to sendHttpRequest(req, addr, port, null);
     ***********************************************************************/
    public AbstractResponse sendHttpRequest(AbstractRequest req, InetAddress addr, int port) throws HandleException {
        return sendHttpRequest(req, addr, port, null);
    }

    public AbstractResponse sendHttpRequest(AbstractRequest req, InetAddress addr, int port, ResponseMessageCallback callback) throws HandleException {
        return sendHttpRequest(req, null, null, addr, port, callback);
    }

    private AbstractResponse sendHttpRequest(AbstractRequest req, String domain, String path, InetAddress addr, int port, ResponseMessageCallback callback) throws HandleException {
        return sendHttpOrHttpsRequest(req, domain, path, addr, port, callback, false);
    }

    public AbstractResponse sendHttpsRequest(AbstractRequest req, InetAddress addr, int port, ResponseMessageCallback callback) throws HandleException {
        return sendHttpsRequest(req, null, null, addr, port, callback);
    }

    private AbstractResponse sendHttpsRequest(AbstractRequest req, String domain, String path, InetAddress addr, int port, ResponseMessageCallback callback) throws HandleException {
        return sendHttpOrHttpsRequest(req, domain, path, addr, port, callback, true);
    }

    @SuppressWarnings("resource") // complicated close logic; do not close streaming response
    private AbstractResponse sendHttpOrHttpsRequest(AbstractRequest req, String domain, String path, InetAddress addr, int port, ResponseMessageCallback callback, boolean isHttps) throws HandleException {
        config.startAutoUpdate(this);

        String protocol = isHttps ? "HTTPS" : "HTTP";

        addr = config.mapLocalAddress(addr);

        MessageEnvelope sndEnvelope = new MessageEnvelope();
        if (req.majorProtocolVersion > 0 && req.minorProtocolVersion >= 0) {
            sndEnvelope.protocolMajorVersion = req.majorProtocolVersion;
            sndEnvelope.protocolMinorVersion = req.minorProtocolVersion;
            sndEnvelope.suggestMajorProtocolVersion = req.suggestMajorProtocolVersion;
            sndEnvelope.suggestMinorProtocolVersion = req.suggestMinorProtocolVersion;
        }

        sndEnvelope.sessionId = req.sessionId;
        if (req.requestId <= 0) {
            sndEnvelope.requestId = Math.abs(messageIDRandom.nextInt());
            req.requestId = sndEnvelope.requestId;
        } else {
            sndEnvelope.requestId = req.requestId;
        }

        // get the encoded message (including header and signature,
        // but not the envelope, and create a set of udp packets from it
        byte requestBuf[] = req.getEncodedMessage();

        // request may choose to encrypt itself here if session available.
        if (req.encrypt || (req.sessionInfo != null && req.shouldEncrypt())) {
            if (req.sessionInfo == null) throw new HandleException(HandleException.INCOMPLETE_SESSIONSETUP, "Cannot encrypt messages without a session");
            requestBuf = req.sessionInfo.encryptBuffer(requestBuf, 0, requestBuf.length);
            // Note that in principle, req.encrypt is just a request that the server encrypt the response;
            // whether to encrypt the request could be separate
            sndEnvelope.encrypted = true;
        }

        sndEnvelope.messageLength = requestBuf.length;

        waitIfSiblingConnectedAndThrowHandleExceptionIfFinished(req);

        if (traceMessages) {
            System.err.println("  sending HDL-" + protocol + " request (" + req + ") to " + Util.rfcIpPortRepr(addr, port));
        }

        AbstractResponse response = null;
        Socket socket = null;
        OutputStream out = null;
        InputStream in = null;
        DataInputStream din = null;
        try {
            // create the socket, set the timeout value
            try {
                if (isHttps) {
                    socket = getHttpsSocket(req);
                } else {
                    socket = SocketChannel.open().socket();
                }
                socket.setSoTimeout(tcpTimeout);
                socket.setSoLinger(false, 0);
                if (req.multithread) req.socketRef.set(socket);
                socket.connect(new InetSocketAddress(addr, port), tcpTimeout);

            } catch (Exception e) {
                req.socketRef.set(null);
                if (socket != null) try {
                    socket.close();
                } catch (Exception e2) {
                }
                if (e instanceof HandleException) throw (HandleException) e;
                throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, Util.rfcIpRepr(addr) + ": " + e.toString(), e);
            }

            lockConnectionAndThrowHandleExceptionIfFinished(req);

            MessageEnvelope rcvEnvelope = new MessageEnvelope();
            // send out the request...
            byte envBuf[] = new byte[Common.MESSAGE_ENVELOPE_SIZE];
            try {
                out = new BufferedOutputStream(socket.getOutputStream());
                Encoder.encodeEnvelope(sndEnvelope, envBuf);
                String uri = encodeHandleAsUri(req.handle);
                if (path != null) {
                    if (!path.startsWith("/")) path = "/" + path;
                    if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
                    uri = path + uri;
                }
                out.write(Util.encodeString("POST " + uri + " HTTP/1.1\r\n"));
                String hostHeader = getHostHeader(domain, addr, port, isHttps);
                out.write(Util.encodeString(hostHeader + "\r\n"));
                out.write(HTTP_ACCEPT_HEADER);
                out.write(HTTP_AGENT_HEADER);
                out.write(HTTP_CONTENT_TYPE_HEADER);
                out.write(Util.encodeString("Content-Length: " + (envBuf.length + requestBuf.length) + "\r\n"));
                out.write(HTTP_NEWLINE);

                out.write(envBuf, 0, Common.MESSAGE_ENVELOPE_SIZE);
                out.write(requestBuf, 0, requestBuf.length);
                out.flush();
                in = new BufferedInputStream(socket.getInputStream());
            } catch (Exception e) {
                throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, String.valueOf(e) + " sending " + protocol + " request to " + Util.rfcIpRepr(addr), e);
            }

            // read the headers
            Map<String, String> headers = new HashMap<>();
            din = new DataInputStream(in);
            while (true) {
                // DataInputStream.readLine is deprecated, but this is all ASCII, so it works
                String line = ((DataInput) din).readLine();
                if (line == null) {
                    if (headers.isEmpty()) {
                        throw new HandleException(HandleException.SERVER_ERROR, "Connection closed without response");
                    }
                    throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, Util.rfcIpRepr(addr) + ": Unexpected end of HTTP message during headers");
                }
                line = line.trim();
                if (line.length() <= 0) break;
                int colIdx = line.indexOf(';');
                if (colIdx < 0) {
                    headers.put(line.toUpperCase(), "");
                } else {
                    headers.put(line.substring(0, colIdx), line.substring(colIdx + 1));
                }
            }

            int r, n = 0;

            while (true) {
                // read the envelope from the socket
                n = 0;
                while (n < Common.MESSAGE_ENVELOPE_SIZE && (r = in.read(envBuf, n, Common.MESSAGE_ENVELOPE_SIZE - n)) > 0) {
                    n += r;
                }
                if (n == 0) throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Connection closed after HTTP headers but with no body");
                if (n < Common.MESSAGE_ENVELOPE_SIZE) throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Connection closed partway through message envelope");

                // decode the envelope
                Encoder.decodeEnvelope(envBuf, rcvEnvelope);

                // if we got someone else's packet, there is something *really* wrong
                if (rcvEnvelope.requestId != req.requestId) throw new HandleException(HandleException.SECURITY_ALERT, "Message came back with different ID: " + req.requestId + "!=" + rcvEnvelope.requestId);

                // read the message body+header from the socket
                byte messageBuf[] = new byte[rcvEnvelope.messageLength];
                n = 0;
                while (n < rcvEnvelope.messageLength && (r = in.read(messageBuf, n, rcvEnvelope.messageLength - n)) > 0) {
                    n += r;
                }
                if (n < rcvEnvelope.messageLength) throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Connection closed partway through message");

                // decrypt message if the session requires encryption
                if (rcvEnvelope.encrypted) {
                    if (rcvEnvelope.sessionId > 0) {
                        //try to decrypt with session key ...
                        ClientSideSessionInfo csinfo = req.sessionInfo;
                        if (csinfo == null) {
                            throw new HandleException(HandleException.INCOMPLETE_SESSIONSETUP, "Cannot encrypt messages without a session");
                        }

                        messageBuf = csinfo.decryptBuffer(messageBuf, 0, messageBuf.length);
                        rcvEnvelope.encrypted = false;
                        rcvEnvelope.messageLength = messageBuf.length;
                    } else {
                        throw new HandleException(HandleException.SECURITY_ALERT, "Invalid response session id.  Cannot decrypt response.");
                    }
                }

                response = (AbstractResponse) Encoder.decodeMessage(messageBuf, 0, rcvEnvelope);

                if (response.streaming) {
                    response.stream = in;
                    response.socket = socket;
                    response.secureStream = isHttps && !isDsaPublicKey(req.serverPubKeyBytes);
                }

                checkSignatureIfNeeded(req, response);

                if (traceMessages) System.err.println("    received HDL-" + protocol + " response: " + response);

                if (callback != null) {
                    callback.handleResponse(response);
                } else {
                    // there is no callback, just return this message
                    return response;
                }

                // If there are no more messages, notify the callback, and return
                // the current (ie the last) message.
                if (!response.continuous) {
                    return response;
                }
            }
        } catch (IOException e) {
            if (traceMessages) e.printStackTrace(System.err);
            throw new HandleException(HandleException.CANNOT_CONNECT_TO_SERVER, "Error talking to " + Util.rfcIpRepr(addr), e);
        } finally {
            req.socketRef.set(null);
            if (socket != null && (response == null || !response.streaming)) {
                if (din != null) try {
                    din.close();
                } catch (Exception e) {
                }
                if (in != null) try {
                    in.close();
                } catch (Exception e) {
                }
                if (out != null) try {
                    out.close();
                } catch (Exception e) {
                }
                try {
                    socket.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private String getHostHeader(String domain, InetAddress addr, int port, boolean isHttps) {
        String hostHeader = "Host: ";
        if (domain != null) {
            hostHeader += domain;
        } else if (addr instanceof Inet6Address) {
            hostHeader += "[" + Util.rfcIpRepr(addr) + "]";
        } else {
            hostHeader += addr.getHostAddress();
        }
        if ((isHttps && port != 443) || (!isHttps && port != 80)) {
            hostHeader += ":" + port;
        }
        return hostHeader;
    }

    private Socket getHttpsSocket(AbstractRequest req) throws Exception {
        if (req.serverPubKeyBytes == null) {
            throw new HandleException(HandleException.SECURITY_ALERT, "Server key not known when getting HTTPS socket");
        }
        SSLContext sslContext;
        // Since many handle servers use DSA keys, and as of 2015 browsers do not allow DSA certificates,
        // we do not check the certificate used.  Since we are just tunneling the handle protocol this
        // does not affect the security; we certify at the handle protocol level instead.
        if (isDsaPublicKey(req.serverPubKeyBytes)) {
            sslContext = SSLEngineHelper.getAllTrustingClientSSLContext();
        } else {
            sslContext = SSLEngineHelper.getClientSSLContext(req.serverPubKeyBytes);
        }
        SSLSocket res = (SSLSocket) sslContext.getSocketFactory().createSocket();
        try {
            res.setEnabledCipherSuites(SSLEngineHelper.ENABLED_CIPHER_SUITES);
            res.setEnabledProtocols(SSLEngineHelper.ENABLED_CLIENT_PROTOCOLS);
            return res;
        } catch (Exception e) {
            try {
                res.close();
            } catch (Exception ex) {
            }
            throw e;
        }
    }

    private static boolean isDsaPublicKey(byte[] pubKeyBytes) throws HandleException {
        byte keyType[] = Encoder.readByteArray(pubKeyBytes, 0);
        return Util.equals(keyType, Common.KEY_ENCODING_DSA_PUBLIC);
    }
}
