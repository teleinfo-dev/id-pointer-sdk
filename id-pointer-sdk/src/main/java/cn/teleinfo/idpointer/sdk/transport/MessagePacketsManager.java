package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.core.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * todo ll:clear timeout packets
 */
public class MessagePacketsManager {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MessagePacketsManager.class);
    private ConcurrentHashMap<Integer, MessagePackets> packetsMap;
    private MsgConverter msgConverter = MsgConverter.getInstance();

    public MessagePacketsManager() {
        this.packetsMap = new ConcurrentHashMap<>();
    }

    public MessagePackets getMessagePackets(MessageEnvelope rcvEnvelope) {
        Integer key = rcvEnvelope.requestId;
        if (!packetsMap.containsKey(key)) {
            packetsMap.putIfAbsent(key, new MessagePackets(rcvEnvelope.messageLength));
        }
        return packetsMap.get(key);
    }

    public MessagePackets getMessagePackets(Integer requestId) {
        return packetsMap.get(requestId);
    }

    public void removeMessagePackets(Integer requestId) {
        packetsMap.remove(requestId);
    }

    /**
     * 获取udp数据包
     */
    public DatagramPacket[] getUdpPacketsForRequest(AbstractRequest req, InetSocketAddress inetSocketAddress) throws HandleException {
        MessageEnvelope sndEnvelope = new MessageEnvelope();
        if (req.majorProtocolVersion > 0 && req.minorProtocolVersion >= 0) {
            sndEnvelope.protocolMajorVersion = req.majorProtocolVersion;
            sndEnvelope.protocolMinorVersion = req.minorProtocolVersion;
            sndEnvelope.suggestMajorProtocolVersion = req.suggestMajorProtocolVersion;
            sndEnvelope.suggestMinorProtocolVersion = req.suggestMinorProtocolVersion;
        }

        sndEnvelope.sessionId = 0;
        if (req.sessionId > 0) {
            sndEnvelope.sessionId = req.sessionId;
        }

        if (req.requestId <= 0) {
            throw new HandleException(HandleException.INTERNAL_ERROR, "illegal requestId");
        }

        sndEnvelope.requestId = req.requestId;

        // get the encoded message (including header body and signature,
        // but not the envelope) and create a set of udp packets from it
        byte requestBuf[] = null;

        if (req instanceof LoginIDSystemRequest) {
            requestBuf = msgConverter.convertLoginIDSystemReqToBytes((LoginIDSystemRequest) req);
        } else {
            requestBuf = req.getEncodedMessage();
        }

        // request may choose to encrypt itself here if session available.
        if (req.encrypt || (req.sessionInfo != null && req.shouldEncrypt())) {
            if (req.sessionInfo == null)
                throw new HandleException(HandleException.INCOMPLETE_SESSIONSETUP, "Cannot encrypt messages without a session");
            requestBuf = req.sessionInfo.encryptBuffer(requestBuf, 0, requestBuf.length);
            // req.encrypt could be just a request that the server encrypt the response;
            // whether to encrypt the request could be separate
            sndEnvelope.encrypted = true;
        }

        sndEnvelope.messageLength = requestBuf.length;

        int numPackets = sndEnvelope.messageLength / Common.MAX_UDP_DATA_SIZE;
        if ((sndEnvelope.messageLength % Common.MAX_UDP_DATA_SIZE) != 0) numPackets++;

        if (numPackets == 0) {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Cannot send empty request");
        }
        DatagramPacket packets[] = new DatagramPacket[numPackets];
        int bytesRemaining = sndEnvelope.messageLength;

        sndEnvelope.truncated = numPackets > 1;

        for (int packetNum = 0; packetNum < numPackets; packetNum++) {
            int thisPacketSize = Math.min(Common.MAX_UDP_DATA_SIZE, bytesRemaining);
            byte buf[] = new byte[thisPacketSize + Common.MESSAGE_ENVELOPE_SIZE];
            sndEnvelope.messageId = packetNum;
            Encoder.encodeEnvelope(sndEnvelope, buf);

            if (log.isTraceEnabled()) {
                log.trace("send envelope: {}", ByteBufUtil.hexDump(buf));
            }

            System.arraycopy(requestBuf, requestBuf.length - bytesRemaining, buf, Common.MESSAGE_ENVELOPE_SIZE, buf.length - Common.MESSAGE_ENVELOPE_SIZE);
            packets[packetNum] = new DatagramPacket(Unpooled.wrappedBuffer(buf), inetSocketAddress);
            bytesRemaining -= thisPacketSize;
        }
        return packets;
    }


    public ByteBuf getTcpMessageEnvelopeForRequest(AbstractMessage req) throws HandleException {
        MessageEnvelope sndEnvelope = new MessageEnvelope();
        if (req.majorProtocolVersion > 0 && req.minorProtocolVersion >= 0) {
            sndEnvelope.protocolMajorVersion = req.majorProtocolVersion;
            sndEnvelope.protocolMinorVersion = req.minorProtocolVersion;
            sndEnvelope.suggestMajorProtocolVersion = req.suggestMajorProtocolVersion;
            sndEnvelope.suggestMinorProtocolVersion = req.suggestMinorProtocolVersion;
        }

        sndEnvelope.sessionId = 0;
        if (req.sessionId > 0) {
            sndEnvelope.sessionId = req.sessionId;
        }

        if (req.requestId <= 0) {
            throw new HandleException(HandleException.INTERNAL_ERROR, "illegal requestId");
        }

        sndEnvelope.requestId = req.requestId;

        // get the encoded message (including header body and signature,
        // but not the envelope) and create a set of udp packets from it
        byte requestBuf[] = null;

        if (req instanceof LoginIDSystemRequest) {
            requestBuf = msgConverter.convertLoginIDSystemReqToBytes((LoginIDSystemRequest) req);
        } else {
            requestBuf = req.getEncodedMessage();
        }

        if (req instanceof AbstractRequest) {
            AbstractRequest request = (AbstractRequest) req;
            // request may choose to encrypt itself here if session available.
            if (req.encrypt || (request.sessionInfo != null && request.shouldEncrypt())) {
                if (request.sessionInfo == null)
                    throw new HandleException(HandleException.INCOMPLETE_SESSIONSETUP, "Cannot encrypt messages without a session");
                requestBuf = request.sessionInfo.encryptBuffer(requestBuf, 0, requestBuf.length);
                // req.encrypt could be just a request that the server encrypt the response;
                // whether to encrypt the request could be separate
                sndEnvelope.encrypted = true;
            }
        }

        sndEnvelope.messageLength = requestBuf.length;

        int numPackets = sndEnvelope.messageLength / Common.MAX_MESSAGE_DATA_SIZE;
        if ((sndEnvelope.messageLength % Common.MAX_MESSAGE_DATA_SIZE) != 0) numPackets++;

        if (numPackets == 0) {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Cannot send empty request");
        }

        int bytesRemaining = sndEnvelope.messageLength;

        sndEnvelope.truncated = numPackets > 1;
        ByteBuf toWriteBuf = Unpooled.buffer();

        int thisPacketSize = 0;
        byte[] buf = null;
        for (int packetNum = 0; packetNum < numPackets; packetNum++) {
            thisPacketSize = Math.min(Common.MAX_MESSAGE_DATA_SIZE, bytesRemaining);
            buf = new byte[thisPacketSize + Common.MESSAGE_ENVELOPE_SIZE];
            sndEnvelope.messageId = packetNum;
            Encoder.encodeEnvelope(sndEnvelope, buf);
            System.arraycopy(requestBuf, requestBuf.length - bytesRemaining, buf, Common.MESSAGE_ENVELOPE_SIZE, buf.length - Common.MESSAGE_ENVELOPE_SIZE);
            if (log.isTraceEnabled()) {
                log.trace("send envelope: {}", ByteBufUtil.hexDump(buf));
            }
            toWriteBuf.writeBytes(buf);
            bytesRemaining -= thisPacketSize;
        }

        return toWriteBuf;
    }
}
