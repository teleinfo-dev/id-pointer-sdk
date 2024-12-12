/**
 * Copyright (C) 2024 teleinfo caict
 * All rights reserved.
 * <p>
 * 版权所有（C）2024 teleinfo caict
 */
package cn.teleinfo.idpointer.sdk.client.v3;

import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.exception.IDRuntimeException;
import cn.teleinfo.idpointer.sdk.transport.v3.IdPromise;
import cn.teleinfo.idpointer.sdk.util.ResponseUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @Author: abluepoint
 * @Email: lilong@teleinfo.cn
 * @Create: 2024/12/12
 * @Description: IdTcpClient -
 */
public class IdTcpClient extends AbstractIdClient {

    private final int promiseTimeout;

    public IdTcpClient(InetSocketAddress serverAddress, int promiseTimeout, int maxConnections) {
        super(new IdTcpEngine(serverAddress, maxConnections));
        this.promiseTimeout = promiseTimeout;
    }

    @Override
    protected IdResponse doReceiveResponse(IdPromise<IdResponse> promise) throws IDException {
        try {
            IdResponse response = promise.sync().get(promiseTimeout, TimeUnit.SECONDS);
            ResponseUtils.checkResponseCode(response);
            return response;
        } catch (ExecutionException | InterruptedException e) {
            throw new IDException(IDException.PROMISE_GET_ERROR, "Create handle error", e);
        } catch (TimeoutException e) {
            throw new IDRuntimeException(IDException.RESPONSE_TIMEOUT, "Create handle timeout", e);
        } finally {
            promise.release();
        }
    }

}
