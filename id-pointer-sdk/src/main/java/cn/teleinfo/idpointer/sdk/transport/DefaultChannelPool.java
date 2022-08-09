package cn.teleinfo.idpointer.sdk.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DefaultChannelPool extends AbstractFixedChannelPool {

    private final int minConnections;
    private final AtomicInteger channelCount = new AtomicInteger(0);

    public DefaultChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler, ChannelHealthChecker healthCheck, AbstractFixedChannelPool.AcquireTimeoutAction action, long acquireTimeoutMillis, int minConnections, int maxConnections, int maxPendingAcquires, boolean releaseHealthCheck) {
        this(bootstrap, handler, healthCheck, action, acquireTimeoutMillis, minConnections, maxConnections, maxPendingAcquires, releaseHealthCheck, false);
    }

    public DefaultChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler, ChannelHealthChecker healthCheck, AbstractFixedChannelPool.AcquireTimeoutAction action, long acquireTimeoutMillis, int minConnections, int maxConnections, int maxPendingAcquires, boolean releaseHealthCheck, boolean lastRecentUsed) {
        super(bootstrap, handler, healthCheck, action, acquireTimeoutMillis, maxConnections, maxPendingAcquires, releaseHealthCheck, lastRecentUsed);
        this.minConnections = minConnections;
    }

    @Override
    protected void acquireInternal(Promise<Channel> promise) {
        if (channelCount.get() <= minConnections) {
            synchronized (this) {
                if (channelCount.get() < minConnections) {
                    acquireNew(promise);
                } else {
                    acquireHealthyFromPoolOrNew(promise);
                }
            }
        } else {
            acquireHealthyFromPoolOrNew(promise);
        }
    }

    @Override
    protected void notifyConnect(ChannelFuture future, Promise<Channel> promise) throws Exception {

        if (future.isSuccess()) {
            int count = channelCount.incrementAndGet();
            Channel channel = future.channel();

            log.info("{} pool channel {} create, pool count is:{}",channel.remoteAddress(),channel.localAddress(), count);
            channel.closeFuture().addListener(closeFuture -> {
                if (closeFuture.isSuccess()) {
                    int countAfterClose = channelCount.decrementAndGet();
                    log.info("{} pool channel {} close, pool count is:{}",channel.remoteAddress(),channel.localAddress(), countAfterClose);
                }
            });
        }

        super.notifyConnect(future, promise);
    }
}