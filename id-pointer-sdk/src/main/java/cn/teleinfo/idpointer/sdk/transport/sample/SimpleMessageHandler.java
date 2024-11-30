package cn.teleinfo.idpointer.sdk.transport.sample;

import cn.teleinfo.idpointer.sdk.core.AbstractResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;

@ChannelHandler.Sharable
public class SimpleMessageHandler extends SimpleChannelInboundHandler<AbstractResponse> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SimpleMessageHandler.class);


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractResponse response) throws Exception {
        Channel channel = ctx.channel();
        log.info("{} received for requestId: {}",channel.remoteAddress(), response.requestId);

        Attribute<Promise<AbstractResponse>> attr = channel.attr(MessagePromiseManager.PROMISE_ATTRIBUTE_KEY);
        Promise<AbstractResponse> promise = attr.getAndSet(null);;
        if(promise != null) {
            promise.setSuccess(response);
        } else {
            log.error("promise is null for requestId: {}", response.requestId);
        }
    }


}
