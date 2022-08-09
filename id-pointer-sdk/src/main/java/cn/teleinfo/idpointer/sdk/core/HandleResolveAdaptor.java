package cn.teleinfo.idpointer.sdk.core;

import cn.teleinfo.idpointer.sdk.client.IDResolver;
import cn.teleinfo.idpointer.sdk.core.util.LRUCacheTable;
import cn.teleinfo.idpointer.sdk.exception.IDException;

public class HandleResolveAdaptor implements HandleResolverInterface {

    private IDResolver idResolver;

    public HandleResolveAdaptor(IDResolver idResolver) {
        this.idResolver = idResolver;
    }

    @Override
    public void setTcpTimeout(int timeout) {
        throw new RuntimeException("not impl");
    }

    @Override
    public void setPreferredProtocols(int[] ints) {
        throw new RuntimeException("not impl");
    }

    @Override
    public void setSiteFilter(SiteFilter keywordSiteFilter) {
        throw new RuntimeException("not impl");
    }

    @Override
    public void setUseIPv6FastFallback(boolean ipv6FastFallback) {
        throw new RuntimeException("not impl");
    }

    @Override
    public void setTraceMessages(boolean b) {
        throw new RuntimeException("not impl");
    }

    @Override
    public int[] getPreferredProtocols() {
        throw new RuntimeException("not impl");
    }

    @Override
    public LRUCacheTable<String, Long> getResponseTimeTbl() {
        throw new RuntimeException("not impl");
    }

    @Override
    public LRUCacheTable<String, Long> getPreferredPrimaryTbl() {
        throw new RuntimeException("not impl");
    }

    @Override
    public AbstractResponse sendRequestToSite(AbstractRequest req, SiteInfo site, int preferredProtocol, ResponseMessageCallback callback) throws HandleException {
        throw new RuntimeException("not impl");
    }

    @Override
    public void setCheckSignatures(boolean b) {
        throw new RuntimeException("not impl");
    }

    @Override
    public AbstractResponse processRequest(AbstractRequest req, ResponseMessageCallback callback) throws HandleException {
        throw new RuntimeException("not impl");
    }

    @Override
    public AbstractResponse processRequest(AbstractRequest req) throws HandleException {
        throw new RuntimeException("not impl");
    }

    @Override
    public HandleValue resolveValueReference(ValueReference valueReference) throws HandleException {
        try {
            HandleValue[] hvs = idResolver.resolveHandle(valueReference.getHandleAsString(), null, new int[]{valueReference.index});
            if(hvs.length==1){
                return hvs[0];
            }
        } catch (IDException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public HandleValue[] resolveHandle(byte[] handle) throws HandleException {
        try {
            HandleValue[] hvs = idResolver.resolveHandle(Util.decodeString(handle),null,null);
            return hvs;
        } catch (IDException e) {
            e.printStackTrace();
        }
        return new HandleValue[0];
    }

    @Override
    public int getTcpTimeout() {
        throw new RuntimeException("not impl");
    }

    @Override
    public void setSessionTracker(ClientSessionTracker sessionTracker) {
        throw new RuntimeException("not impl");
    }
}
