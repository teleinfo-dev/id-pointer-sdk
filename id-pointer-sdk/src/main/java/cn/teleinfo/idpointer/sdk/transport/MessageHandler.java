package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.core.AbstractResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;

@ChannelHandler.Sharable
public class MessageHandler extends SimpleChannelInboundHandler<AbstractResponse> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MessageHandler.class);
    private MessageManager messageManager;

    public MessageHandler(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable cause) throws Exception {
        channelHandlerContext.fireChannelInactive();
        log.warn("exception occurred,inactive channel", cause);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractResponse msg) throws Exception {
        Channel channel = ctx.channel();
        log.info("{} received for requestId: {}",channel.remoteAddress(), msg.requestId);
        ResponsePromise responsePromise = messageManager.getResponsePromise(msg.requestId);

        if (responsePromise != null) {
            responsePromise.setSuccess(msg);
        } else {
            log.warn("{} promise not found by requestId: {},msg is {}",channel.remoteAddress(), msg.requestId, msg);
        }
    }

    MessageManager getMessageManager() {
        return messageManager;
    }
}
