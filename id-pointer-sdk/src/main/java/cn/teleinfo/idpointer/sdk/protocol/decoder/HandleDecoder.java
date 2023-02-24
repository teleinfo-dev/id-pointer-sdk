package cn.teleinfo.idpointer.sdk.protocol.decoder;

import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.security.HdlSecurityProvider;
import cn.teleinfo.idpointer.sdk.session.Session;
import cn.teleinfo.idpointer.sdk.transport.MessagePackets;
import cn.teleinfo.idpointer.sdk.transport.MessagePacketsManager;
import cn.teleinfo.idpointer.sdk.transport.ResponsePromise;
import cn.teleinfo.idpointer.sdk.transport.Transport;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

import javax.crypto.Cipher;
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

            // 当前只检查了长度
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

            if (rcvEnvelope.encrypted) {
                log.debug("encrypted: {}",rcvEnvelope.encrypted);
                messageBuf = decryptMessage(ctx, rcvEnvelope, messageBuf);
            }


            if (rcvEnvelope.truncated) {
                //消息分消息信封传传递
                MessagePackets messagePackets = messagePacketsManager.getMessagePackets(rcvEnvelope);

                if (messagePackets == null) {
                    messagePackets = new MessagePackets(rcvEnvelope.messageLength);
                }

                messagePackets.receivePacket(rcvEnvelope, messageBuf);

                if (messagePackets.isReceiveCompleted()) {
                    log.debug("response data <==: {}", Hex.encodeHexString(messagePackets.getMessageBytes()));
                    AbstractMessage message = Encoder.decodeMessage(messagePackets.getMessageBytes(), 0, rcvEnvelope);
                    log.debug("<== receive response, requestId {},detail {}", message.requestId, message);
                    return message;
                }
                return null;

            } else {

                log.debug("response data <==: {}", Hex.encodeHexString(messageBuf));
                AbstractMessage message = Encoder.decodeMessage(messageBuf, 0, rcvEnvelope);
                log.debug("<== receive response, requestId {},detail {}", message.requestId, message);
                return message;
            }
        }
    }

    private byte[] decryptMessage(ChannelHandlerContext ctx, MessageEnvelope rcvEnvelope, byte[] messageBuf) throws IDException, HandleException {
        Channel channel = ctx.channel();
        Attribute<Session> attr = channel.attr(Transport.SESSION_KEY);

        Session session = attr.get();
        if (session == null || !session.isEncryptMessage()) {
            throw new IDException(IDException.ENCRYPTION_ERROR, "session not setup while decrypt");
        }

        int offset = 0;
        int len = messageBuf.length;
        try {
            // create, initialize and cache a new decryption cipher
            HdlSecurityProvider provider = HdlSecurityProvider.getInstance();
            if (provider == null) {
                throw new HandleException(HandleException.MISSING_CRYPTO_PROVIDER, "Encryption/Key generation engine missing");
            }

            boolean legacy = !AbstractMessage.hasEqualOrGreaterVersion(rcvEnvelope.protocolMajorVersion, rcvEnvelope.protocolMinorVersion, 2, 4);
            byte[] iv = null;
            if (!legacy) {
                int ivSize = provider.getIvSize(session.getSessionKeyAlgorithmCode(), rcvEnvelope.protocolMajorVersion, rcvEnvelope.protocolMinorVersion);
                if (ivSize > 0) iv = Util.substring(messageBuf, offset, offset + ivSize);
                offset += ivSize;
                len -= ivSize;
            }
            Cipher decryptCipher = provider.getCipher(session.getSessionKeyAlgorithmCode(), session.getSessionKey(), Cipher.DECRYPT_MODE, iv, rcvEnvelope.protocolMajorVersion, rcvEnvelope.protocolMinorVersion);
            messageBuf = decryptCipher.doFinal(messageBuf, offset, len);
        } catch (Exception e) {
            if (e instanceof HandleException) throw (HandleException) e;
            throw new HandleException(HandleException.ENCRYPTION_ERROR, "Error decrypting buffer", e);
        }

        rcvEnvelope.encrypted = false;
        rcvEnvelope.messageLength = messageBuf.length;
        return messageBuf;
    }


}
