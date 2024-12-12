/**
 * Copyright (C) 2024 teleinfo caict
 * All rights reserved.
 * <p>
 * 版权所有（C）2024 teleinfo caict
 */
package cn.teleinfo.idpointer.sdk.transport.v3;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;

/**
 * @Author: abluepoint
 * @Email: lilong@teleinfo.cn
 * @Create: 2024/12/12
 * @Description: ResponsePromiseDefault -
 */
public class IdPromiseDefault<V> extends DefaultPromise<V> implements IdPromise<V> {

    private final ChannelPool channelPool;
    private final Channel channel;

    public IdPromiseDefault(EventExecutor executor, ChannelPool channelPool, Channel channel) {
        super(executor);
        this.channelPool = channelPool;
        this.channel = channel;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public void release() {
        channelPool.release(channel);
    }
}
