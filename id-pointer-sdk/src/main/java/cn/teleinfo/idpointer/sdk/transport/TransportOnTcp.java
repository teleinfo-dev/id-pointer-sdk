package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.core.AbstractRequest;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class TransportOnTcp {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TransportOnTcp.class);
    private final ChannelPoolMap<InetSocketAddress, TimedChannelPool> idChannelPoolMap;
    private final MessageManager messageManager;

    public TransportOnTcp(ChannelPoolMap<InetSocketAddress, TimedChannelPool> idChannelPoolMap, MessageManager messageManager) {
        this.idChannelPoolMap = idChannelPoolMap;
        this.messageManager = messageManager;
    }

    public ResponsePromise process(AbstractRequest request, InetSocketAddress inetSocketAddress) throws IDException {

        TimedChannelPool fixedChannelPool = idChannelPoolMap.get(inetSocketAddress);
        fixedChannelPool.setLastActiveTime(System.currentTimeMillis());

        log.debug("fixedChannelPool: {}",fixedChannelPool);
        Future<Channel> channelFuture = fixedChannelPool.acquire();
        Channel channel = null;
        try {
            channel = channelFuture.get();
            ResponsePromise promise = messageManager.process(request, channel);

            final Channel finalChannel = channel;

            promise.addListener(future -> {
                // 使用监听器处理
                if (future.isDone()) {
                    if (finalChannel != null) {
                        fixedChannelPool.release(finalChannel);
                    }
                }
            });
            return promise;
        } catch (ExecutionException | InterruptedException e) {
            throw new IDException(IDException.CHANNEL_GET_ERROR, "channel get error", e);
        }
    }

    public ChannelPoolMap<InetSocketAddress, TimedChannelPool> getIdChannelPoolMap() {
        return idChannelPoolMap;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
