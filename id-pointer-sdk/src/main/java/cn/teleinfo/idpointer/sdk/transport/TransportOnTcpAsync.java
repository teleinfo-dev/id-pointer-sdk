package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.core.AbstractIdRequest;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class TransportOnTcpAsync extends TransportOnTcp {
    public TransportOnTcpAsync(ChannelPoolMap<InetSocketAddress, TimedChannelPool> idChannelPoolMap, MessageManager messageManager) {
        super(idChannelPoolMap, messageManager);
    }

    @Override
    public ResponsePromise process(AbstractIdRequest request, InetSocketAddress inetSocketAddress) throws IDException {
        ChannelPool fixedChannelPool = getIdChannelPoolMap().get(inetSocketAddress);
        Future<Channel> channelFuture = fixedChannelPool.acquire();
        Channel channel = null;
        try {
            channel = channelFuture.get();
            return getMessageManager().process(request, channel);
        } catch (ExecutionException | InterruptedException e) {
            throw new IDException(IDException.CHANNEL_GET_ERROR, "channel get error", e);
        } finally {
            if (channel != null) {
                fixedChannelPool.release(channel);
            }
        }
    }
}
