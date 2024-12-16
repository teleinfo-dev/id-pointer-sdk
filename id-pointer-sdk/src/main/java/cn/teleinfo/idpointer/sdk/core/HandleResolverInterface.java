package cn.teleinfo.idpointer.sdk.core;

import cn.teleinfo.idpointer.sdk.core.util.LRUCacheTable;

public interface HandleResolverInterface {
    void setTcpTimeout(int timeout);

    void setPreferredProtocols(int[] ints);

    void setSiteFilter(SiteFilter keywordSiteFilter);

    void setUseIPv6FastFallback(boolean ipv6FastFallback);

    void setTraceMessages(boolean b);

    int[] getPreferredProtocols();

    LRUCacheTable<String, Long> getResponseTimeTbl();

    LRUCacheTable<String, Long> getPreferredPrimaryTbl();

    AbstractIdResponse sendRequestToSite(AbstractIdRequest req, SiteInfo site, int preferredProtocol, ResponseMessageCallback callback) throws HandleException;

    void setCheckSignatures(boolean b);

    AbstractIdResponse processRequest(AbstractIdRequest req, ResponseMessageCallback callback) throws HandleException;

    AbstractIdResponse processRequest(AbstractIdRequest req) throws HandleException;

    HandleValue resolveValueReference(ValueReference valueReference) throws HandleException;

    HandleValue[] resolveHandle(byte[] handle) throws HandleException;

    int getTcpTimeout();

    void setSessionTracker(ClientSessionTracker sessionTracker);
}
