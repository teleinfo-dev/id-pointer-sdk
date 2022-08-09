package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;

public interface IDResolver {

    public HandleValue[] resolveHandle(String handle) throws IDException;

    /**
     * Resolves a handle and returns a set of handle values that satisfy the
     * type filter specified. If the resolution is to retrieve all handle
     * values, specify null for both filter and indexes. If the administrative
     * priveleges are applicable, the restricted values will also be returned.
     *
     * @param handle
     *            The value of the handle to resolve
     * @param types
     *            The types of the handle values that we are looking for.
     * @param auth Whether to perform an authoritative resolution
     * @exception IDException Describes
     *                the error in resolution
     */
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes, boolean auth) throws IDException;

    /**
     * Resolves a handle and returns a set of handle values that satisfy the
     * type filter specified. If the resolution is to retrieve all handle
     * values, specify null for both filter and indexes. If the administrative
     * priveleges are applicable, the restricted values will also be returned.
     * Also, the resolution request is not authoritative.
     *
     * @param handle
     *            The value of the handle to resolve
     * @param types
     *            The types of the handle values that we are looking for.
     * @exception IDException Describes
     *                the error in resolution
     */
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes) throws IDException;

}
