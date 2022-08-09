package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.core.Common;
import cn.teleinfo.idpointer.sdk.core.MessageEnvelope;
import io.netty.buffer.ByteBuf;

public class MessagePackets {
    private int messageLength;
    private int packetsNum;
    private int lastPacketSize;
    private byte[] messageBytes;
    private boolean receiveCompleted = false;
    private boolean[] packetsReceived;
    private long createTimeMillis;

    public MessagePackets(int messageLength) {
        this(messageLength, Common.MAX_UDP_DATA_SIZE);
    }

    public MessagePackets(int messageLength, int dataSize) {
        this.messageLength = messageLength;
        this.packetsNum = messageLength / dataSize;
        this.lastPacketSize = messageLength % dataSize;
        if (this.lastPacketSize > 0) {
            this.packetsNum++;
        } else {
            this.lastPacketSize = dataSize;
        }

        this.packetsReceived = new boolean[this.packetsNum];
        this.messageBytes = new byte[messageLength];
        this.createTimeMillis = System.currentTimeMillis();
    }

    public boolean isReceiveCompleted() {
        if (!receiveCompleted) {
            boolean haveAllPackets = true;
            for (int i = 0; i < packetsReceived.length; i++) {
                if (!packetsReceived[i]) {
                    haveAllPackets = false;
                    break;
                }
            }
            receiveCompleted = haveAllPackets;
        }
        return receiveCompleted;
    }

    /**
     * 接收 packet
     */
    public void receivePacket(MessageEnvelope rcvEnvelope, byte[] dataArray) {
        packetsReceived[rcvEnvelope.messageId] = true;
        System.arraycopy(dataArray, 0, messageBytes, rcvEnvelope.messageId * Common.MAX_UDP_DATA_SIZE, dataArray.length);
    }

    public byte[] getMessageBytes() {
        return messageBytes;
    }
}
