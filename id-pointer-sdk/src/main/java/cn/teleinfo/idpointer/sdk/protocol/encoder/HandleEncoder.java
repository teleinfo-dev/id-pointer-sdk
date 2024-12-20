package cn.teleinfo.idpointer.sdk.protocol.encoder;

import cn.teleinfo.idpointer.sdk.core.AbstractMessage;
import cn.teleinfo.idpointer.sdk.transport.MessagePacketsManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

public class HandleEncoder extends MessageToByteEncoder<AbstractMessage> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HandleEncoder.class);
    private MessagePacketsManager messagePacketsManager = new MessagePacketsManager();

    @Override
    protected void encode(ChannelHandlerContext ctx, AbstractMessage req, ByteBuf out) throws Exception {

        Channel channel = ctx.channel();
        log.info("==> {} send requestId {},detail {}", channel.localAddress(), req.requestId, req);
        ByteBuf byteBuf = messagePacketsManager.getTcpMessageEnvelopeForRequest(req, channel);

        if(log.isDebugEnabled()){
            byteBuf.markReaderIndex();
            byte[] dataArray = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(dataArray);
            log.debug("req data ==> : {}", Hex.encodeHexString(dataArray));
            byteBuf.resetReaderIndex();
        }


        out.writeBytes(Unpooled.wrappedBuffer(byteBuf));

    }
}
