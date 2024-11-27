package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.AbstractRequest;
import cn.teleinfo.idpointer.sdk.core.AbstractResponse;
import cn.teleinfo.idpointer.sdk.core.AuthenticationInfo;
import cn.teleinfo.idpointer.sdk.core.Util;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.protocol.decoder.HandleDecoder;
import cn.teleinfo.idpointer.sdk.protocol.encoder.HandleEncoder;
import cn.teleinfo.idpointer.sdk.transport.*;
import cn.teleinfo.idpointer.sdk.transport.sample.SimpleMessageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SampleIdClient extends AbstractIdClient{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractIdClient.class);
    private final RequestIdFactory requestIdGenerate;
    private final NioEventLoopGroup group = new NioEventLoopGroup();

    public SampleIdClient(InetSocketAddress serverAddress, int promiseTimeout) {
        super(serverAddress, promiseTimeout, null, null, false);
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();
    }

    public SampleIdClient(InetSocketAddress serverAddress, int promiseTimeout, AuthenticationInfo authenticationInfo, boolean encrypt) {
        super(serverAddress, promiseTimeout, authenticationInfo, new LoginInfoPoolKey(serverAddress, new IdUserId(Util.decodeString(authenticationInfo.getUserIdHandle()), authenticationInfo.getUserIdIndex())), encrypt);
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();
    }

    @Override
    protected AbstractResponse doRequest(AbstractRequest request) throws IDException {
        int nextInteger = requestIdGenerate.getNextInteger();
        request.requestId = nextInteger;
        SimpleMessageHandler messageHandler = new SimpleMessageHandler();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) {
                            channel.pipeline().addLast("encoder", new HandleEncoder());
                            channel.pipeline().addLast("decoder", new HandleDecoder());
                            channel.pipeline().addLast(messageHandler);
                        }
                    });

            // Connect to the server
            ChannelFuture future = bootstrap.connect(getServerAddress()).sync();
            Channel channel = future.channel();

            Promise<AbstractResponse> promise = new DefaultPromise<>(channel.eventLoop());
            messageHandler.setPromise(promise);

            // Send a request
            channel.writeAndFlush(request);

            return promise.sync().get(getPromiseTimeout(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Interrupted", e);
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected ResponsePromise doRequestInternal(AbstractRequest request) throws IDException {
        return null;
    }

    @Override
    public void close() throws IOException {
        group.shutdownGracefully();
    }
}
