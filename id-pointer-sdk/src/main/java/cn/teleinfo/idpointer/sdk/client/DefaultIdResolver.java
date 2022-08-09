package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;

public class DefaultIdResolver implements IDResolver{

    private ValueHelper valueHelper = ValueHelper.getInstance();

    private final IDClientFactory idClientFactory;

    public DefaultIdResolver(IDClientFactory idClientFactory) {
        this.idClientFactory = idClientFactory;
    }

    @Override
    public HandleValue[] resolveHandle(String handle) throws IDException {
        String prefix = valueHelper.getPrefix(handle);
        IDClient idClient = idClientFactory.newInstance(prefix);
        return idClient.resolveHandle(handle);
    }

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes, boolean auth) throws IDException {
        String prefix = valueHelper.getPrefix(handle);
        IDClient idClient = idClientFactory.newInstance(prefix);
        return idClient.resolveHandle(handle,types,indexes,auth);
    }

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes) throws IDException {
        String prefix = valueHelper.getPrefix(handle);
        IDClient idClient = idClientFactory.newInstance(prefix);
        return idClient.resolveHandle(handle,types,indexes);
    }
}
