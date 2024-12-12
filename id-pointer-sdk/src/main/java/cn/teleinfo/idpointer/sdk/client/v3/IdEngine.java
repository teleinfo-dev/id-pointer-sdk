package cn.teleinfo.idpointer.sdk.client.v3;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.transport.v3.IdPromise;
import io.netty.util.concurrent.Promise;

/**
 * The interface for the ID Engine. connect to one ID server. operations on handles.
 *
 * @author bluepoint
 * @version 3.0
 */
public interface IdEngine {

    IdPromise<IdResponse> resolveHandle(String handle) throws IDException;

    /**
     * Resolve a handle.
     * @param handle The handle to resolve.
     * @param types  The types to resolve.
     * @param indexes The indexes to resolve.
     * @return The response.
     * @throws IDException If an error occurs.
     */
    public IdPromise<IdResponse> resolveHandle(String handle, String[] types, int[] indexes) throws IDException;

    /**
     * Create a handle.
     * @param handle
     * @param values
     * @return
     * @throws IDException
     */
    public IdPromise<IdResponse> createHandle(String handle, HandleValue[] values) throws IDException;

    IdPromise<IdResponse> createHandle(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    /**
     * Delete a handle.
     * @param handle
     * @return
     * @throws IDException
     */
    public IdPromise<IdResponse> deleteHandle(String handle) throws IDException;

    /**
     * @param handle
     * @param values
     * @param overwrite
     * @return
     * @throws IDException
     */
    public IdPromise<IdResponse> addHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    IdPromise<IdResponse> updateHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    /**
     * @author bluepoint
     * @description 更新handle的值
     * @param handle
     * @param values
     * @return
     * @throws IDException
     */
    public IdPromise<IdResponse> updateHandleValues(String handle, HandleValue[] values) throws IDException;

    public IdPromise<IdResponse> deleteHandleValues(String handle, HandleValue[] values) throws IDException;

    public IdPromise<IdResponse> deleteHandleValues(String handle, int[] indexes) throws IDException;

    public IdPromise<IdResponse> homeNa(String na) throws IDException;

    public IdPromise<IdResponse> unhomeNa(String na) throws IDException;

}
