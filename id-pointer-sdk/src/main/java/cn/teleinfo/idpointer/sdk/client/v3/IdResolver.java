package cn.teleinfo.idpointer.sdk.client.v3;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;

/**
 * The interface for the ID Resolver. resolve a handle.
 *
 */
public interface IdResolver {

    public HandleValue[] resolveHandle(String handle) throws IDException;

    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes) throws IDException;

}
