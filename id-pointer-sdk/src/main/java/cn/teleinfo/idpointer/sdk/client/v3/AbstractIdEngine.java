package cn.teleinfo.idpointer.sdk.client.v3;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.transport.v3.IdPromise;
import io.netty.util.concurrent.Promise;


public abstract class AbstractIdEngine implements IdEngine{

    @Override
    public IdPromise<IdResponse> resolveHandle(String handle) throws IDException {
        return resolveHandle(handle, null, null);
    }

    @Override
    public IdPromise<IdResponse> createHandle(String handle, HandleValue[] values) throws IDException {
        return createHandle(handle, values, false);
    }

    @Override
    public IdPromise<IdResponse> updateHandleValues(String handle, HandleValue[] values) throws IDException {
        return updateHandleValues(handle, values, false);
    }

    @Override
    public IdPromise<IdResponse> deleteHandleValues(String handle, HandleValue[] values) throws IDException {
        int[] indexes = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            indexes[i] = values[i].getIndex();
        }
        return deleteHandleValues(handle, indexes);
    }

}
