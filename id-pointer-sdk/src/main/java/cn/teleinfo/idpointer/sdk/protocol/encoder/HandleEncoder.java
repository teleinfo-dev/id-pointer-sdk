package cn.teleinfo.idpointer.sdk.protocol.encoder;

import cn.teleinfo.idpointer.sdk.core.AbstractMessage;
import cn.teleinfo.idpointer.sdk.core.AbstractRequest;
import cn.teleinfo.idpointer.sdk.transport.MessagePacketsManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;

public class HandleEncoder extends MessageToByteEncoder<AbstractMessage> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HandleEncoder.class);
    private MessagePacketsManager messagePacketsManager = new MessagePacketsManager();

    @Override
    protected void encode(ChannelHandlerContext ctx, AbstractMessage req, ByteBuf out) throws Exception {
        log.debug("==> {}:{}", req.requestId, req);

        ByteBuf byteBuf = messagePacketsManager.getTcpMessageEnvelopeForRequest(req);

        // ByteBuf byteBuf1 = Unpooled.wrappedBuffer(byteBuf);
        // log.info("==> requestId[{}]:{},[{}]",req.requestId, req);

        out.writeBytes(Unpooled.wrappedBuffer(byteBuf));
    }
}
