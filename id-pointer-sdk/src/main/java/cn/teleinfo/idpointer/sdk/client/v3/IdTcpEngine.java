/**
 * Copyright (C) 2024 teleinfo caict
 * All rights reserved.
 * <p>
 * 版权所有（C）2024 teleinfo caict
 */
package cn.teleinfo.idpointer.sdk.client.v3;

import cn.teleinfo.idpointer.sdk.core.AbstractIdResponse;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.transport.RequestIdFactoryDefault;
import cn.teleinfo.idpointer.sdk.transport.v3.IdPromise;
import cn.teleinfo.idpointer.sdk.transport.v3.IdTcpTransport;
import cn.teleinfo.idpointer.sdk.transport.v3.IdTcpTransportImpl;
import cn.teleinfo.idpointer.sdk.transport.v3.RequestIdFactory;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @Author: abluepoint
 * @Email: lilong@teleinfo.cn
 * @Create: 2024/12/12
 * @Description: IdTcpEngine -
 */
public class IdTcpEngine extends AbstractRequestIdEngine{

    public final IdTcpTransport transport;
    private final RequestIdFactory requestIdFactory;

    public IdTcpEngine(InetSocketAddress serverAddress, int maxConnections) {
        this.transport = new IdTcpTransportImpl(serverAddress, maxConnections);
        this.requestIdFactory = new RequestIdFactoryDefault();
    }

    @Override
    protected IdPromise<IdResponse> doRequestAsync(IdRequest request) throws IDException {
        // 设置requestId
        int nextInteger = requestIdFactory.getNextInteger();
        request.setRequestId(nextInteger);

        return transport.send(request);
    }
}
