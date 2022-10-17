package cn.teleinfo.idpointer.sdk.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;

import java.net.InetSocketAddress;

public class ChannelPoolMapLogined extends AbstractChannelPoolMap<InetSocketAddress, ChannelPool> {

    private final Bootstrap bootstrap;
    private final ChannelPoolHandler channelPoolHandler;
    private final int minConnectionsPerServer;
    private final int maxConnectionsPerServer;

    public ChannelPoolMapLogined(Bootstrap bootstrap, ChannelPoolHandler channelPoolHandler, int minConnectionsPerServer, int maxConnectionsPerServer) {
        this.bootstrap = bootstrap;
        this.channelPoolHandler = channelPoolHandler;
        this.minConnectionsPerServer = minConnectionsPerServer;
        this.maxConnectionsPerServer = maxConnectionsPerServer;
    }

    @Override
    protected ChannelPool newPool(InetSocketAddress key) {
        Bootstrap bootstrapPooled = bootstrap.clone();
        bootstrapPooled.remoteAddress(key);
        //return new FixedChannelPool(bootstrapPooled, channelPoolHandler, ChannelHealthChecker.ACTIVE, FixedChannelPool.AcquireTimeoutAction.FAIL, 5000L, maxConnectionsPerServer, 10000, true, false);
        return new DefaultChannelPool(bootstrapPooled, channelPoolHandler, ChannelHealthChecker.ACTIVE, AbstractFixedChannelPool.AcquireTimeoutAction.FAIL, 5000L, minConnectionsPerServer, maxConnectionsPerServer, 10000, true, false);
    }

}
