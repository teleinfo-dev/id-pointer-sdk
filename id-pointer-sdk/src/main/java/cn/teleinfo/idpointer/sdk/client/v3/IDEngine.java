package cn.teleinfo.idpointer.sdk.client.v3;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import io.netty.util.concurrent.Promise;

/**
 * The interface for the ID Engine. connect to one ID server. operations on handles.
 *
 * @author bluepoint
 * @version 3.0
 */
public interface IDEngine {

    /**
     * Resolve a handle.
     *
     * @param handle The handle to resolve.
     * @param types  The types to resolve.
     * @param indexes The indexes to resolve.
     * @return The response.
     * @throws IDException If an error occurs.
     */
    public Promise<Response> resolveHandle(String handle, String[] types, int[] indexes) throws IDException;

    /**
     * Create a handle.
     * @param handle
     * @param values
     * @return
     * @throws IDException
     */
    public Promise<Response> createHandle(String handle, HandleValue[] values) throws IDException;

    /**
     * Delete a handle.
     * @param handle
     * @return
     * @throws IDException
     */
    public Promise<Response> deleteHandle(String handle) throws IDException;

    public Promise<Response> addHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public Promise<Response> updateHandleValues(String handle, HandleValue[] values) throws IDException;

    public Promise<Response> deleteHandleValues(String handle, HandleValue[] values) throws IDException;

    public Promise<Response> deleteHandleValues(String handle, int[] indexes) throws IDException;

    public Promise<Response> homeNa(String na) throws IDException;

    public Promise<Response> unhomeNa(String na) throws IDException;

}
