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

    AbstractResponse sendRequestToSite(AbstractRequest req, SiteInfo site, int preferredProtocol, ResponseMessageCallback callback) throws HandleException;

    void setCheckSignatures(boolean b);

    AbstractResponse processRequest(AbstractRequest req, ResponseMessageCallback callback) throws HandleException;

    AbstractResponse processRequest(AbstractRequest req) throws HandleException;

    HandleValue resolveValueReference(ValueReference valueReference) throws HandleException;

    HandleValue[] resolveHandle(byte[] handle) throws HandleException;

    int getTcpTimeout();

    void setSessionTracker(ClientSessionTracker sessionTracker);
}
