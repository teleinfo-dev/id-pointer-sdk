/**
 * Copyright (C) 2024 teleinfo caict
 * All rights reserved.
 * <p>
 * 版权所有（C）2024 teleinfo caict
 */
package cn.teleinfo.idpointer.sdk.client.v3;

import cn.teleinfo.idpointer.sdk.core.HandleException;
import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.core.ResolutionIdResponse;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.exception.IDRuntimeException;
import cn.teleinfo.idpointer.sdk.transport.v3.IdPromise;
import cn.teleinfo.idpointer.sdk.util.ResponseUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @Author: abluepoint
 * @Email: lilong@teleinfo.cn
 * @Create: 2024/12/12
 * @Description: AbstractIdClient -
 */
public abstract class AbstractIdClient implements IdClient {
    private final IdEngine idEngine;

    public AbstractIdClient(IdEngine idEngine) {
        this.idEngine = idEngine;
    }

    @Override
    public void createHandle(String handle, HandleValue[] values) throws IDException {
        IdPromise<IdResponse> promise = idEngine.createHandle(handle, values);
        doReceiveResponse(promise);
    }



    @Override
    public void deleteHandle(String handle) throws IDException {
        IdPromise<IdResponse> promise = idEngine.deleteHandle(handle);
        doReceiveResponse(promise);
    }

    @Override
    public void createHandle(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        IdPromise<IdResponse> promise = idEngine.createHandle(handle, values, overwrite);
        doReceiveResponse(promise);
    }


    @Override
    public void addHandleValues(String handle, HandleValue[] values) throws IDException {
        addHandleValues(handle, values, false);
    }

    @Override
    public void addHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        IdPromise<IdResponse> promise = idEngine.addHandleValues(handle, values, overwrite);
        doReceiveResponse(promise);
    }

    @Override
    public void deleteHandleValues(String handle, HandleValue[] values) throws IDException {
        IdPromise<IdResponse> promise = idEngine.deleteHandleValues(handle, values);
        doReceiveResponse(promise);
    }

    @Override
    public void deleteHandleValues(String handle, int[] indexes) throws IDException {
        IdPromise<IdResponse> promise = idEngine.deleteHandleValues(handle, indexes);
        doReceiveResponse(promise);
    }

    @Override
    public void updateHandleValues(String handle, HandleValue[] values) throws IDException {
        updateHandleValues(handle, values, false);
    }

    @Override
    public void updateHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        IdPromise<IdResponse> promise = idEngine.updateHandleValues(handle, values,overwrite);
        doReceiveResponse(promise);
    }

    @Override
    public void homeNa(String na) throws IDException {
        IdPromise<IdResponse> promise = idEngine.homeNa(na);
        doReceiveResponse(promise);
    }

    @Override
    public void unhomeNa(String na) throws IDException {
        IdPromise<IdResponse> promise = idEngine.unhomeNa(na);
        doReceiveResponse(promise);
    }

    @Override
    public HandleValue[] resolveHandle(String handle) throws IDException {
        return resolveHandle(handle, null, null);
    }

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes) throws IDException {
        IdPromise<IdResponse> promise = idEngine.resolveHandle(handle,types,indexes);
        IdResponse response = doReceiveResponse(promise);
        HandleValue[] hvs = null;
        if (response instanceof ResolutionIdResponse) {
            try {
                hvs = ((ResolutionIdResponse) response).getHandleValues();
                return hvs;
            } catch (HandleException e) {
                throw new IDException(IDException.RC_INVALID_RESPONSE_CODE, "Get handle value error ", response, e);
            }
        } else {
            throw new IDException(IDException.RC_INVALID_RESPONSE_CODE, "not resolution response", response);
        }
    }

    protected abstract IdResponse doReceiveResponse(IdPromise<IdResponse> promise) throws IDException;

    @Override
    public IdEngine getIdEngine() {
        return idEngine;
    }
}
