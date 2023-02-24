package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.client.LoginInfoPoolKey;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPoolHandler;

/**
 * 只是按照服务与用户区分连接池,本身没有集成登录功能
 */
public class ChannelPoolMapLogin extends AbstractChannelPoolMap<LoginInfoPoolKey, TimedChannelPool> {

    private final Bootstrap bootstrap;
    private final ChannelPoolHandler channelPoolHandler;
    private final int minConnectionsPerServer;
    private final int maxConnectionsPerServer;

    public ChannelPoolMapLogin(Bootstrap bootstrap, ChannelPoolHandler channelPoolHandler, int minConnectionsPerServer, int maxConnectionsPerServer) {
        this.bootstrap = bootstrap;
        this.channelPoolHandler = channelPoolHandler;
        this.minConnectionsPerServer = minConnectionsPerServer;
        this.maxConnectionsPerServer = maxConnectionsPerServer;
    }

    @Override
    protected TimedChannelPool newPool(LoginInfoPoolKey key) {
        Bootstrap bootstrapPooled = bootstrap.clone();
        bootstrapPooled.remoteAddress(key.getAddress());
        return new DefaultChannelPool(bootstrapPooled, channelPoolHandler, ChannelHealthChecker.ACTIVE, AbstractFixedChannelPool.AcquireTimeoutAction.FAIL, 5000L, minConnectionsPerServer, maxConnectionsPerServer, 10000, true, false);
    }




}
