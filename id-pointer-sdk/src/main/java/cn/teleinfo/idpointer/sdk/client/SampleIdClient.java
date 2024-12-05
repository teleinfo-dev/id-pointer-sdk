package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.protocol.decoder.HandleDecoder;
import cn.teleinfo.idpointer.sdk.protocol.encoder.HandleEncoder;
import cn.teleinfo.idpointer.sdk.transport.RequestIdFactory;
import cn.teleinfo.idpointer.sdk.transport.RequestIdFactoryDefault;
import cn.teleinfo.idpointer.sdk.transport.ResponsePromise;
import cn.teleinfo.idpointer.sdk.transport.sample.MessagePromiseManager;
import cn.teleinfo.idpointer.sdk.transport.sample.SimpleMessageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SampleIdClient extends AbstractIdClient {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractIdClient.class);
    private final RequestIdFactory requestIdGenerate;
    private FixedChannelPool fixedChannelPool;

    /**
     * @param serverAddress 服务器地址
     * @param promiseTimeout 超时时间
     */
    public SampleIdClient(InetSocketAddress serverAddress, int promiseTimeout){
        this(serverAddress, promiseTimeout, 50);
    }

    /**
     * @param serverAddress 服务器地址
     * @param promiseTimeout 超时时间
     * @param maxConnections 最大连接数
     */
    public SampleIdClient(InetSocketAddress serverAddress, int promiseTimeout, int maxConnections) {
        super(serverAddress, promiseTimeout, null, null, false);
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();

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
                .remoteAddress(getServerAddress())
        ;

        this.fixedChannelPool = new FixedChannelPool(bootstrap, channelPoolHandler, maxConnections);
    }

    public SampleIdClient(InetSocketAddress serverAddress, int promiseTimeout, AuthenticationInfo authenticationInfo, boolean encrypt) {
        super(serverAddress, promiseTimeout, authenticationInfo, new LoginInfoPoolKey(serverAddress, new IdUserId(Util.decodeString(authenticationInfo.getUserIdHandle()), authenticationInfo.getUserIdIndex())), encrypt);
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();
    }

    @Override
    protected AbstractResponse doRequest(AbstractRequest request) throws IDException {
        int nextInteger = requestIdGenerate.getNextInteger();
        request.requestId = nextInteger;

        Future<Channel> channelFuture = fixedChannelPool.acquire();
        Channel channel = null;
        try {
            channel = channelFuture.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IDException(IDException.CHANNEL_GET_ERROR, "Can't get channel from pool", e);
        }

        try {

            Promise<AbstractResponse> promise = new DefaultPromise<>(channel.eventLoop());
            channel.attr(MessagePromiseManager.PROMISE_ATTRIBUTE_KEY).set(promise);

            // Send a request
            channel.writeAndFlush(request);

            AbstractResponse response = promise.sync().get(getPromiseTimeout(), TimeUnit.SECONDS);

            if (response.responseCode != AbstractMessage.RC_SUCCESS && response.responseCode != AbstractMessage.RC_AUTHENTICATION_NEEDED) {
                throw new IDException("request error,response is {}", response);
            }

            return response;

        } catch (InterruptedException e) {
            log.warn("Interrupted", e);
            throw new RuntimeException(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            if (channel != null) {
                fixedChannelPool.release(channel);
            }
        }
    }

    @Override
    protected ResponsePromise doRequestInternal(AbstractRequest request) throws IDException {
        throw new IDException(IDException.CLIENT_ERROR, "Not implemented");
    }

    @Override
    public void close() throws IOException {
        fixedChannelPool.close();
    }
}
