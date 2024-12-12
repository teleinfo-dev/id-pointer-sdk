package cn.teleinfo.idpointer.sdk.transport;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SM4;
import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.security.HdlSecurityProvider;
import cn.teleinfo.idpointer.sdk.session.SessionDefault;
import cn.teleinfo.idpointer.sdk.session.v3.Session;
import cn.teleinfo.idpointer.sdk.transport.v3.IdTcpTransport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Attribute;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

import javax.crypto.Cipher;
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
    public DatagramPacket[] getUdpPacketsForRequest(AbstractIdRequest req, InetSocketAddress inetSocketAddress) throws HandleException {
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

        if (req instanceof LoginIDSystemIdRequest) {
            requestBuf = msgConverter.convertLoginIDSystemReqToBytes((LoginIDSystemIdRequest) req);
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


    public ByteBuf getTcpMessageEnvelopeForRequest(AbstractMessage req, Channel channel) throws HandleException, IDException {
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

        if (req instanceof LoginIDSystemIdRequest) {
            requestBuf = msgConverter.convertLoginIDSystemReqToBytes((LoginIDSystemIdRequest) req);
        } else {
            requestBuf = req.getEncodedMessage();
        }

        log.debug("requestBuf:{}",Hex.encodeHexString(requestBuf));

        if (req instanceof AbstractIdRequest) {
            AbstractIdRequest request = (AbstractIdRequest) req;
            // request may choose to encrypt itself here if session available.
            //if (req.encrypt || (request.sessionInfo != null && request.shouldEncrypt())) {
            //    if (request.sessionInfo == null)
            //        throw new HandleException(HandleException.INCOMPLETE_SESSIONSETUP, "Cannot encrypt messages without a session");
            //    requestBuf = request.sessionInfo.encryptBuffer(requestBuf, 0, requestBuf.length);
            //    // req.encrypt could be just a request that the server encrypt the response;
            //    // whether to encrypt the request could be separate
            //    sndEnvelope.encrypted = true;
            //}
            if (req.encrypt) {
                // todo:请求加密
                Attribute<SessionDefault> attr = channel.attr(IdTcpTransport.SESSION_KEY);
                SessionDefault sessionDefault = attr.get();
                if (sessionDefault == null || !sessionDefault.isEncryptMessage()) {
                    throw new IDException(IDException.ENCRYPTION_ERROR, "session not setup");
                }
                try {
                    // create, initialize and cache a new encryption cipher
                    HdlSecurityProvider provider = HdlSecurityProvider.getInstance();
                    if (provider == null) {
                        throw new IDException(IDException.MISSING_CRYPTO_PROVIDER, "Encryption/Key generation engine missing");
                    }

                    if (sessionDefault.getSessionKeyAlgorithmCode() == HdlSecurityProvider.ENCRYPT_ALG_SM4) {
                        // todo: key初始化
                        log.debug("session key:{}", Hex.encodeHexString(sessionDefault.getSessionKey()));

                        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, sessionDefault.getSessionKey(),  sessionDefault.getSessionKey());

                        requestBuf = sm4.encrypt(requestBuf);
                        log.debug("requestBuf encrypt:{}",Hex.encodeHexString(requestBuf));
                    } else {
                        Cipher encryptCipher = provider.getCipher(sessionDefault.getSessionKeyAlgorithmCode(), sessionDefault.getSessionKey(), javax.crypto.Cipher.ENCRYPT_MODE, null, req.majorProtocolVersion, req.minorProtocolVersion);

                        byte[] ciphertext = encryptCipher.doFinal(requestBuf, 0, requestBuf.length);

                        boolean legacy = !AbstractMessage.hasEqualOrGreaterVersion(2, 10, 2, 4);
                        if (!legacy) {
                            byte[] iv = encryptCipher.getIV();
                            if (iv == null) {
                                iv = new byte[0];
                            }
                            ByteBuf toWriteBuf = Unpooled.buffer();
                            toWriteBuf.writeBytes(iv);
                            toWriteBuf.writeBytes(ciphertext);
                            requestBuf = Util.concat(iv, ciphertext);
                        } else {
                            requestBuf = ciphertext;
                        }
                    }

                } catch (Exception e) {
                    log.error("===", e);
                    if (e instanceof IDException) throw (IDException) e;
                    throw new IDException(IDException.ENCRYPTION_ERROR, "Error encrypting buffer", e);
                }

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
