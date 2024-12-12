package cn.teleinfo.idpointer.sdk.transport.v3;

import cn.teleinfo.idpointer.sdk.client.v3.IdRequest;
import cn.teleinfo.idpointer.sdk.client.v3.IdResponse;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.session.SessionDefault;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Promise;

import java.io.Closeable;

public interface IdTcpTransport {

    public static AttributeKey<Promise<IdResponse>> PROMISE_ATTRIBUTE_KEY = AttributeKey.valueOf("promise");
    public static AttributeKey<SessionDefault> SESSION_KEY = AttributeKey.newInstance("SESSION");


    public ChannelPool getChannelPool();

    public IdPromise<IdResponse> send(IdRequest request) throws IDException;

    IdPromise<IdResponse> send(IdRequest request, ChannelPool channelPoolUsed, Channel channel) throws IDException;

    public void close();
}
