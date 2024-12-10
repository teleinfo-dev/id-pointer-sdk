package cn.teleinfo.idpointer.sdk.client.v3;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;

public interface IDResolver {

    public HandleValue[] resolveHandle(String handle) throws IDException;
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes) throws IDException;

}
