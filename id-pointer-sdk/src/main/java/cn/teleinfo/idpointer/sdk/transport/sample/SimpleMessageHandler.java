package cn.teleinfo.idpointer.sdk.transport.sample;

import cn.teleinfo.idpointer.sdk.core.AbstractResponse;
import cn.teleinfo.idpointer.sdk.transport.MessageHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;

public class SimpleMessageHandler extends SimpleChannelInboundHandler<AbstractResponse> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SimpleMessageHandler.class);
    private Promise<AbstractResponse> promise;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractResponse response) throws Exception {
        Channel channel = ctx.channel();
        log.info("{} received for requestId: {}",channel.remoteAddress(), response.requestId);
        promise.setSuccess(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 异常时设置 Promise 失败
        if (promise != null) {
            promise.setFailure(cause);
        }
        ctx.close();
    }

    public void setPromise(Promise<AbstractResponse> promise) {
        this.promise = promise;
    }
}
