package cn.teleinfo.idpointer.sdk.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;

public class IdleChannelCloseHandler extends IdleHandler {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            ctx.fireChannelInactive();
            Channel channel = ctx.channel();
            channel.close();
        }
    }
}
