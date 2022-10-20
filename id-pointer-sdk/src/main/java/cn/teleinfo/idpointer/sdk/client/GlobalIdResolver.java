package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;

public class GlobalIdResolver implements IDResolver{

    private ValueHelper valueHelper = ValueHelper.getInstance();

    @Override
    public HandleValue[] resolveHandle(String handle) throws IDException {
        String prefix = valueHelper.getPrefix(handle);
        IDClient idClient = GlobalIdClientFactory.getClientFactory().newInstance(prefix);
        return idClient.resolveHandle(handle);
    }

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes, boolean auth) throws IDException {
        String prefix = valueHelper.getPrefix(handle);
        IDClient idClient = GlobalIdClientFactory.getClientFactory().newInstance(prefix);
        return idClient.resolveHandle(handle,types,indexes,auth);
    }

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes) throws IDException {
        String prefix = valueHelper.getPrefix(handle);
        IDClient idClient = GlobalIdClientFactory.getClientFactory().newInstance(prefix);
        return idClient.resolveHandle(handle,types,indexes);
    }

}
