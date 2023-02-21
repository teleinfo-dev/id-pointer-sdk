package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;

public class DefaultIdResolver implements IDResolver {

    private ValueHelper valueHelper = ValueHelper.getInstance();

    private final IDClientFactory idClientFactory;

    public DefaultIdResolver(IDClientFactory idClientFactory) {
        this.idClientFactory = idClientFactory;
    }

    @Override
    public HandleValue[] resolveHandle(String handle) throws IDException {
        return resolveHandle(handle, null, null);
    }

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes) throws IDException {
        //如果是前缀,直接返回结果
        if (handle.indexOf("/") == -1 || handle.startsWith("0.NA/")) {
            HandleValue[] prefixHandleValues = GlobalIdClientFactory.getInstance().getPrefixHandleValues(handle, types, indexes);
            return prefixHandleValues;
        }
        String prefix = valueHelper.getPrefix(handle);
        //连接具体企业节点
        IDClient idClient = idClientFactory.newInstance(prefix);
        return idClient.resolveHandle(handle, types, indexes);
    }
}
