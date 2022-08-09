package cn.teleinfo.idpointer.sdk.transport;


import cn.teleinfo.idpointer.sdk.protocol.decoder.HandleDecoder;
import cn.teleinfo.idpointer.sdk.protocol.encoder.HandleEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ChannelPoolMapManager {

    private final MessageManager messageManager;
    private final AbstractChannelPoolMap<InetSocketAddress, ChannelPool> channelPoolMap;
    private final EventLoopGroup eventLoopGroup;

    public ChannelPoolMapManager(int nThreads, int minConnectionsPerServer, int maxConnectionsPerServer) {
        this(nThreads, minConnectionsPerServer, maxConnectionsPerServer, 60);
    }

    public ChannelPoolMapManager(int nioThreads, int minConnectionsPerServer, int maxConnectionsPerServer, int promiseTimeout) {
        this.messageManager = new MessageManagerImpl(promiseTimeout);
        this.eventLoopGroup = new NioEventLoopGroup(nioThreads);
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

        // todo: 参数化
        ChannelPoolHandler channelPoolHandler = new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel channel) throws Exception {
                channel.pipeline().addLast(new IdleStateHandler(0, 0, 900));
                channel.pipeline().addLast("encoder", new HandleEncoder());
                channel.pipeline().addLast("decoder", new HandleDecoder());
                channel.pipeline().addLast(new HeartBeatHandler(messageManager));
                channel.pipeline().addLast(messageHandler);
            }
        };

        channelPoolMap = new ChannelPoolMapDefault(bootstrap, channelPoolHandler, minConnectionsPerServer, maxConnectionsPerServer);
    }


    public AbstractChannelPoolMap<InetSocketAddress, ChannelPool> getChannelPoolMap() {
        return channelPoolMap;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    public void close() throws IOException {
        channelPoolMap.close();
        getMessageManager().close();
        eventLoopGroup.shutdownGracefully();
    }
}
