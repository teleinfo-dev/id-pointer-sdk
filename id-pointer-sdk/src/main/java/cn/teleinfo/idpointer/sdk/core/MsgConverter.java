package cn.teleinfo.idpointer.sdk.core;

public class MsgConverter {

    private static MsgConverter msgConverter;

    private MsgConverter() {

    }

    public static MsgConverter getInstance() {
        if (msgConverter == null) {
            synchronized (MsgConverter.class) {
                if (msgConverter == null) {
                    msgConverter = new MsgConverter();
                }
            }
        }
        return msgConverter;
    }

    public byte[] convertLoginIDSystemReqToBytes(LoginIDSystemIdRequest req) {
        int bodyLen = req.handle.length + 4 * 2;
        byte[] msg = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        Encoder.writeHeader(req, msg, bodyLen);
        int loc = Common.MESSAGE_HEADER_SIZE;
        loc += Encoder.writeByteArray(msg, loc, req.handle, 0, req.handle.length);
        loc += write4Bytes(msg, loc, req.requestedIndexes);
        return msg;
    }

    public LoginIDSystemIdResponse convertBytesToLoginIDSystemResponse(byte[] msg, int offset, MessageEnvelope env) {
        return new LoginIDSystemIdResponse(null, null);
    }

    public  int writeByteArray(byte[] dest, int offset, byte[] src) {
        if (src != null) {
            return Encoder.writeByteArray(dest, offset, src, 0, src.length);
        }else {
            return write4Bytes(dest, offset, 0);
        }
    }

    private int write4Bytes(byte[] buf, int offset, int value) {
        buf[offset++] = (byte) (255 & value >>> 24);
        buf[offset++] = (byte) (255 & value >> 16);
        buf[offset++] = (byte) (255 & value >> 8);
        buf[offset++] = (byte) (255 & value);
        return 4;
    }
}
