package cn.teleinfo.idpointer.sdk.protocol.decoder;

import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.transport.MessagePackets;
import cn.teleinfo.idpointer.sdk.transport.MessagePacketsManager;
import cn.teleinfo.idpointer.sdk.transport.ResponsePromise;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;

import java.util.List;

public class HandleDecoder extends ByteToMessageDecoder {


    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HandleDecoder.class);
    private MessagePacketsManager messagePacketsManager;

    public HandleDecoder() {

    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Object decoded = decode(ctx, in);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (in.readableBytes() < Common.MESSAGE_ENVELOPE_SIZE) {
            return null;
        } else {
            in.markReaderIndex();

            // 打印数据
            // byte[] messageBuf1 = new byte[in.readableBytes()];
            // in.readBytes(messageBuf1);
            // System.err.println(ByteBufUtil.hexDump(messageBuf1));
            // in.resetReaderIndex();

            // todo ll:只检查长度?
            MessageEnvelope rcvEnvelope = new MessageEnvelope();
            byte[] envBuf = new byte[Common.MESSAGE_ENVELOPE_SIZE];
            in.readBytes(envBuf);
            Encoder.decodeEnvelope(envBuf, rcvEnvelope);

            if (in.readableBytes() < rcvEnvelope.messageLength) {
                in.resetReaderIndex();
                return null;
            }
            byte[] messageBuf = new byte[rcvEnvelope.messageLength];
            in.readBytes(messageBuf);

            if (rcvEnvelope.truncated) {
                //消息分消息信封传传递
                MessagePackets messagePackets = messagePacketsManager.getMessagePackets(rcvEnvelope);

                if (messagePackets == null) {
                    messagePackets = new MessagePackets(rcvEnvelope.messageLength);
                }

                byte[] dataArray = new byte[in.readableBytes()];
                in.readBytes(dataArray);

                messagePackets.receivePacket(rcvEnvelope, dataArray);

                if (messagePackets.isReceiveCompleted()) {
                    AbstractMessage message = Encoder.decodeMessage(messagePackets.getMessageBytes(), 0, rcvEnvelope);
                    return message;
                }
                return null;
            } else {
                AbstractMessage message = Encoder.decodeMessage(messageBuf, 0, rcvEnvelope);
                log.debug("<== {}:{}",message.requestId, message);
                return message;
            }
        }
    }

}
