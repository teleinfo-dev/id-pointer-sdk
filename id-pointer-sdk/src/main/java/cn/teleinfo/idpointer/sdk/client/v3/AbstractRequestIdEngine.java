/**
 * Copyright (C) 2024 teleinfo caict
 * All rights reserved.
 * <p>
 * 版权所有（C）2024 teleinfo caict
 */
package cn.teleinfo.idpointer.sdk.client.v3;

import cn.teleinfo.idpointer.sdk.client.ValueHelper;
import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.transport.v3.IdPromise;
import io.netty.util.concurrent.Promise;

import java.nio.charset.StandardCharsets;

/**
 * @Author: abluepoint
 * @Email: lilong@teleinfo.cn
 * @Create: 2024/12/12
 * @Description: AbstractRequestIdEngine -
 */
public abstract class AbstractRequestIdEngine extends AbstractIdEngine{

    private final ValueHelper valueHelper = ValueHelper.getInstance();
    @Override
    public IdPromise<IdResponse> resolveHandle(String handle, String[] types, int[] indexes) throws IDException {
        byte[][] reqTypes = valueHelper.getTypeStringBytes(types);
        ResolutionIdRequest request = new ResolutionIdRequest(handle.getBytes(StandardCharsets.UTF_8), reqTypes, indexes, null);
        return doRequestAsync(request);
    }

    @Override
    public IdPromise<IdResponse> createHandle(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        CreateHandleIdRequest request = new CreateHandleIdRequest(Util.encodeString(handle), values, null);
        request.overwriteWhenExists = overwrite;
        return doRequestAsync(request);
    }

    @Override
    public IdPromise<IdResponse> deleteHandle(String handle) throws IDException {
        return null;
    }

    @Override
    public IdPromise<IdResponse> addHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        AddValueIdRequest request = new AddValueIdRequest(Util.encodeString(handle), values, null);
        request.overwriteWhenExists = overwrite;
        return doRequestAsync(request);
    }

    @Override
    public IdPromise<IdResponse> updateHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        DeleteHandleIdRequest request = new DeleteHandleIdRequest(handle.getBytes(StandardCharsets.UTF_8), null);
        return doRequestAsync(request);
    }

    @Override
    public IdPromise<IdResponse> deleteHandleValues(String handle, int[] indexes) throws IDException {
        RemoveValueIdRequest request = new RemoveValueIdRequest(handle.getBytes(StandardCharsets.UTF_8), indexes, null);
        return doRequestAsync(request);
    }

    @Override
    public IdPromise<IdResponse> homeNa(String na) throws IDException {
        HomeNaIdRequest request = new HomeNaIdRequest(na.getBytes(StandardCharsets.UTF_8), null);
        return doRequestAsync(request);
    }

    @Override
    public IdPromise<IdResponse> unhomeNa(String na) throws IDException {
        UnhomeNaIdRequest request = new UnhomeNaIdRequest(na.getBytes(StandardCharsets.UTF_8), null);
        return doRequestAsync(request);
    }

    protected abstract IdPromise<IdResponse> doRequestAsync(IdRequest request) throws IDException;
}
