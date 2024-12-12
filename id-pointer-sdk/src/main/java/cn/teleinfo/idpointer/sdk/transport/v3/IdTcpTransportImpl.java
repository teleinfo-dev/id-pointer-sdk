/**
 * Copyright (C) 2024 teleinfo caict
 * All rights reserved.
 * <p>
 * 版权所有（C）2024 teleinfo caict
 */
package cn.teleinfo.idpointer.sdk.transport.v3;

import cn.teleinfo.idpointer.sdk.client.v3.IdRequest;
import cn.teleinfo.idpointer.sdk.client.v3.IdResponse;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.protocol.decoder.HandleDecoder;
import cn.teleinfo.idpointer.sdk.protocol.encoder.HandleEncoder;
import cn.teleinfo.idpointer.sdk.transport.sample.SimpleMessageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @Author: abluepoint
 * @Email: lilong@teleinfo.cn
 * @Create: 2024/12/11
 * @Description: IdTcpTransportImpl -
 */
public class IdTcpTransportImpl implements IdTcpTransport {
    private final ChannelPool channelPool;

    public IdTcpTransportImpl(InetSocketAddress serverAddress, int maxConnections) {
        SimpleMessageHandler messageHandler = new SimpleMessageHandler();

        ChannelPoolHandler channelPoolHandler = new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel ch) throws Exception {
                ch.pipeline().addLast(new HandleEncoder());
                ch.pipeline().addLast(new HandleDecoder());
                ch.pipeline().addLast(messageHandler);
            }

        };

        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .remoteAddress(serverAddress)
        ;

        this.channelPool = new FixedChannelPool(bootstrap, channelPoolHandler, maxConnections);
    }


    @Override
    public ChannelPool getChannelPool() {
        return channelPool;
    }

    @Override
    public IdPromise<IdResponse> send(IdRequest request) throws IDException {
        Future<Channel> channelFuture = channelPool.acquire();
        Channel channel = null;
        try {
            channel = channelFuture.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IDException(IDException.CHANNEL_GET_ERROR, "Can't get channel from pool", e);
        }

        return send(request, channelPool, channel);
    }

    @Override
    public IdPromise<IdResponse> send(IdRequest request, ChannelPool channelPoolUsed, Channel channel) throws IDException {

        IdPromise<IdResponse> promise = new IdPromiseDefault<>(channel.eventLoop(), channelPoolUsed, channel);
        channel.attr(IdTcpTransport.PROMISE_ATTRIBUTE_KEY).set(promise);
        channel.writeAndFlush(request);
        return promise;
    }

    @Override
    public void close() {
        channelPool.close();
    }
}
