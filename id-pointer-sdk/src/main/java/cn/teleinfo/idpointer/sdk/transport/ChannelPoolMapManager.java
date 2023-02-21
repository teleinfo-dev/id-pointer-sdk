package cn.teleinfo.idpointer.sdk.transport;


import cn.teleinfo.idpointer.sdk.client.LoginInfo;
import cn.teleinfo.idpointer.sdk.protocol.decoder.HandleDecoder;
import cn.teleinfo.idpointer.sdk.protocol.encoder.HandleEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;

/**
 * ChannelPoolMapManager
 */

public class ChannelPoolMapManager {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ChannelPoolMapManager.class);
    private final MessageManager messageManager;
    private final AbstractChannelPoolMap<InetSocketAddress, TimedChannelPool> channelPoolMap;
    private final AbstractChannelPoolMap<LoginInfo, TimedChannelPool> loginChannelPoolMap;
    private final EventLoopGroup eventLoopGroup;
    private final int noLoginIdleTimeSeconds;
    private final int loginIdleTimeSeconds;

    public ChannelPoolMapManager(int nThreads, int minConnectionsPerServer, int maxConnectionsPerServer) {
        // @format off
        this(nThreads, 60,
                ChannelPoolConfig.builder()
                .idleTimeSeconds(600)
                .heatBeatRunning(false)
                .minConnectionsPerServer(minConnectionsPerServer)
                .maxConnectionsPerServer(maxConnectionsPerServer)
                .build(),
                ChannelPoolConfig.builder()
                .idleTimeSeconds(600)
                .heatBeatRunning(false)
                .minConnectionsPerServer(minConnectionsPerServer)
                .maxConnectionsPerServer(maxConnectionsPerServer)
                .build());
        // @format on
    }

    public ChannelPoolMapManager(int nioThreads, int promiseTimeout, ChannelPoolConfig defaultPoolConfig, ChannelPoolConfig loginPoolConfig) {
        this.messageManager = new MessageManagerImpl(promiseTimeout);
        this.eventLoopGroup = new NioEventLoopGroup(nioThreads);
        this.noLoginIdleTimeSeconds = defaultPoolConfig.getIdleTimeSeconds();
        this.loginIdleTimeSeconds = loginPoolConfig.getIdleTimeSeconds();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        // bootstrap.option(ChannelOption.SO_SNDBUF, 1024);
        // bootstrap.option(ChannelOption.MAX_MESSAGES_PER_WRITE, 1);
        // bootstrap.option(ChannelOption.WRITE_SPIN_COUNT, 1);
        // .option(ChannelOption.SO_BACKLOG, 1024);
        // .option(ChannelOption.SO_RCVBUF, 1024);

        MessageHandler messageHandler = new MessageHandler(messageManager);

        // 非登录ChannelPoolHandler
        ChannelPoolHandler channelPoolHandler = new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel channel) throws Exception {
                channel.pipeline().addLast(new IdleStateHandler(0, 0, defaultPoolConfig.getIdleTimeSeconds()));
                channel.pipeline().addLast("encoder", new HandleEncoder());
                channel.pipeline().addLast("decoder", new HandleDecoder());
                if (defaultPoolConfig.isHeatBeatRunning()) {
                    channel.pipeline().addLast(new IdleHeartBeatHandler(messageManager));
                } else {
                    channel.pipeline().addLast(new IdleChannelCloseHandler());
                }
                channel.pipeline().addLast(messageHandler);
            }
        };

        channelPoolMap = new ChannelPoolMapDefault(bootstrap, channelPoolHandler, defaultPoolConfig.getMinConnectionsPerServer(), defaultPoolConfig.getMaxConnectionsPerServer());

        // 登录ChannelPoolHandler
        ChannelPoolHandler loginChannelPoolHandler = new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel channel) throws Exception {
                channel.pipeline().addLast(new IdleStateHandler(0, 0, loginPoolConfig.getIdleTimeSeconds()));
                channel.pipeline().addLast("encoder", new HandleEncoder());
                channel.pipeline().addLast("decoder", new HandleDecoder());
                if (loginPoolConfig.isHeatBeatRunning()) {
                    channel.pipeline().addLast(new IdleHeartBeatHandler(messageManager));
                } else {
                    channel.pipeline().addLast(new IdleChannelCloseHandler());
                }
                channel.pipeline().addLast(messageHandler);
            }
        };

        loginChannelPoolMap = new ChannelPoolMapLogin(bootstrap, loginChannelPoolHandler, loginPoolConfig.getMinConnectionsPerServer(), loginPoolConfig.getMaxConnectionsPerServer());

        // 断开长时间不用的连接池
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                clearChannelPool();
                clearLoginChannelPool();
            }
        }, "ChannelPoolClean");
        thread.setDaemon(true);
        thread.start();

    }

    public AbstractChannelPoolMap<InetSocketAddress, TimedChannelPool> getChannelPoolMap() {
        return channelPoolMap;
    }

    public AbstractChannelPoolMap<LoginInfo, TimedChannelPool> getLoginChannelPoolMap() {
        return loginChannelPoolMap;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    public void close() throws IOException {
        channelPoolMap.close();
        loginChannelPoolMap.close();
        getMessageManager().close();
        eventLoopGroup.shutdownGracefully();
    }

    private void clearChannelPool() {
        Iterator<Map.Entry<InetSocketAddress, TimedChannelPool>> iterator = channelPoolMap.iterator();
        long currentTimeMillis = System.currentTimeMillis();
        long noLoginIdleTime = (noLoginIdleTimeSeconds + 10) * 1000;
        Map.Entry<InetSocketAddress, TimedChannelPool> next = null;
        TimedChannelPool timedChannelPool = null;
        while (iterator.hasNext()) {
            next = iterator.next();
            timedChannelPool = next.getValue();
            if (currentTimeMillis - timedChannelPool.getLastActiveTime() > noLoginIdleTime) {
                if (timedChannelPool.acquiredChannelCount() <= 0) {
                    channelPoolMap.remove(next.getKey());
                }
            }
        }
    }

    private void clearLoginChannelPool() {
        Iterator<Map.Entry<LoginInfo, TimedChannelPool>> iterator = loginChannelPoolMap.iterator();
        long currentTimeMillis = System.currentTimeMillis();
        long noLoginIdleTime = (loginIdleTimeSeconds + 10) * 1000;
        Map.Entry<LoginInfo, TimedChannelPool> next = null;
        TimedChannelPool timedChannelPool = null;
        while (iterator.hasNext()) {
            next = iterator.next();
            timedChannelPool = next.getValue();
            if (currentTimeMillis - timedChannelPool.getLastActiveTime() > noLoginIdleTime) {
                if (timedChannelPool.acquiredChannelCount() <= 0) {
                    loginChannelPoolMap.remove(next.getKey());
                }
            }
        }
    }
}
