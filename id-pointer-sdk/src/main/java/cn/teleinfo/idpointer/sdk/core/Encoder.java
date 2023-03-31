/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
 All rights reserved.

 The HANDLE.NET software is made available subject to the
 Handle.Net Public License Agreement, which may be obtained at
 http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
 \**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import com.google.gson.Gson;
import org.slf4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

/*******************************************************************************
 * The static functions in this class are used to translate message objects and
 * records to and from their byte-array representation in which they are sent
 * over the network.
 *******************************************************************************/
public abstract class Encoder {

    public static final int INT_SIZE = 4;
    public static final int INT2_SIZE = 2;
    public static final int LONG_SIZE = 8;

    public static final int MSG_FLAG_AUTH = 0x80000000; // don't use cache, use only primaries
    public static final int MSG_FLAG_CERT = 0x40000000; // asks server to sign responses
    public static final int MSG_FLAG_ENCR = 0x20000000; // asks server to encrypt responses
    public static final int MSG_FLAG_RECU = 0x10000000; // server should try and resolve handle if not found
    public static final int MSG_FLAG_CACR = 0x08000000; // responses should be signed by cache
    public static final int MSG_FLAG_CONT = 0x04000000; // there are more parts to this message
    public static final int MSG_FLAG_KPAL = 0x02000000; // keep the socket open for more requests
    public static final int MSG_FLAG_PUBL = 0x01000000; // resolution requests should only return public vals
    public static final int MSG_FLAG_RRDG = 0x00800000; // responses should include a digest of the request
    public static final int MSG_FLAG_OVRW = 0x00400000; // ask server to overwrite existing values
    public static final int MSG_FLAG_MINT = 0x00200000; // used in create request. Asks server to mint a new suffix
    public static final int MSG_FLAG_DNRF = 0x00100000; // requests server to not send a referral response

    public static final byte ENV_FLAG_COMPRESSED = (byte) 0x80;
    public static final byte ENV_FLAG_ENCRYPTED = (byte) 0x40;
    public static final byte ENV_FLAG_TRUNCATED = (byte) 0x20;

    // the permission masks for the per-value access specifiers (rwrw)
    public static final byte PERM_ADMIN_READ = 0x8;
    public static final byte PERM_ADMIN_WRITE = 0x4;
    public static final byte PERM_PUBLIC_READ = 0x2;
    public static final byte PERM_PUBLIC_WRITE = 0x1;

    public static final String MSG_INVALID_ARRAY_SIZE = "Invalid array size";

    static final int PERM_ADD_HANDLE = 0x0001;
    static final int PERM_DELETE_HANDLE = 0x0002;
    static final int PERM_ADD_NA = 0x0004;
    static final int PERM_DELETE_NA = 0x0008;
    static final int PERM_MODIFY_VALUE = 0x0010;
    static final int PERM_REMOVE_VALUE = 0x0020;
    static final int PERM_ADD_VALUE = 0x0040;
    static final int PERM_MODIFY_ADMIN = 0x0080;
    static final int PERM_REMOVE_ADMIN = 0x0100;
    static final int PERM_ADD_ADMIN = 0x0200;
    static final int PERM_READ_VALUE = 0x0400;
    static final int PERM_LIST_HDLS = 0x0800;

    //session implementation
    public static final int SESSION_FLAG_CERT = 0x80000000; // asks server to sign responses for all session msgs
    public static final int SESSION_FLAG_ENCR = 0x40000000; // asks server to encrypt responses for all session msgs
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Encoder.class);

    /*******************************************************************************
     * Read an 8-octet integer (java long) value from the given byte array
     * starting at the specified location.
     *******************************************************************************/
    public static final long readLong(byte buf[], int offset) {
        return (buf[offset]) << 56 | (buf[offset + 1] & 0xFFL) << 48 | (buf[offset + 2] & 0xFFL) << 40 | (buf[offset + 3] & 0xFFL) << 32 | (buf[offset + 4] & 0xFFL) << 24 | (buf[offset + 5] & 0xFFL) << 16 | (buf[offset + 6] & 0xFFL) << 8
                | (buf[offset + 7] & 0xFFL);
    }

    /*******************************************************************************
     * Write an 8-octet integer (java long) value into the given byte array
     * starting at the specified location.
     *******************************************************************************/
    public static final int writeLong(byte buf[], int offset, long value) {
        buf[offset++] = (byte) (0xff & (value >>> 56)); // should this be a signed shift??
        buf[offset++] = (byte) (0xff & (value >>> 48));
        buf[offset++] = (byte) (0xff & (value >>> 40));
        buf[offset++] = (byte) (0xff & (value >>> 32));
        buf[offset++] = (byte) (0xff & (value >>> 24));
        buf[offset++] = (byte) (0xff & (value >>> 16));
        buf[offset++] = (byte) (0xff & (value >>> 8));
        buf[offset++] = (byte) (0xff & (value));
        return LONG_SIZE;
    }

    /*******************************************************************************
     * Read a 2-byte integer value from the given byte array
     * starting at the specified location.
     *******************************************************************************/
    public static final int readInt2(byte buf[], int offset) {
        return (buf[offset]) << 8 | buf[offset + 1] & 0x00ff;
    }

    /*******************************************************************************
     * Write a 2-byte integer value into the given byte array
     * starting at the specified location.
     *******************************************************************************/
    public static final int writeInt2(byte buf[], int offset, int value) {
        buf[offset++] = (byte) ((value & 0xff00) >>> 8);
        buf[offset++] = (byte) (value & 0xff);
        return INT2_SIZE;
    }

    /*******************************************************************************
     * Read a 4-byte integer value from the given byte array
     * starting at the specified location.
     *******************************************************************************/
    public static final int readInt(byte buf[], int offset) {
        return buf[offset] << 24 | (0x00ff & buf[offset + 1]) << 16 | (0x00ff & buf[offset + 2]) << 8 | (0x00ff & buf[offset + 3]);
    }

    /*******************************************************************************
     * Write a 4-byte integer value into the given byte array
     * starting at the specified location.
     *******************************************************************************/
    public static final int writeInt(byte buf[], int offset, int value) {
        buf[offset++] = (byte) (0xff & (value >>> 24)); //// should this be a signed shift??
        buf[offset++] = (byte) (0xff & (value >> 16));
        buf[offset++] = (byte) (0xff & (value >> 8));
        buf[offset++] = (byte) (0xff & (value));
        return INT_SIZE;
    }

    /*******************************************************************************
     * Read a byte array from the given buffer starting at the specified
     * location.  This method first reads a 4-octet integer and then reads
     * that many bytes from the buffer.
     *******************************************************************************/
    public static final byte[] readByteArray(byte buf[], int offset) throws HandleException {
        int len = readInt(buf, offset);
        if (len < 0 || len > Common.MAX_ARRAY_SIZE)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, MSG_INVALID_ARRAY_SIZE);
        byte a[] = new byte[len];
        System.arraycopy(buf, offset + INT_SIZE, a, 0, len);
        return a;
    }

    /*******************************************************************************
     * Write the given byte array to a given buffer starting at the specified
     * location.  This first writes the length of the array as a 4-octet integer,
     * and then writes the bytes of the array.
     *******************************************************************************/
    public static final int writeByteArray(byte buf[], int offset, byte bufToWrite[]) {
        if (bufToWrite != null) return writeByteArray(buf, offset, bufToWrite, 0, bufToWrite.length);
        else return writeInt(buf, offset, 0);
    }

    /*******************************************************************************
     * Write the given byte array to a given buffer starting at the specified
     * location.  This first writes the length of the array as a 4-octet integer,
     * and then writes the bytes of the array.
     *******************************************************************************/
    public static final int writeByteArray(byte buf[], int offset, byte bufToWrite[], int woffset, int length) {
        offset += writeInt(buf, offset, length);
        System.arraycopy(bufToWrite, woffset, buf, offset, length);
        return INT_SIZE + length;
    }

    /*******************************************************************************
     * This writes an array of byte arrays to the given buffer.  This first writes
     * the number of arrays as a 4-octet integer, and then writes each individual
     * byte array using a call to writeByteArray.
     *******************************************************************************/
    public static final int writeByteArrayArray(byte buf[], int offset, byte bufToWrite[][]) {
        if (bufToWrite == null) return writeInt(buf, offset, 0);
        int origOffset = offset;
        int alen = bufToWrite.length;
        offset += writeInt(buf, offset, alen);
        for (int i = 0; i < alen; i++) {
            offset += writeByteArray(buf, offset, bufToWrite[i], 0, bufToWrite[i].length);
        }
        return offset - origOffset;
    }

    /*******************************************************************************
     * This writes a given array of integers to the given buffer, starting at the
     * specified location.  This first writes the length of the integer array as
     * a 4-octet integer, then writes each integer in the array using a call to
     * writeInt.  This will return the number of bytes that were read.
     *******************************************************************************/
    public static final int writeIntArray(byte buf[], int offset, int bufToWrite[]) {
        if (bufToWrite == null) return writeInt(buf, offset, 0);
        int alen = bufToWrite.length;
        offset += writeInt(buf, offset, alen);
        for (int i = 0; i < alen; i++) {
            offset += writeInt(buf, offset, bufToWrite[i]);
        }
        return INT_SIZE + INT_SIZE * alen;
    }

    /*******************************************************************************
     * Read an array of 4-byte integer values from the given buffer starting at
     * the specified location.  This method first reads a 4-octet integer and
     * then reads that many integer values from the buffer.
     *******************************************************************************/
    public static final int[] readIntArray(byte buf[], int offset) throws HandleException {
        int len = readInt(buf, offset);
        if (len < 0 || len > Common.MAX_ARRAY_SIZE)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, MSG_INVALID_ARRAY_SIZE);
        offset += INT_SIZE;
        int a[] = new int[len];
        for (int i = 0; i < len; i++) {
            a[i] = readInt(buf, offset);
            offset += INT_SIZE;
        }
        return a;
    }

    /*******************************************************************************
     * This allocates and reads an array of byte arrays where the length of the
     * array is already known.  For each byte array in 'a', this reads a byte
     * array using a call to readByteArray.  This will return the number of bytes
     * that were read.
     *******************************************************************************/
    public static final int readByteArrayArray(byte a[][], byte buf[], int offset) throws HandleException {
        int origOffset = offset;
        for (int i = 0; i < a.length; i++) {
            a[i] = readByteArray(buf, offset);
            offset += a[i].length + INT_SIZE;
        }
        return offset - origOffset;
    }

    /*******************************************************************************
     * Display the contents of the given buffer in a somewhat human-readable
     * format.  This is only used for debugging.
     *******************************************************************************/
    public static final void dumpBytes(byte buf[]) {
        if (buf != null) dumpBytes(buf, 0, buf.length);
    }

    /*******************************************************************************
     * Display the contents of the given buffer in a somewhat human-readable
     * format.  This is only used for debugging.
     *******************************************************************************/
    public static final void dumpBytes(byte buf[], int len) {
        dumpBytes(buf, 0, len);
    }

    /*******************************************************************************
     * Display the contents of the given buffer in a somewhat human-readable
     * format.  This is only used for debugging.
     *******************************************************************************/
    public static final void dumpBytes(byte buf[], int offset, int len) {
        len += offset;
        int j = 0;
        for (int i = offset; i < len && i < buf.length; i++, j++) {
            if (j % 8 == 0) System.err.print((i == 0) ? "" : "\n");
            else System.err.print("  ");
            String hs = Integer.toHexString(0x00ff & buf[i]);
            if (hs.length() < 2) System.err.print('0');
            System.err.print(hs);
        }
        System.err.print('\n');
    }

    public static class MessageHeaders {

        public int opCode;
        public int responseCode;
        public int opFlags;
        public int serialNum;
        public short recursionCount;
        public int expiration;
        public int bodyLen;
        public int bodyOffset;

        public MessageHeaders(byte[] msg, int offset) {
            int loc = offset;

            // read the header fields..
            opCode = Encoder.readInt(msg, loc);
            loc += Encoder.INT_SIZE;

            responseCode = Encoder.readInt(msg, loc);
            loc += Encoder.INT_SIZE;

            opFlags = Encoder.readInt(msg, loc);
            loc += Encoder.INT_SIZE;

            serialNum = Encoder.readInt2(msg, loc);
            loc += Encoder.INT2_SIZE;

            recursionCount = msg[loc++];

            loc += 1; // 1 reserved byte

            expiration = Encoder.readInt(msg, loc);
            loc += Encoder.INT_SIZE;

            bodyLen = Encoder.readInt(msg, loc);
            loc += Encoder.INT_SIZE;

            bodyOffset = loc;
        }
    }

    public static int readOpCode(byte[] msg, int offset) {
        return readInt(msg, offset);
    }

    /*************************************************************************
     * decode a response message - given the message buffer and a separate 0
     * envelop, return a response object.  If the message is a certified
     * message, the signature for the message is read and put into the
     * message object.
     *************************************************************************/
    public static final AbstractMessage decodeMessage(byte msg[], int offset, MessageEnvelope envelope) throws HandleException {
        MessageHeaders headers = new MessageHeaders(msg, offset);
        int bodyOffset = headers.bodyOffset;
        int loc = bodyOffset;

        AbstractMessage message = null;
        if (headers.responseCode == AbstractMessage.RC_RESERVED) {
            // this is a request, not a response, message
            switch (headers.opCode) {
                case AbstractMessage.OC_RESOLUTION:
                    message = decodeResolutionRequest(msg, loc, envelope);
                    break;
                case AbstractMessage.OC_RESPONSE_TO_CHALLENGE:
                    message = decodeChallengeAnswer(msg, loc, envelope);
                    break;
                case AbstractMessage.OC_VERIFY_CHALLENGE:
                    message = decodeVerifyAuthRequest(msg, loc, envelope);
                    break;
                case AbstractMessage.OC_CREATE_HANDLE:
                    message = decodeCreateHandleRequest(msg, loc, envelope, headers.opCode);
                    break;
                case AbstractMessage.OC_ADD_VALUE:
                    message = decodeAddValueRequest(msg, loc, envelope);
                    break;
                case AbstractMessage.OC_MODIFY_VALUE:
                    message = decodeModifyValueRequest(msg, loc, envelope);
                    break;
                case AbstractMessage.OC_REMOVE_VALUE:
                    message = decodeRemoveValueRequest(msg, loc, envelope);
                    break;
                case AbstractMessage.OC_DELETE_HANDLE:
                    message = decodeDeleteHandleRequest(msg, loc, envelope);
                    break;
                case AbstractMessage.OC_LIST_HANDLES:
                    message = decodeListHandlesRequest(msg, loc, envelope);
                    break;
                case AbstractMessage.OC_LIST_HOMED_NAS:
                    message = decodeListNAsRequest(msg, loc, envelope);
                    break;
                case AbstractMessage.OC_RETRIEVE_TXN_LOG:
                    message = decodeRetrieveTxnRequest(msg, loc, envelope);
                    break;
                case AbstractMessage.OC_DUMP_HANDLES:
                    message = decodeDumpHandlesRequest(msg, loc, envelope, headers.bodyLen);
                    break;
                case AbstractMessage.OC_SESSION_SETUP:
                    message = decodeSessionSetupRequest(msg, loc, envelope);
                    break;

                case AbstractMessage.OC_SESSION_EXCHANGEKEY:
                    message = decodeSessionExchangeKeyRequest(msg, loc, envelope);
                    break;

                // requests without messages
                case AbstractMessage.OC_SESSION_TERMINATE:
                case AbstractMessage.OC_GET_NEXT_TXN_ID:
                case AbstractMessage.OC_HOME_NA:
                case AbstractMessage.OC_UNHOME_NA:
                case AbstractMessage.OC_GET_SITE_INFO:
                case AbstractMessage.OC_BACKUP_SERVER:
                    //兼容2001
                case AbstractMessage.OC_LOGIN_ID_SYSTEM:
                    message = decodeGenericRequest(msg, loc, headers.opCode, envelope);
                    break;
                default:
                    throw new HandleException(HandleException.INTERNAL_ERROR, "Unknown opCode in message: " + headers.opCode);
            }
        } else { // this is a response message
            int bodyOffsetBeforeRequestDigest = bodyOffset;
            byte requestDigest[] = null;
            byte rdHashType = 0;
            if (headers.responseCode != AbstractMessage.RC_RESERVED && (MSG_FLAG_RRDG & headers.opFlags) != 0) {
                // this is a response to a request that requested a digest of
                // the request that the server received.  This allows the client
                // to know that the server is responding to the exact request that
                // was sent (i.e. the request wasn't modified in transit to the server)
                rdHashType = msg[bodyOffset++];
                switch (rdHashType) {
                    case Common.HASH_CODE_SHA256:
                        requestDigest = new byte[Common.SHA256_DIGEST_SIZE];
                        System.arraycopy(msg, bodyOffset, requestDigest, 0, Common.SHA256_DIGEST_SIZE);
                        bodyOffset += Common.SHA256_DIGEST_SIZE;
                        break;
                    case Common.HASH_CODE_SHA1:
                        requestDigest = new byte[Common.SHA1_DIGEST_SIZE];
                        System.arraycopy(msg, bodyOffset, requestDigest, 0, Common.SHA1_DIGEST_SIZE);
                        bodyOffset += Common.SHA1_DIGEST_SIZE;
                        break;
                    case Common.HASH_CODE_MD5:
                        requestDigest = new byte[Common.MD5_DIGEST_SIZE];
                        System.arraycopy(msg, bodyOffset, requestDigest, 0, Common.MD5_DIGEST_SIZE);
                        bodyOffset += Common.MD5_DIGEST_SIZE;
                        break;
                    case Common.HASH_CODE_MD5_OLD_FORMAT:
                        bodyOffset--;
                        requestDigest = readByteArray(msg, bodyOffset);
                        bodyOffset += INT_SIZE + requestDigest.length;
                        break;
                    default:
                        throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Unrecognized request hash type: " + ((int) rdHashType));
                }
            }
            int bodyLengthAfterRequestDigest = headers.bodyLen - (bodyOffset - bodyOffsetBeforeRequestDigest);

            // the type of message body, if a response, is based on both the
            // responseCode and opCode
            switch (headers.responseCode) {
                case AbstractMessage.RC_SUCCESS:
                    switch (headers.opCode) {
                        case AbstractMessage.OC_RESOLUTION:
                            message = decodeResolutionResponse(msg, bodyOffset, envelope);
                            break;
                        case AbstractMessage.OC_VERIFY_CHALLENGE:
                            message = decodeVerifyAuthResponse(msg, bodyOffset, envelope);
                            break;
                        case AbstractMessage.OC_LIST_HANDLES:
                            message = decodeListHandlesResponse(msg, bodyOffset, envelope);
                            break;
                        case AbstractMessage.OC_LIST_HOMED_NAS:
                            message = decodeListNAsResponse(msg, bodyOffset, envelope);
                            break;
                        case AbstractMessage.OC_RETRIEVE_TXN_LOG:
                            message = decodeRetrieveTxnResponse(msg, bodyOffset, envelope);
                            break;
                        case AbstractMessage.OC_DUMP_HANDLES:
                            message = decodeDumpHandlesResponse(msg, bodyOffset, envelope);
                            break;
                        case AbstractMessage.OC_SESSION_SETUP:
                            message = decodeSetupSessionResponse(msg, bodyOffset, envelope);
                            break;
                        case AbstractMessage.OC_CREATE_HANDLE:
                            message = decodeCreateHandleResponse(msg, bodyOffset, envelope, bodyLengthAfterRequestDigest);
                            break;

                        case AbstractMessage.OC_SESSION_TERMINATE:
                        case AbstractMessage.OC_DELETE_HANDLE:
                        case AbstractMessage.OC_ADD_VALUE:
                        case AbstractMessage.OC_MODIFY_VALUE:
                        case AbstractMessage.OC_REMOVE_VALUE:
                        case AbstractMessage.OC_HOME_NA:
                        case AbstractMessage.OC_UNHOME_NA:
                        case AbstractMessage.OC_BACKUP_SERVER:
                        case AbstractMessage.OC_SESSION_EXCHANGEKEY:
                            message = decodeGenericResponse(msg, bodyOffset, envelope);
                            break;
                        case AbstractMessage.OC_GET_NEXT_TXN_ID:
                            message = decodeNextTxnIdResponse(msg, bodyOffset, envelope);
                            break;
                        case AbstractMessage.OC_GET_SITE_INFO:
                            message = decodeGetSiteInfoResponse(msg, bodyOffset, bodyLengthAfterRequestDigest, envelope);
                            break;
                        //兼容idis2001
                        case AbstractMessage.OC_LOGIN_ID_SYSTEM:
                            MsgConverter msgConverter = MsgConverter.getInstance();
                            message = msgConverter.convertBytesToLoginIDSystemResponse(msg, bodyOffset, envelope);
                            break;

                        default:
                            throw new HandleException(HandleException.INTERNAL_ERROR, "Unknown opCode in response: " + headers.opCode);
                    }
                    break;
                case AbstractMessage.RC_SERVICE_REFERRAL:
                    message = decodeServiceReferralResponse(headers.responseCode, msg, bodyOffset, envelope, bodyOffset + bodyLengthAfterRequestDigest);
                    break;
                case AbstractMessage.RC_PREFIX_REFERRAL:
                    if (AbstractMessage.hasEqualOrGreaterVersion(envelope.protocolMajorVersion, envelope.protocolMinorVersion, 2, 5)) {
                        message = decodeServiceReferralResponse(headers.responseCode, msg, bodyOffset, envelope, bodyOffset + bodyLengthAfterRequestDigest);
                    } else {
                        // in old servers this was AbstractMessage.RC_SERVER_BACKUP
                        message = decodeErrorMessage(msg, bodyOffset, envelope, envelope.messageLength + offset);
                    }
                    break;
                case AbstractMessage.RC_ERROR:
                case AbstractMessage.RC_AUTHEN_ERROR:
                case AbstractMessage.RC_INVALID_ADMIN:
                case AbstractMessage.RC_INSUFFICIENT_PERMISSIONS:
                case AbstractMessage.RC_AUTHENTICATION_FAILED:
                case AbstractMessage.RC_INVALID_CREDENTIAL:
                case AbstractMessage.RC_AUTHEN_TIMEOUT:
                case AbstractMessage.RC_VALUES_NOT_FOUND:
                case AbstractMessage.RC_HANDLE_NOT_FOUND:
                case AbstractMessage.RC_HANDLE_ALREADY_EXISTS:
                case AbstractMessage.RC_VALUE_ALREADY_EXISTS:
                case AbstractMessage.RC_INVALID_VALUE:
                case AbstractMessage.RC_OUT_OF_DATE_SITE_INFO:
                case AbstractMessage.RC_SERVER_NOT_RESP:
                case AbstractMessage.RC_SERVER_BACKUP:
                case AbstractMessage.RC_SESSION_FAILED:
                case AbstractMessage.RC_SESSION_TIMEOUT:
                case AbstractMessage.RC_INVALID_SESSION_KEY:
                case AbstractMessage.RC_NEED_RSAKEY_FOR_SESSIONEXCHANGE:
                case AbstractMessage.RC_INVALID_SESSIONSETUP_REQUEST:
                case AbstractMessage.RC_SESSION_MESSAGE_REJECTED:
                case AbstractMessage.RC_SERVER_TOO_BUSY:
                case AbstractMessage.RC_PROTOCOL_ERROR:
                case AbstractMessage.RC_OPERATION_NOT_SUPPORTED:
                case AbstractMessage.RC_RECURSION_COUNT_TOO_HIGH:
                case AbstractMessage.RC_INVALID_HANDLE:
                case AbstractMessage.RC_CLIENT_CHANNEL_ERROR:
                case AbstractMessage.RC_CLIENT_TIME_OUT:
                case AbstractMessage.RC_REQUEST_LIMIT_DAILY:
                case AbstractMessage.RC_REGISTER_COUNT_LIMIT:
                case AbstractMessage.RC_PREFIX_LIMIT:
                    message = decodeErrorMessage(msg, bodyOffset, envelope, envelope.messageLength + offset);
                    break;
                case AbstractMessage.RC_AUTHENTICATION_NEEDED:
                    message = decodeChallenge(msg, bodyOffset, headers.opCode, envelope);
                    break;

                default:
                    throw new HandleException(HandleException.INTERNAL_ERROR, "Unknown responseCode: " + headers.responseCode);
            }

            if (requestDigest != null) {
                message.requestDigest = requestDigest;
                message.rdHashType = rdHashType;
            }
        }

        if (message == null) {
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Unknown message type: opCode=" + headers.opCode + "; responseCode=" + headers.responseCode);
        }

        // copy the body and header of the message into the rawMessage field
        // of the message object so that it can be used later, or signed/verified
        // with a digital signature.  This is also used when returning a digest
        // of the original request - necessary for verifying that the server is
        // responding to the exact request that we sent it.
        message.messageBody = new byte[Common.MESSAGE_HEADER_SIZE + headers.bodyLen];
        System.arraycopy(msg, offset, message.messageBody, 0, Common.MESSAGE_HEADER_SIZE + headers.bodyLen);

        // copy the header fields into the message object
        message.sessionId = envelope.sessionId;
        message.requestId = envelope.requestId;
        message.majorProtocolVersion = envelope.protocolMajorVersion;
        message.minorProtocolVersion = envelope.protocolMinorVersion;
        message.suggestMajorProtocolVersion = envelope.suggestMajorProtocolVersion;
        message.suggestMinorProtocolVersion = envelope.suggestMinorProtocolVersion;
        message.opCode = headers.opCode;
        message.responseCode = headers.responseCode;
        message.siteInfoSerial = headers.serialNum;
        message.recursionCount = headers.recursionCount;

        if (headers.responseCode == AbstractMessage.RC_PREFIX_REFERRAL && !AbstractMessage.hasEqualOrGreaterVersion(envelope.protocolMajorVersion, envelope.protocolMinorVersion, 2, 5)) {
            message.responseCode = AbstractMessage.RC_SERVER_BACKUP;
        }

        decodeOpFlagsInToMessage(message, headers.opFlags);

        message.expiration = headers.expiration;

        // skip to the signature section of the message
        loc += headers.bodyLen;

        // read the signature, if there is room for one
        if (offset + envelope.messageLength >= loc + INT_SIZE) {
            int signatureLength = readInt(msg, loc);
            loc += INT_SIZE;
            // copy the signature into the message object
            message.signature = new byte[signatureLength];
            System.arraycopy(msg, loc, message.signature, 0, signatureLength);
            loc += signatureLength;
        }

        return message;
    }

    public static void decodeOpFlagsInToMessage(AbstractMessage message, int opFlags) {
        message.authoritative = (MSG_FLAG_AUTH & opFlags) != 0;
        message.certify = (MSG_FLAG_CERT & opFlags) != 0;
        message.encrypt = (MSG_FLAG_ENCR & opFlags) != 0;
        message.recursive = (MSG_FLAG_RECU & opFlags) != 0;
        message.cacheCertify = (MSG_FLAG_CACR & opFlags) != 0;
        message.ignoreRestrictedValues = (MSG_FLAG_PUBL & opFlags) != 0;
        message.continuous = (MSG_FLAG_CONT & opFlags) != 0;
        message.keepAlive = (MSG_FLAG_KPAL & opFlags) != 0;
        message.returnRequestDigest = (MSG_FLAG_RRDG & opFlags) != 0;
        message.overwriteWhenExists = (MSG_FLAG_OVRW & opFlags) != 0;
        message.mintNewSuffix = (MSG_FLAG_MINT & opFlags) != 0;
        message.doNotRefer = (MSG_FLAG_DNRF & opFlags) != 0;
    }

    /*************************************************************************
     * Decode a site info record from a byte array.
     *************************************************************************/
    public static SiteInfo decodeSiteInfoRecord(byte data[], int offset) throws HandleException {
        SiteInfo site = new SiteInfo();
        decodeSiteInfoRecord(data, offset, site);
        return site;
    }

    /*************************************************************************
     * Decode a site info record from a byte array.
     *************************************************************************/
    public static void decodeSiteInfoRecord(byte data[], int offset, SiteInfo site) throws HandleException {
        site.dataFormatVersion = readInt2(data, offset);
        offset += INT2_SIZE;

        site.majorProtocolVersion = data[offset++];
        site.minorProtocolVersion = data[offset++];

        site.serialNumber = readInt2(data, offset);
        offset += INT2_SIZE;

        site.isPrimary = (SiteInfo.PRIMARY_SITE & data[offset]) != 0;
        site.multiPrimary = (SiteInfo.MULTI_PRIMARY & data[offset++]) != 0;

        site.hashOption = data[offset++];

        site.hashFilter = readByteArray(data, offset);
        offset += INT_SIZE + site.hashFilter.length;

        int numAtts = readInt(data, offset);
        if (numAtts < 0 || numAtts > Common.MAX_ARRAY_SIZE)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, MSG_INVALID_ARRAY_SIZE);

        site.attributes = new Attribute[numAtts];
        offset += INT_SIZE;
        for (int i = 0; i < site.attributes.length; i++) {
            site.attributes[i] = new Attribute();
            site.attributes[i].name = readByteArray(data, offset);
            offset += INT_SIZE + site.attributes[i].name.length;
            site.attributes[i].value = readByteArray(data, offset);
            offset += INT_SIZE + site.attributes[i].value.length;
        }

        int numServers = readInt(data, offset);
        if (numServers < 0 || numServers > Common.MAX_ARRAY_SIZE)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, MSG_INVALID_ARRAY_SIZE);

        site.servers = new ServerInfo[numServers];
        offset += INT_SIZE;

        // decode each server...
        for (int i = 0; i < site.servers.length; i++) {
            ServerInfo server = new ServerInfo();
            site.servers[i] = server;

            server.serverId = readInt(data, offset);
            offset += INT_SIZE;

            server.ipAddress = new byte[Common.IP_ADDRESS_LENGTH];
            System.arraycopy(data, offset, server.ipAddress, 0, Common.IP_ADDRESS_LENGTH);
            offset += Common.IP_ADDRESS_LENGTH;

            server.publicKey = readByteArray(data, offset);
            offset += INT_SIZE + server.publicKey.length;

            int numIntf = readInt(data, offset);
            if (numIntf < 0 || numIntf > Common.MAX_ARRAY_SIZE)
                throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, MSG_INVALID_ARRAY_SIZE);

            server.interfaces = new Interface[numIntf];
            offset += INT_SIZE;

            // decode each interface...
            for (int j = 0; j < server.interfaces.length; j++) {
                Interface intrfc = new Interface();
                server.interfaces[j] = intrfc;
                intrfc.type = data[offset++];
                intrfc.protocol = data[offset++];
                intrfc.port = readInt(data, offset);
                offset += INT_SIZE;
            }
        }
        if (offset < data.length)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Unexpected data remaining after decoding");
    }

    /**********************************************************************
     * Encode the given site info record into a byte array and return
     * the result.
     **********************************************************************/
    public static byte[] encodeSiteInfoRecord(SiteInfo site) {
        int sz = 0;
        sz += 2; // dataFormatVersion
        sz += 2; // protocolVersion
        sz += 2; // serialNumber
        sz += 1; // primary-mask flags
        sz += 1; // hashOption
        sz += (INT_SIZE) + (site.hashFilter == null ? 0 : site.hashFilter.length); // hashFilter
        sz += INT_SIZE; // number of attributes
        if (site.attributes != null) {
            for (Attribute attribute : site.attributes) {
                sz += INT_SIZE + attribute.name.length;
                sz += INT_SIZE + attribute.value.length;
            }
        }
        sz += INT_SIZE; // number of servers
        if (site.servers != null) {
            for (ServerInfo server : site.servers) {
                sz += INT_SIZE; // serverId
                sz += Common.IP_ADDRESS_LENGTH; // ipAddress
                sz += INT_SIZE + (server.publicKey == null ? 0 : server.publicKey.length); //pubkey

                sz += INT_SIZE; // number of interfaces
                if (server.interfaces != null) {
                    sz += (INT_SIZE + 1 + 1) * server.interfaces.length;
                }
            }
        }

        byte buf[] = new byte[sz];
        int offset = 0;
        offset += writeInt2(buf, offset, site.dataFormatVersion);

        buf[offset++] = site.majorProtocolVersion;
        buf[offset++] = site.minorProtocolVersion;

        offset += writeInt2(buf, offset, site.serialNumber);

        buf[offset++] = (byte) ((site.isPrimary ? SiteInfo.PRIMARY_SITE : 0) | (site.multiPrimary ? SiteInfo.MULTI_PRIMARY : 0));

        buf[offset++] = site.hashOption;

        offset += writeByteArray(buf, offset, site.hashFilter);

        if (site.attributes == null) {
            offset += writeInt(buf, offset, 0);
        } else {
            offset += writeInt(buf, offset, site.attributes.length);
            for (Attribute attribute : site.attributes) {
                offset += writeByteArray(buf, offset, attribute.name);
                offset += writeByteArray(buf, offset, attribute.value);
            }
        }

        if (site.servers == null) {
            offset += writeInt(buf, offset, 0);
        } else {
            offset += writeInt(buf, offset, site.servers.length);
            for (ServerInfo server : site.servers) {
                offset += writeInt(buf, offset, server.serverId);

                System.arraycopy(server.ipAddress, 0, buf, offset + Common.IP_ADDRESS_LENGTH - server.ipAddress.length, server.ipAddress.length);
                offset += Common.IP_ADDRESS_LENGTH;

                offset += writeByteArray(buf, offset, server.publicKey);

                if (server.interfaces == null) {
                    offset += writeInt(buf, offset, 0);
                } else {
                    offset += writeInt(buf, offset, server.interfaces.length);
                    for (Interface intrfc : server.interfaces) {
                        buf[offset++] = intrfc.type;
                        buf[offset++] = intrfc.protocol;
                        offset += writeInt(buf, offset, intrfc.port);
                    }
                }
            }
        }
        return buf;
    }

    public static AdminRecord decodeAdminRecord(byte[] data, int offset) throws HandleException {
        AdminRecord res = new AdminRecord();
        decodeAdminRecord(data, offset, res);
        return res;
    }

    /*******************************************************************************
     * Decode an administrator record from the given byte array.
     *******************************************************************************/
    public static void decodeAdminRecord(byte data[], int offset, AdminRecord admin) throws HandleException {
        try {
            int permissions = readInt2(data, offset);
            offset += INT2_SIZE;

            admin.perms[AdminRecord.ADD_HANDLE] = (PERM_ADD_HANDLE & permissions) != 0;
            admin.perms[AdminRecord.DELETE_HANDLE] = (PERM_DELETE_HANDLE & permissions) != 0;
            admin.perms[AdminRecord.ADD_DERIVED_PREFIX] = (PERM_ADD_NA & permissions) != 0;
            admin.perms[AdminRecord.DELETE_DERIVED_PREFIX] = (PERM_DELETE_NA & permissions) != 0;
            admin.perms[AdminRecord.READ_VALUE] = (PERM_READ_VALUE & permissions) != 0;
            admin.perms[AdminRecord.MODIFY_VALUE] = (PERM_MODIFY_VALUE & permissions) != 0;
            admin.perms[AdminRecord.REMOVE_VALUE] = (PERM_REMOVE_VALUE & permissions) != 0;
            admin.perms[AdminRecord.ADD_VALUE] = (PERM_ADD_VALUE & permissions) != 0;
            admin.perms[AdminRecord.MODIFY_ADMIN] = (PERM_MODIFY_ADMIN & permissions) != 0;
            admin.perms[AdminRecord.REMOVE_ADMIN] = (PERM_REMOVE_ADMIN & permissions) != 0;
            admin.perms[AdminRecord.ADD_ADMIN] = (PERM_ADD_ADMIN & permissions) != 0;
            admin.perms[AdminRecord.LIST_HANDLES] = (PERM_LIST_HDLS & permissions) != 0;

            admin.adminId = readByteArray(data, offset);
            offset += INT_SIZE + admin.adminId.length;

            admin.adminIdIndex = readInt(data, offset);
            offset += INT_SIZE;

            admin.legacyByteLength = offset + INT2_SIZE == data.length;
        } catch (HandleException e) {
            throw e;
        } catch (Exception e) {
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Error decoding admin record", e);
        }
        if (!admin.legacyByteLength && offset < data.length)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Unexpected data remaining after decoding");
    }

    /*******************************************************************************
     * Encode the given admin record into a byte array and return it.
     *******************************************************************************/
    public static byte[] encodeAdminRecord(AdminRecord admin) {
        int recordLen = (admin.legacyByteLength ? INT_SIZE : INT2_SIZE) + // permissions
                INT_SIZE + // admin ID index
                INT_SIZE + // admin ID handle
                admin.adminId.length;
        byte buf[] = new byte[recordLen];
        int offset = 0;
        int permissions = 0;
        permissions |= admin.perms[AdminRecord.ADD_HANDLE] ? PERM_ADD_HANDLE : 0;
        permissions |= admin.perms[AdminRecord.DELETE_HANDLE] ? PERM_DELETE_HANDLE : 0;
        permissions |= admin.perms[AdminRecord.ADD_DERIVED_PREFIX] ? PERM_ADD_NA : 0;
        permissions |= admin.perms[AdminRecord.DELETE_DERIVED_PREFIX] ? PERM_DELETE_NA : 0;
        permissions |= admin.perms[AdminRecord.READ_VALUE] ? PERM_READ_VALUE : 0;
        permissions |= admin.perms[AdminRecord.MODIFY_VALUE] ? PERM_MODIFY_VALUE : 0;
        permissions |= admin.perms[AdminRecord.REMOVE_VALUE] ? PERM_REMOVE_VALUE : 0;
        permissions |= admin.perms[AdminRecord.ADD_VALUE] ? PERM_ADD_VALUE : 0;
        permissions |= admin.perms[AdminRecord.MODIFY_ADMIN] ? PERM_MODIFY_ADMIN : 0;
        permissions |= admin.perms[AdminRecord.REMOVE_ADMIN] ? PERM_REMOVE_ADMIN : 0;
        permissions |= admin.perms[AdminRecord.ADD_ADMIN] ? PERM_ADD_ADMIN : 0;
        permissions |= admin.perms[AdminRecord.LIST_HANDLES] ? PERM_LIST_HDLS : 0;
        offset += writeInt2(buf, offset, permissions);

        offset += writeByteArray(buf, offset, admin.adminId);
        offset += writeInt(buf, offset, admin.adminIdIndex);
        return buf;
    }

    /**
     * Encode the given secret key into a byte array, performing an SHA1 hash
     * and lower-case hex encoding if the hash flag is set.  If the hash
     * flag is not set this may return the same secretKey array that was
     * passed as a parameter.
     */
    public static byte[] encodeSecretKey(byte secretKey[], boolean hash) throws Exception {
        if (hash) {
            secretKey = Util.doSHA1Digest(secretKey);
            secretKey = Util.encodeString(Util.decodeHexString(secretKey, false).toLowerCase());
        }
        return secretKey;
    }

    /*******************************************************************************
     * Calculate the size that a buffer would have to be in order to hold
     * an encoded value of the given admin record.
     *******************************************************************************/
    public static int calculateAdminRecordSize(AdminRecord admin) {
        return INT_SIZE + // the length of the admin ID
                admin.adminId.length + INT_SIZE + // the size of the admin ID index
                INT_SIZE; // the size of the admin permissions
    }

    /*******************************************************************************
     * Encode a generic request (containing a handle, and the basic header info).
     *******************************************************************************/
    public static byte[] encodeGenericRequest(AbstractRequest req) {
        int bodyLen = req.handle.length + INT_SIZE;
        log.debug("bodyLen:{}",bodyLen);
        byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];
        int loc = writeHeader(req, msg, bodyLen);
        writeByteArray(msg, loc, req.handle);
        return msg;
    }

    /*******************************************************************************
     * Decode and return a generic request method with the given encoding and opCode
     * This returns a GenericRequest object which consists of all the normal message
     * info along with a handle.
     *******************************************************************************/
    public static GenericRequest decodeGenericRequest(byte msg[], int offset, int opCode, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        byte handle[] = readByteArray(msg, offset);
        offset += (handle.length + INT_SIZE);
        GenericRequest req = new GenericRequest(handle, opCode, null);
        switch (opCode) {
            case AbstractMessage.OC_GET_NEXT_TXN_ID:
            case AbstractMessage.OC_HOME_NA:
            case AbstractMessage.OC_UNHOME_NA:
                req.isAdminRequest = true;
                break;
            case AbstractMessage.OC_SESSION_TERMINATE:
            case AbstractMessage.OC_GET_SITE_INFO:
            case AbstractMessage.OC_BACKUP_SERVER:
                req.isAdminRequest = false;
                break;
            default:
                System.err.println("Warning: uncaught generic request in Encoder");
                req.isAdminRequest = false;
        }
        return req;
    }

    public static byte[] encodeHandleValue(HandleValue value) {
        byte[] buf = new byte[Encoder.calcStorageSize(value)];
        Encoder.encodeHandleValue(buf, 0, value);
        return buf;
    }

    public static byte[][] encodeHandleValues(HandleValue[] values) {
        byte[][] res = new byte[values.length][];
        for (int i = 0; i < res.length; i++) {
            res[i] = encodeHandleValue(values[i]);
        }
        return res;
    }

    /************************************************************************************
     * Encode the values of the handle into the specified array starting at offset.
     * @return the number of bytes written to the array.;
     ************************************************************************************/
    public static final int encodeHandleValue(byte buf[], int offset, HandleValue value) {
        int origOffset = offset;

        writeInt(buf, offset, value.index); // index
        offset += INT_SIZE;

        writeInt(buf, offset, value.timestamp); // timestamp
        offset += INT_SIZE;

        buf[offset++] = value.ttlType; // ttl Type

        writeInt(buf, offset, value.ttl); // ttl
        offset += INT_SIZE;

        buf[offset++] = (byte) // permission
                ((value.adminRead ? PERM_ADMIN_READ : 0) | (value.adminWrite ? PERM_ADMIN_WRITE : 0) | (value.publicRead ? PERM_PUBLIC_READ : 0) | (value.publicWrite ? PERM_PUBLIC_WRITE : 0));

        offset += writeByteArray(buf, offset, // type
                value.type, 0, value.type.length);

        offset += writeByteArray(buf, offset, // data
                value.data, 0, value.data.length);

        if (value.references != null) { // references
            offset += writeInt(buf, offset, value.references.length);
            for (ValueReference reference : value.references) {
                offset += writeByteArray(buf, offset, reference.handle);
                offset += writeInt(buf, offset, reference.index);
            }
        } else {
            offset += writeInt(buf, offset, 0);
        }

        return offset - origOffset;
    }

    /****************************************************************************
     * Get only the type from the encoded handle value starting at offset.
     ****************************************************************************/
    public static final byte[] getHandleValueType(byte buf[], int offset) throws HandleException {
        return readByteArray(buf, offset + 14);
    }

    /****************************************************************************
     * Get only the index from the encoded handle value starting at offset.
     ****************************************************************************/
    public static final int getHandleValueIndex(byte buf[], int offset) {
        return readInt(buf, offset);
    }

    /****************************************************************************
     * Get only the permissions from the encoded handle value starting at offset.
     ****************************************************************************/
    public static final byte getHandleValuePermissions(byte buf[], int offset) {
        return buf[offset + 13];
    }

    /**********************************************************************
     * Calculate the number of bytes required to store the specified value
     **********************************************************************/
    public static final int calcStorageSize(HandleValue value) {
        int sz = INT_SIZE + // index - 4 bytes
                INT_SIZE + // timestamp - 4 bytes
                1 + // ttlType - 1 byte
                INT_SIZE + // ttl - 4 bytes
                1 + // permissions - 1 byte
                INT_SIZE + // type
                value.type.length + INT_SIZE + // data
                value.data.length;

        // the size of the references...
        sz += INT_SIZE;
        if (value.references != null) {
            for (ValueReference reference : value.references) {
                sz += INT_SIZE + reference.handle.length + INT_SIZE;
            }
        }
        return sz;
    }

    /**********************************************************************
     * Calculate the number of bytes required to store the specified value
     **********************************************************************/
    public static final int calcHandleValueSize(byte values[], int offset) {
        // calculate the size of the fixed fields
        int origOffset = offset;
        offset += INT_SIZE + // index - 4 bytes
                INT_SIZE + // timestamp - 4 bytes
                1 + // ttlType - 1 byte
                INT_SIZE + // ttl - 4 bytes
                1; // permissions - 1 byte

        int fieldLen = readInt(values, offset); // type field
        offset += INT_SIZE + fieldLen;

        fieldLen = readInt(values, offset); // data field
        offset += INT_SIZE + fieldLen;

        fieldLen = readInt(values, offset); // references (number of)
        offset += INT_SIZE;

        for (int i = 0; i < fieldLen; i++) { // each reference - hdl length + hdl + index
            int refLen = readInt(values, offset);
            offset += INT_SIZE + refLen + INT_SIZE;
        }
        return offset - origOffset;
    }

    /**
     * Converts a raw buffer into an array of HandleValue.
     *
     * @param handleValues
     * @return null in the case the handleValues parameter is null;
     * @throws HandleException
     */
    public static HandleValue[] decodeHandleValues(byte[][] handleValues) throws HandleException {
        if (handleValues == null) return null;
        HandleValue value = null;
        HandleValue[] values = new HandleValue[handleValues.length];
        for (int i = 0; i < handleValues.length; i++) {
            value = new HandleValue();
            Encoder.decodeHandleValue(handleValues[i], 0, value);
            values[i] = value;
        }
        return values;
    }

    /**
     * @deprecated Use {@link #decodeHandleValues(byte[][])}.
     */
    @Deprecated
    public static HandleValue[] decodeHandleValue(byte[][] handleValues) throws HandleException {
        return decodeHandleValues(handleValues);
    }

    /************************************************************************************
     * Populate the specified handle value with the values encoded in the given
     * byte array and return the number of bytes read.
     ************************************************************************************/
    public static final int decodeHandleValue(byte buf[], int offset, HandleValue value) throws HandleException {
        int origOffset = offset;
        value.index = readInt(buf, offset); // index 4 bytes
        offset += INT_SIZE;

        value.timestamp = readInt(buf, offset); // timestamp 4 bytes
        offset += INT_SIZE;

        value.ttlType = buf[offset++]; // ttl Type 1 byte

        value.ttl = readInt(buf, offset); // ttl 4 bytes
        offset += INT_SIZE;

        byte permissions = buf[offset++]; // permissions 1 byte
        value.adminRead = (permissions & PERM_ADMIN_READ) != 0;
        value.adminWrite = (permissions & PERM_ADMIN_WRITE) != 0;
        value.publicRead = (permissions & PERM_PUBLIC_READ) != 0;
        value.publicWrite = (permissions & PERM_PUBLIC_WRITE) != 0;

        value.type = readByteArray(buf, offset); // type
        offset += INT_SIZE + value.type.length;

        value.data = readByteArray(buf, offset); // data
        offset += INT_SIZE + value.data.length;

        value.references = new ValueReference[readInt(buf, offset)];
        offset += INT_SIZE; // references
        for (int i = 0; i < value.references.length; i++) {
            value.references[i] = new ValueReference();
            value.references[i].handle = readByteArray(buf, offset);
            offset += INT_SIZE + value.references[i].handle.length;
            value.references[i].index = readInt(buf, offset);
            offset += INT_SIZE;
        }

        value.cachedBuf = buf;
        value.cachedBufOffset = origOffset;
        value.cachedBufLength = offset - origOffset;

        return offset - origOffset;
    }

    /*******************************************************************************
     * Encode the given message object as a byte array and return the resulting
     * buffer.  The isResponse flag is necessary to determine the type of object
     * being encoded (not really, maybe this can be cleaned up).
     *******************************************************************************/
    public static final byte[] encodeMessage(AbstractMessage msg) throws HandleException {
        byte buf[] = null;
        switch (msg.responseCode) {
            case AbstractMessage.RC_RESERVED:
                // this is a request message, not a response
                switch (msg.opCode) {
                    case AbstractMessage.OC_RESOLUTION:
                        buf = encodeResolutionRequest((ResolutionRequest) msg);
                        break;
                    case AbstractMessage.OC_VERIFY_CHALLENGE:
                        buf = encodeVerifyAuthRequest((VerifyAuthRequest) msg);
                        break;
                    case AbstractMessage.OC_CREATE_HANDLE:
                        buf = encodeCreateHandleRequest((CreateHandleRequest) msg);
                        break;
                    case AbstractMessage.OC_DELETE_HANDLE:
                        buf = encodeDeleteHandleRequest((DeleteHandleRequest) msg);
                        break;
                    case AbstractMessage.OC_RETRIEVE_TXN_LOG:
                        buf = encodeRetrieveTxnRequest((RetrieveTxnRequest) msg);
                        break;
                    case AbstractMessage.OC_DUMP_HANDLES:
                        buf = encodeDumpHandlesRequest((DumpHandlesRequest) msg);
                        break;
                    case AbstractMessage.OC_RESPONSE_TO_CHALLENGE:
                        buf = encodeChallengeAnswer((ChallengeAnswerRequest) msg);
                        break;
                    case AbstractMessage.OC_ADD_VALUE:
                        buf = encodeAddValueRequest((AddValueRequest) msg);
                        break;
                    case AbstractMessage.OC_MODIFY_VALUE:
                        buf = encodeModifyValueRequest((ModifyValueRequest) msg);
                        break;
                    case AbstractMessage.OC_REMOVE_VALUE:
                        buf = encodeRemoveValueRequest((RemoveValueRequest) msg);
                        break;
                    case AbstractMessage.OC_LIST_HANDLES:
                        buf = encodeListHandlesRequest((ListHandlesRequest) msg);
                        break;
                    case AbstractMessage.OC_LIST_HOMED_NAS:
                        buf = encodeListNAsRequest((ListNAsRequest) msg);
                        break;
                    case AbstractMessage.OC_SESSION_SETUP:
                        buf = encodeSessionSetupRequest((SessionSetupRequest) msg);
                        break;

                    case AbstractMessage.OC_SESSION_EXCHANGEKEY:
                        buf = encodeSessionExchangeKeyRequest((SessionExchangeKeyRequest) msg);
                        break;
                    case AbstractMessage.OC_LOGIN_ID_SYSTEM:
                        MsgConverter msgConverter = MsgConverter.getInstance();
                        buf = msgConverter.convertLoginIDSystemReqToBytes((LoginIDSystemRequest) msg);
                        break;

                    case AbstractMessage.OC_SESSION_TERMINATE:
                    case AbstractMessage.OC_GET_NEXT_TXN_ID:
                    case AbstractMessage.OC_GET_SITE_INFO:
                    case AbstractMessage.OC_HOME_NA:
                    case AbstractMessage.OC_UNHOME_NA:
                    case AbstractMessage.OC_BACKUP_SERVER:
                        buf = encodeGenericRequest((AbstractRequest) msg);
                        break;
                    default:
                        throw new HandleException(HandleException.INTERNAL_ERROR, "Unknown opCode: " + msg.opCode);
                }
                break;
            case AbstractMessage.RC_SUCCESS:
                // this is a successful response message.  The type of response will be
                // determined by the opCode.
                switch (msg.opCode) {
                    case AbstractMessage.OC_RESOLUTION:
                        buf = encodeResolutionResponse((ResolutionResponse) msg);
                        break;
                    case AbstractMessage.OC_VERIFY_CHALLENGE:
                        buf = encodeVerifyAuthResponse((VerifyAuthResponse) msg);
                        break;
                    case AbstractMessage.OC_GET_NEXT_TXN_ID:
                        buf = encodeNextTxnIdResponse((NextTxnIdResponse) msg);
                        break;
                    case AbstractMessage.OC_LIST_HANDLES:
                        buf = encodeListHandlesResponse((ListHandlesResponse) msg);
                        break;
                    case AbstractMessage.OC_LIST_HOMED_NAS:
                        buf = encodeListNAsResponse((ListNAsResponse) msg);
                        break;
                    case AbstractMessage.OC_RETRIEVE_TXN_LOG:
                        buf = encodeRetrieveTxnResponse((RetrieveTxnResponse) msg);
                        break;
                    case AbstractMessage.OC_DUMP_HANDLES:
                        buf = encodeDumpHandlesResponse((DumpHandlesResponse) msg);
                        break;
                    case AbstractMessage.OC_GET_SITE_INFO:
                        buf = encodeGetSiteInfoResponse((GetSiteInfoResponse) msg);
                        break;
                    case AbstractMessage.OC_SESSION_SETUP:
                        buf = encodeSessionSetupResponse((SessionSetupResponse) msg);
                        break;
                    case AbstractMessage.OC_CREATE_HANDLE:
                        buf = encodeCreateHandleResponse((CreateHandleResponse) msg);
                        break;

                    case AbstractMessage.OC_UNHOME_NA:
                    case AbstractMessage.OC_HOME_NA:
                    case AbstractMessage.OC_DELETE_HANDLE:
                    case AbstractMessage.OC_ADD_VALUE:
                    case AbstractMessage.OC_MODIFY_VALUE:
                    case AbstractMessage.OC_REMOVE_VALUE:
                    case AbstractMessage.OC_BACKUP_SERVER:
                    case AbstractMessage.OC_SESSION_TERMINATE:
                    case AbstractMessage.OC_SESSION_EXCHANGEKEY:
                        buf = encodeGenericResponse(msg);
                        break;
                    case AbstractMessage.OC_RESPONSE_TO_CHALLENGE:
                        throw new HandleException(HandleException.INTERNAL_ERROR, "Invalid response message opCode " + msg.opCode);
                    default:
                        throw new HandleException(HandleException.INTERNAL_ERROR, "Unknown opCode: " + msg.opCode);
                }
                break;
            case AbstractMessage.RC_PREFIX_REFERRAL:
            case AbstractMessage.RC_SERVICE_REFERRAL:
                buf = encodeServiceReferralResponse((ServiceReferralResponse) msg);
                break;
            case AbstractMessage.RC_ERROR:
            case AbstractMessage.RC_AUTHEN_ERROR:
            case AbstractMessage.RC_INVALID_ADMIN:
            case AbstractMessage.RC_INSUFFICIENT_PERMISSIONS:
            case AbstractMessage.RC_AUTHENTICATION_FAILED:
            case AbstractMessage.RC_INVALID_CREDENTIAL:
            case AbstractMessage.RC_AUTHEN_TIMEOUT:
            case AbstractMessage.RC_VALUES_NOT_FOUND:
            case AbstractMessage.RC_HANDLE_NOT_FOUND:
            case AbstractMessage.RC_HANDLE_ALREADY_EXISTS:
            case AbstractMessage.RC_VALUE_ALREADY_EXISTS:
            case AbstractMessage.RC_INVALID_VALUE:
            case AbstractMessage.RC_OUT_OF_DATE_SITE_INFO:
            case AbstractMessage.RC_SERVER_NOT_RESP:
            case AbstractMessage.RC_SERVER_BACKUP:
            case AbstractMessage.RC_SESSION_FAILED:
            case AbstractMessage.RC_SESSION_TIMEOUT:
            case AbstractMessage.RC_INVALID_SESSION_KEY:
            case AbstractMessage.RC_NEED_RSAKEY_FOR_SESSIONEXCHANGE:
            case AbstractMessage.RC_INVALID_SESSIONSETUP_REQUEST:
            case AbstractMessage.RC_SESSION_MESSAGE_REJECTED:
            case AbstractMessage.RC_SERVER_TOO_BUSY:
            case AbstractMessage.RC_PROTOCOL_ERROR:
            case AbstractMessage.RC_OPERATION_NOT_SUPPORTED:
            case AbstractMessage.RC_RECURSION_COUNT_TOO_HIGH:
            case AbstractMessage.RC_INVALID_HANDLE:
            case AbstractMessage.RC_CLIENT_CHANNEL_ERROR:
            case AbstractMessage.RC_CLIENT_TIME_OUT:
            case AbstractMessage.RC_REQUEST_LIMIT_DAILY:
            case AbstractMessage.RC_REGISTER_COUNT_LIMIT:
            case AbstractMessage.RC_PREFIX_LIMIT:
                buf = encodeErrorMessage((ErrorResponse) msg);
                break;
            case AbstractMessage.RC_AUTHENTICATION_NEEDED:
                buf = encodeChallenge((ChallengeResponse) msg);
                break;
            default:
                throw new HandleException(HandleException.INTERNAL_ERROR, "Unknown responseCode: " + msg.responseCode);
        }

        if (buf == null)
            throw new HandleException(HandleException.INTERNAL_ERROR, "Encoder.encodeMessage() not implemented for " + "type: " + msg.opCode);
        return buf;
    }

    /*******************************************************************************
     * Write the encoded value of the given message envelope to the
     * given buffer.
     *******************************************************************************/
    public static final void encodeEnvelope(MessageEnvelope msgEnv, byte udpPkt[]) {
        udpPkt[0] = msgEnv.protocolMajorVersion;
        udpPkt[1] = msgEnv.protocolMinorVersion;
        udpPkt[2] = (byte) ((msgEnv.compressed ? ENV_FLAG_COMPRESSED : 0) | (msgEnv.encrypted ? ENV_FLAG_ENCRYPTED : 0) | (msgEnv.truncated ? ENV_FLAG_TRUNCATED : 0) | msgEnv.suggestMajorProtocolVersion);
        udpPkt[3] = msgEnv.suggestMinorProtocolVersion;
        writeInt(udpPkt, 4, msgEnv.sessionId); // bytes 4,5,6,7
        writeInt(udpPkt, 8, msgEnv.requestId); // bytes 8,9,10,11
        writeInt(udpPkt, 12, msgEnv.messageId); // bytes 12,13,14,15
        writeInt(udpPkt, 16, msgEnv.messageLength); // bytes 16,17,18,19
    }

    /*******************************************************************************
     * Read the encoded value of the given buffer and populate the fields
     * of the given message envelope object.
     *******************************************************************************/
    public static final void decodeEnvelope(byte udpPkt[], MessageEnvelope msgEnv) throws HandleException {
        if (udpPkt == null || udpPkt.length < Common.MESSAGE_ENVELOPE_SIZE)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Invalid message envelope");
        msgEnv.protocolMajorVersion = udpPkt[0];
        msgEnv.protocolMinorVersion = udpPkt[1];
        msgEnv.compressed = (udpPkt[2] & ENV_FLAG_COMPRESSED) != 0;
        msgEnv.encrypted = (udpPkt[2] & ENV_FLAG_ENCRYPTED) != 0;
        msgEnv.truncated = (udpPkt[2] & ENV_FLAG_TRUNCATED) != 0;
        msgEnv.suggestMajorProtocolVersion = (byte) (udpPkt[2] & 0x03);
        msgEnv.suggestMinorProtocolVersion = udpPkt[3];
        if (msgEnv.suggestMajorProtocolVersion == 0) {
            msgEnv.suggestMajorProtocolVersion = msgEnv.protocolMajorVersion;
            msgEnv.suggestMinorProtocolVersion = msgEnv.protocolMinorVersion;
        }
        msgEnv.sessionId = readInt(udpPkt, 4);
        msgEnv.requestId = readInt(udpPkt, 8);
        msgEnv.messageId = readInt(udpPkt, 12);
        msgEnv.messageLength = readInt(udpPkt, 16);
        if (msgEnv.messageLength > Common.MAX_MESSAGE_LENGTH || msgEnv.messageLength < 0)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Invalid message length: " + msgEnv.messageLength);
    }

    /*******************************************************************************
     * Encode a NextTxnIdResponse object and return the buffer with the encoding.
     *******************************************************************************/
    static final byte[] encodeNextTxnIdResponse(NextTxnIdResponse res) {
        int bodyLen = LONG_SIZE + (res.returnRequestDigest ? 1 + res.requestDigest.length : 0);
        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int offset = writeHeader(res, msg, bodyLen);

        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }

        offset += writeLong(msg, offset, res.nextTxnId);
        return msg;
    }

    /*******************************************************************************
     * Decode, create, and return a NextTxnIdResponse object from the given buffer
     *******************************************************************************/
    static final NextTxnIdResponse decodeNextTxnIdResponse(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) {
        long nextTxnId = readLong(msg, offset);
        return new NextTxnIdResponse(nextTxnId);
    }

    /*******************************************************************************
     * Decode, create, and return an AddValueRequest object from the given buffer
     *******************************************************************************/
    public static final AddValueRequest decodeAddValueRequest(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        byte handle[];
        HandleValue values[];

        handle = readByteArray(msg, offset);
        offset += INT_SIZE + handle.length;
        values = new HandleValue[readInt(msg, offset)];
        offset += INT_SIZE;
        for (int i = 0; i < values.length; i++) {
            values[i] = new HandleValue();
            offset += decodeHandleValue(msg, offset, values[i]);
        }
        return new AddValueRequest(handle, values, null);
    }

    /*******************************************************************************
     * Encode an AddValueRequest object and return the buffer with the encoding.
     *******************************************************************************/
    public static final byte[] encodeAddValueRequest(AddValueRequest req) {
        int bodyLen = INT_SIZE + // space for the handle length
                req.handle.length + // space for the handle itself
                INT_SIZE; // space for the number of values

        // now, for each value, calculate the storage size needed
        for (HandleValue value : req.values) {
            bodyLen += calcStorageSize(value);
        }
        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int loc = writeHeader(req, msg, bodyLen);

        // write the handle
        loc += writeByteArray(msg, loc, req.handle, 0, req.handle.length);

        loc += writeInt(msg, loc, req.values.length);
        // write the value
        for (HandleValue value : req.values) {
            loc += encodeHandleValue(msg, loc, value);
        }
        return msg;
    }

    /*******************************************************************************
     * Decode, create, and return a ModifyValueRequest object from the given buffer
     *******************************************************************************/
    public static final ModifyValueRequest decodeModifyValueRequest(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        byte handle[];
        HandleValue values[];

        handle = readByteArray(msg, offset);
        offset += INT_SIZE + handle.length;

        int numValues = readInt(msg, offset);
        offset += INT_SIZE;

        values = new HandleValue[numValues];
        for (int i = 0; i < numValues; i++) {
            values[i] = new HandleValue();
            offset += decodeHandleValue(msg, offset, values[i]);
        }
        return new ModifyValueRequest(handle, values, null);
    }

    /*******************************************************************************
     * Encode a ModifyValueRequest object and return the buffer with the encoding.
     *******************************************************************************/
    public static final byte[] encodeModifyValueRequest(ModifyValueRequest req) {
        int bodyLen = INT_SIZE + // space for the handle length
                req.handle.length + // space for the handle itself
                INT_SIZE; // space for the number of values
        for (HandleValue value : req.values) {
            bodyLen += calcStorageSize(value); // space for the value
        }

        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int loc = writeHeader(req, msg, bodyLen);

        // write the handle
        loc += writeByteArray(msg, loc, req.handle, 0, req.handle.length);

        // write the values
        loc += writeInt(msg, loc, req.values.length);
        for (HandleValue value : req.values) {
            loc += encodeHandleValue(msg, loc, value);
        }
        return msg;
    }

    /*******************************************************************************
     * Decode, create, and return a RemoveValueRequest object from the given buffer
     *******************************************************************************/
    public static final RemoveValueRequest decodeRemoveValueRequest(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        byte handle[];
        int indexes[];

        handle = readByteArray(msg, offset);
        offset += INT_SIZE + handle.length;

        indexes = readIntArray(msg, offset);
        offset += INT_SIZE + indexes.length * INT_SIZE;
        return new RemoveValueRequest(handle, indexes, null);
    }

    /*******************************************************************************
     * Encode a RemoveValueRequest object and return the buffer with the encoding.
     *******************************************************************************/
    public static final byte[] encodeRemoveValueRequest(RemoveValueRequest req) {
        int bodyLen = INT_SIZE + // space for the handle length
                req.handle.length + // space for the handle itself
                INT_SIZE + // space for the number of handle indexes
                req.indexes.length * INT_SIZE; // space for the handle indexes

        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int loc = writeHeader(req, msg, bodyLen);

        // write the handle
        loc += writeByteArray(msg, loc, req.handle, 0, req.handle.length);

        // write the index
        loc += writeIntArray(msg, loc, req.indexes);

        return msg;
    }

    /*******************************************************************************
     * Encode a VerifyAuthRequest object and return the buffer with the encoding.
     *******************************************************************************/
    static final byte[] encodeVerifyAuthRequest(VerifyAuthRequest req) {
        int bodyLen = INT_SIZE + INT_SIZE + req.handle.length + INT_SIZE + req.nonce.length + INT_SIZE + req.origRequestDigest.length + INT_SIZE + req.signedResponse.length;

        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int offset = 0;
        offset += writeHeader(req, msg, bodyLen);
        offset += writeByteArray(msg, offset, req.handle);
        offset += writeInt(msg, offset, req.handleIndex);
        offset += writeByteArray(msg, offset, req.nonce);
        offset += writeByteArray(msg, offset, req.origRequestDigest);
        offset += writeByteArray(msg, offset, req.signedResponse);
        return msg;
    }

    /*******************************************************************************
     * Decode, create, and return a VerifyAuthRequest object from the given buffer
     *******************************************************************************/
    static final VerifyAuthRequest decodeVerifyAuthRequest(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        byte handle[] = readByteArray(msg, offset);
        offset += INT_SIZE + handle.length;
        int handleIndex = readInt(msg, offset);
        offset += INT_SIZE;
        byte nonce[] = readByteArray(msg, offset);
        offset += INT_SIZE + nonce.length;

        byte requestDigest[] = null;
        byte digestAlg = msg[offset++];
        switch (digestAlg) {
            case 0:
                // This is the only format currently in use
                // really indicates a length
                offset--;
                requestDigest = readByteArray(msg, offset);
                offset += INT_SIZE + requestDigest.length;
                break;
            case Common.HASH_CODE_MD5:
                requestDigest = new byte[Common.MD5_DIGEST_SIZE];
                System.arraycopy(msg, offset, requestDigest, 0, Common.MD5_DIGEST_SIZE);
                offset += Common.MD5_DIGEST_SIZE;
                break;
            case Common.HASH_CODE_SHA1:
                requestDigest = new byte[Common.SHA1_DIGEST_SIZE];
                System.arraycopy(msg, offset, requestDigest, 0, Common.SHA1_DIGEST_SIZE);
                offset += Common.SHA1_DIGEST_SIZE;
                break;
            case Common.HASH_CODE_SHA256:
                requestDigest = new byte[Common.SHA256_DIGEST_SIZE];
                System.arraycopy(msg, offset, requestDigest, 0, Common.SHA256_DIGEST_SIZE);
                offset += Common.SHA256_DIGEST_SIZE;
                break;
            default:
                throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Invalid hash type in message: " + ((int) digestAlg));
        }

        byte signedResponse[] = readByteArray(msg, offset);
        offset += INT_SIZE + signedResponse.length;

        return new VerifyAuthRequest(handle, nonce, requestDigest, digestAlg, signedResponse, handleIndex, null);
    }

    /*******************************************************************************
     * Decode, create, and return a GetSiteInfoResponse object from the given buffer
     *******************************************************************************/
    static final GetSiteInfoResponse decodeGetSiteInfoResponse(byte msg[], int offset, int len, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        byte[] b = new byte[len];
        System.arraycopy(msg, offset, b, 0, len);
        SiteInfo site = new SiteInfo();
        decodeSiteInfoRecord(b, 0, site);
        return new GetSiteInfoResponse(site);
    }

    /*******************************************************************************
     * Encode a GetSiteInfoResponse object into a buffer and return the result
     *******************************************************************************/
    static final byte[] encodeGetSiteInfoResponse(GetSiteInfoResponse res) {
        byte siteBuf[] = encodeSiteInfoRecord(res.siteInfo);
        int bodyLen = siteBuf.length + (res.returnRequestDigest ? 1 + res.requestDigest.length : 0);
        byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];
        int offset = writeHeader(res, msg, bodyLen);
        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }

        System.arraycopy(siteBuf, 0, msg, offset, siteBuf.length);
        return msg;
    }

    /*******************************************************************************
     * Encode a VerifyAuthRequest object into a buffer and return the result
     *******************************************************************************/
    static final byte[] encodeVerifyAuthResponse(VerifyAuthResponse res) {
        int bodyLen = 1 + (res.returnRequestDigest ? 1 + res.requestDigest.length : 0);
        byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];
        int offset = writeHeader(res, msg, bodyLen);

        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }

        msg[offset++] = (byte) (res.isValid ? 0x01 : 0x00);
        return msg;
    }

    /*******************************************************************************
     * Decode, create, and return a VerifyAuthResponse object from the given buffer
     *******************************************************************************/
    static final VerifyAuthResponse decodeVerifyAuthResponse(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) {
        boolean isValid = (msg[offset] & 0x01) != 0;
        return new VerifyAuthResponse(isValid);
    }

    /*******************************************************************************
     * Encode a RetrieveTxnRequest object into a buffer and return the result
     *******************************************************************************/
    static final byte[] encodeRetrieveTxnRequest(RetrieveTxnRequest req) {
        if (req.hasEqualOrGreaterVersion(2, 9)) {
            if (req.replicationStateInfo != null) {
                Gson gson = GsonUtility.getGson();
                String replicationStateInfoJson = gson.toJson(req.replicationStateInfo);
                byte[] replicationStateInfoJsonBytes = Util.encodeString(replicationStateInfoJson);

                int bodyLen = 1 + INT_SIZE + replicationStateInfoJsonBytes.length + 1 + INT_SIZE + INT_SIZE;
                byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];

                int offset = writeHeader(req, msg, bodyLen);
                msg[offset++] = 1;
                offset += writeByteArray(msg, offset, replicationStateInfoJsonBytes);
                msg[offset++] = req.rcvrHashType;
                offset += writeInt(msg, offset, req.numServers);
                offset += writeInt(msg, offset, req.serverNum);
                return msg;
            } else {
                int bodyLen = 1 + LONG_SIZE + LONG_SIZE + 1 + INT_SIZE + INT_SIZE;
                byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];

                int offset = writeHeader(req, msg, bodyLen);
                msg[offset++] = 0;
                offset += writeLong(msg, offset, req.lastTxnId);
                offset += writeLong(msg, offset, req.lastQueryDate);
                msg[offset++] = req.rcvrHashType;
                offset += writeInt(msg, offset, req.numServers);
                offset += writeInt(msg, offset, req.serverNum);
                return msg;
            }
        } else {
            int bodyLen = LONG_SIZE + LONG_SIZE + 1 + INT_SIZE + INT_SIZE;
            byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];

            int offset = writeHeader(req, msg, bodyLen);
            offset += writeLong(msg, offset, req.lastTxnId);
            offset += writeLong(msg, offset, req.lastQueryDate);
            msg[offset++] = req.rcvrHashType;
            offset += writeInt(msg, offset, req.numServers);
            offset += writeInt(msg, offset, req.serverNum);
            return msg;
        }
    }

    /*******************************************************************************
     * Encode a RetrieveTxnResponse object into a buffer and return the result
     *******************************************************************************/
    static final byte[] encodeRetrieveTxnResponse(RetrieveTxnResponse res) {
        int bodyLen = 1 + (res.returnRequestDigest ? 1 + res.requestDigest.length : 0);
        byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];

        int offset = writeHeader(res, msg, bodyLen);

        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }

        msg[offset] = (byte) 0;
        if (res.keepAlive) msg[offset] |= 0x01;
        return msg;
    }

    /*******************************************************************************
     * Decode, create, and return a RetrieveTxnResponse object from the given buffer
     *******************************************************************************/
    static final RetrieveTxnResponse decodeRetrieveTxnResponse(byte msg[], int loc, @SuppressWarnings("unused") MessageEnvelope env) {
        RetrieveTxnResponse res = new RetrieveTxnResponse();
        res.keepAlive = (msg[loc] & 0x01) != 0;
        return res;
    }

    /*******************************************************************************
     * Decode, create, and return a RetrieveTxnRequest object from the given buffer
     *******************************************************************************/
    static final RetrieveTxnRequest decodeRetrieveTxnRequest(byte msg[], int loc, MessageEnvelope env) throws HandleException {
        if (AbstractMessage.hasEqualOrGreaterVersion(env.protocolMajorVersion, env.protocolMinorVersion, 2, 9)) {
            byte isPullOtherTransactionsByte = msg[loc++];
            if (isPullOtherTransactionsByte == 1) {
                return decodeRetrieveTransactionRequestPullingAllSources(msg, loc);
            } else {
                return decodeRetrieveTransactionRequestPullingOnlyOneSource(msg, loc);
            }
        } else {
            return decodeRetrieveTransactionRequestPullingOnlyOneSource(msg, loc);
        }
    }

    private static RetrieveTxnRequest decodeRetrieveTransactionRequestPullingAllSources(byte[] msg, int loc) throws HandleException {
        byte[] replicationStateInfoJsonBytes = readByteArray(msg, loc);
        loc += replicationStateInfoJsonBytes.length + INT_SIZE;
        String replicationStateInfoJson = Util.decodeString(replicationStateInfoJsonBytes);
        ReplicationStateInfo replicationStateInfo = GsonUtility.getGson().fromJson(replicationStateInfoJson, ReplicationStateInfo.class);
        byte rcvrHashType = msg[loc++];
        int numServers = readInt(msg, loc);
        loc += INT_SIZE;
        int serverNum = readInt(msg, loc);
        loc += INT_SIZE;

        return new RetrieveTxnRequest(replicationStateInfo, rcvrHashType, numServers, serverNum, null);
    }

    private static RetrieveTxnRequest decodeRetrieveTransactionRequestPullingOnlyOneSource(byte[] msg, int loc) {
        long lastTxnId = readLong(msg, loc);
        loc += LONG_SIZE;
        long lastQueryDate = readLong(msg, loc);
        loc += LONG_SIZE;
        byte rcvrHashType = msg[loc++];
        int numServers = readInt(msg, loc);
        loc += INT_SIZE;
        int serverNum = readInt(msg, loc);
        loc += INT_SIZE;

        return new RetrieveTxnRequest(lastTxnId, lastQueryDate, rcvrHashType, numServers, serverNum, null);
    }

    /*******************************************************************************
     * Encode a DumpHandlesRequest object into a buffer and return the result
     *******************************************************************************/
    static final byte[] encodeDumpHandlesRequest(DumpHandlesRequest req) {
        int bodyLen = 1 + INT_SIZE + INT_SIZE;
        if (req.startingPoint != null) {
            bodyLen += INT_SIZE + req.startingPoint.length + INT_SIZE;
        }
        byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];

        int offset = writeHeader(req, msg, bodyLen);
        msg[offset++] = req.rcvrHashType;
        offset += writeInt(msg, offset, req.numServers);
        offset += writeInt(msg, offset, req.serverNum);

        if (req.startingPoint != null) {
            // write the startingPoint
            offset += writeByteArray(msg, offset, req.startingPoint, 0, req.startingPoint.length);
            offset += writeInt(msg, offset, req.startingPointType);
        }
        return msg;
    }

    /*******************************************************************************
     * Encode a DumpHandlesResponse object into a buffer and return the result
     *******************************************************************************/
    static final byte[] encodeDumpHandlesResponse(DumpHandlesResponse res) {
        int bodyLen = res.returnRequestDigest ? 1 + res.requestDigest.length : 0;
        byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];
        int offset = writeHeader(res, msg, bodyLen);
        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }
        return msg;
    }

    /*******************************************************************************
     * Decode, create, and return a DumpHandlesResponse object from the given buffer
     *******************************************************************************/
    @SuppressWarnings("unused")
    static final DumpHandlesResponse decodeDumpHandlesResponse(byte msg[], int loc, MessageEnvelope env) {
        return new DumpHandlesResponse();
    }

    /*******************************************************************************
     * Decode, create, and return a DumpHandlesRequest object from the given buffer
     * @throws HandleException
     *******************************************************************************/
    static final DumpHandlesRequest decodeDumpHandlesRequest(byte msg[], int loc, @SuppressWarnings("unused") MessageEnvelope env, int bodyLen) throws HandleException {
        int startOfBody = loc;
        byte rcvrHashType = msg[loc++];
        int numServers = readInt(msg, loc);
        loc += INT_SIZE;
        int serverNum = readInt(msg, loc);
        loc += INT_SIZE;
        if (loc == startOfBody + bodyLen) {
            return new DumpHandlesRequest(rcvrHashType, numServers, serverNum, null);
        } else {
            byte[] startingPoint = readByteArray(msg, loc);
            loc += INT_SIZE + startingPoint.length;
            int startingPointType = readInt(msg, loc);
            loc += INT_SIZE;
            return new DumpHandlesRequest(rcvrHashType, numServers, serverNum, null, startingPoint, startingPointType);
        }
    }

    /*******************************************************************************
     * Decode, create, and return a DeleteHandleRequest from the given buffer
     *******************************************************************************/
    public static final DeleteHandleRequest decodeDeleteHandleRequest(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        byte handle[];
        handle = readByteArray(msg, offset);
        return new DeleteHandleRequest(handle, null);
    }

    /*******************************************************************************
     * Encode the given DeleteHandleRequest and return the resulting buffer.
     *******************************************************************************/
    public static final byte[] encodeDeleteHandleRequest(DeleteHandleRequest req) {
        int bodyLen = INT_SIZE + // space for the handle length
                req.handle.length; // space for the handle itself

        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int loc = writeHeader(req, msg, bodyLen);

        // write the handle
        loc += writeByteArray(msg, loc, req.handle, 0, req.handle.length);
        return msg;
    }

    /*******************************************************************************
     * Decode the given generic message (ie no body, identified only by the opCode
     * and responseCode) from the given buffer.
     *******************************************************************************/
    @SuppressWarnings("unused")
    public static final GenericResponse decodeGenericResponse(byte msg[], int loc, MessageEnvelope env) {
        return new GenericResponse();
    }

    /*******************************************************************************
     * Encode the given generic message (identified only by the opCode, and responseCode)
     * and return the resulting buffer.
     *******************************************************************************/
    public static final byte[] encodeGenericResponse(AbstractMessage res) {
        // generic responses have no fields
        int bodyLen = res.returnRequestDigest ? res.requestDigest.length + 1 : 0;
        byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];

        int offset = writeHeader(res, msg, bodyLen);

        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }

        return msg;
    }

    /*******************************************************************************
     * Decode, create, and return a CreateHandleRequest from the given buffer
     *******************************************************************************/
    public static final CreateHandleRequest decodeCreateHandleRequest(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env, @SuppressWarnings("unused") int opCode) throws HandleException {
        byte handle[];
        HandleValue values[];

        handle = readByteArray(msg, offset);
        offset += INT_SIZE + handle.length;
        values = new HandleValue[readInt(msg, offset)];
        offset += INT_SIZE;
        for (int i = 0; i < values.length; i++) {
            values[i] = new HandleValue();
            offset += decodeHandleValue(msg, offset, values[i]);
        }
        return new CreateHandleRequest(handle, values, null);
    }

    /*******************************************************************************
     * Encode the given CreateHandleRequest and return the resulting buffer.
     *******************************************************************************/
    public static final byte[] encodeCreateHandleRequest(CreateHandleRequest req) {
        int bodyLen = INT_SIZE + // space for the handle length
                req.handle.length + // space for the handle itself
                INT_SIZE; // space for value list length

        // add the size for each value
        for (HandleValue value : req.values) {
            bodyLen += calcStorageSize(value);
        }
        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int loc = writeHeader(req, msg, bodyLen);

        // write the handle
        loc += writeByteArray(msg, loc, req.handle, 0, req.handle.length);

        loc += writeInt(msg, loc, req.values.length);

        for (HandleValue value : req.values) {
            loc += encodeHandleValue(msg, loc, value);
        }
        return msg;
    }

    /*******************************************************************************
     * Decode and create a list-handles request object from the given buffer.
     *******************************************************************************/
    static ListHandlesRequest decodeListHandlesRequest(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        // read the handle
        byte naHandle[] = readByteArray(msg, offset);
        offset += (naHandle.length + INT_SIZE);

        return new ListHandlesRequest(naHandle, null);
    }

    /*******************************************************************************
     * Decode and create a list-handles request object from the given buffer.
     *******************************************************************************/
    static ListNAsRequest decodeListNAsRequest(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        // read the handle
        byte naHandle[] = readByteArray(msg, offset);
        offset += (naHandle.length + INT_SIZE);

        return new ListNAsRequest(naHandle, null);
    }

    /*******************************************************************************
     * Decode and create a list-handles response object from the given buffer.
     *******************************************************************************/
    static ListHandlesResponse decodeListHandlesResponse(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        int numHandles = readInt(msg, offset);
        offset += INT_SIZE;

        byte handles[][] = new byte[numHandles][];
        for (int i = 0; i < numHandles; i++) {
            handles[i] = readByteArray(msg, offset);
            offset += INT_SIZE + handles[i].length;
        }

        ListHandlesResponse response = new ListHandlesResponse();
        response.handles = handles;
        return response;
    }

    /*******************************************************************************
     * Decode and create a list-handles response object from the given buffer.
     *******************************************************************************/
    static ListNAsResponse decodeListNAsResponse(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        int numHandles = readInt(msg, offset);
        offset += INT_SIZE;

        byte handles[][] = new byte[numHandles][];
        for (int i = 0; i < numHandles; i++) {
            handles[i] = readByteArray(msg, offset);
            offset += INT_SIZE + handles[i].length;
        }

        ListNAsResponse response = new ListNAsResponse();
        response.handles = handles;
        return response;
    }

    /*******************************************************************************
     * Encode the given ListHandlesRequest and return the resulting buffer
     *******************************************************************************/
    static final byte[] encodeListHandlesRequest(ListHandlesRequest req) {
        int bodyLen = INT_SIZE + req.handle.length; // space for the NA handle

        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        writeHeader(req, msg, bodyLen);

        int loc = Common.MESSAGE_HEADER_SIZE;

        // write the NA handle
        loc += writeByteArray(msg, loc, req.handle, 0, req.handle.length);
        return msg;
    }

    /*******************************************************************************
     * Encode the given ListHandlesRequest and return the resulting buffer
     *******************************************************************************/
    static final byte[] encodeListNAsRequest(ListNAsRequest req) {
        int bodyLen = INT_SIZE + req.handle.length; // space for the NA handle

        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        writeHeader(req, msg, bodyLen);

        int loc = Common.MESSAGE_HEADER_SIZE;

        // write the NA handle
        loc += writeByteArray(msg, loc, req.handle, 0, req.handle.length);
        return msg;
    }

    /*******************************************************************************
     * Encode the given ListHandlesResponse and return the resulting buffer
     *******************************************************************************/
    static final byte[] encodeListHandlesResponse(ListHandlesResponse res) {
        int bodyLen = INT_SIZE + // space for value list length
                (res.returnRequestDigest ? 1 + res.requestDigest.length : 0); // request digest

        // add the size for each handle
        for (byte[] handle : res.handles) {
            bodyLen += (handle.length) + INT_SIZE;
        }
        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int offset = writeHeader(res, msg, bodyLen);

        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }

        // write the number of handles...
        offset += writeInt(msg, offset, res.handles.length);

        // write each handle...
        for (byte[] handle : res.handles) {
            offset += writeByteArray(msg, offset, handle);
        }
        return msg;
    }

    /*******************************************************************************
     * Encode the given ListNAsResponse and return the resulting buffer
     *******************************************************************************/
    static final byte[] encodeListNAsResponse(ListNAsResponse res) {
        int bodyLen = INT_SIZE + // space for value list length
                (res.returnRequestDigest ? 1 + res.requestDigest.length : 0); // request digest

        // add the size for each handle
        for (byte[] handle : res.handles) {
            bodyLen += (handle.length) + INT_SIZE;
        }
        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int offset = writeHeader(res, msg, bodyLen);

        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }

        // write the number of handles...
        offset += writeInt(msg, offset, res.handles.length);

        // write each handle...
        for (byte[] handle : res.handles) {
            offset += writeByteArray(msg, offset, handle);
        }
        return msg;
    }

    /*******************************************************************************
     * Decode and create a resolution request object from the given buffer.
     *******************************************************************************/
    public static ResolutionRequest decodeResolutionRequest(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        // read the handle
        byte handle[] = readByteArray(msg, offset);
        offset += (handle.length + INT_SIZE);

        // read the index list
        int indexes[] = readIntArray(msg, offset);
        offset += INT_SIZE + INT_SIZE * indexes.length;

        // read the type list
        byte types[][] = new byte[readInt(msg, offset)][];
        offset += INT_SIZE;
        offset += readByteArrayArray(types, msg, offset);

        return new ResolutionRequest(handle, types, indexes, null);
    }

    /*******************************************************************************
     * Decode and create a resolution response object from the given buffer.
     *******************************************************************************/
    public static ResolutionResponse decodeResolutionResponse(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        int handleLen = readInt(msg, offset);
        offset += INT_SIZE;

        if (handleLen < 0 || handleLen > Common.MAX_HANDLE_LENGTH)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Invalid handle length: " + handleLen);

        byte handle[] = new byte[handleLen];
        System.arraycopy(msg, offset, handle, 0, handleLen);
        offset += handleLen;

        int numValues = readInt(msg, offset);
        offset += INT_SIZE;

        if (numValues < 0 || numValues > Common.MAX_HANDLE_VALUES)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Invalid number of values: " + numValues);

        byte values[][] = new byte[numValues][];
        for (int i = 0; i < numValues; i++) {
            int valLen = calcHandleValueSize(msg, offset);
            values[i] = new byte[valLen];
            System.arraycopy(msg, offset, values[i], 0, valLen);
            offset += valLen;
        }

        return new ResolutionResponse(handle, values);
    }

    public static ServiceReferralResponse decodeServiceReferralResponse(int responseCode, byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env, int endOfBuf) throws HandleException {
        int handleLen = readInt(msg, offset);
        offset += INT_SIZE;

        if (handleLen < 0 || handleLen > Common.MAX_HANDLE_LENGTH)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Invalid handle length: " + handleLen);

        byte handle[] = new byte[handleLen];
        System.arraycopy(msg, offset, handle, 0, handleLen);
        offset += handleLen;

        byte[][] values = null;
        if (offset < endOfBuf) {
            int numValues = readInt(msg, offset);
            offset += INT_SIZE;

            if (numValues < 0 || numValues > Common.MAX_HANDLE_VALUES)
                throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Invalid number of values: " + numValues);

            values = new byte[numValues][];
            for (int i = 0; i < numValues; i++) {
                int valLen = calcHandleValueSize(msg, offset);
                values[i] = new byte[valLen];
                System.arraycopy(msg, offset, values[i], 0, valLen);
                offset += valLen;
            }
        }
        return new ServiceReferralResponse(responseCode, handle, values);
    }

    /*******************************************************************************
     * Encode the given ResolutionRequest and return the resulting buffer
     *******************************************************************************/
    public static final byte[] encodeResolutionRequest(ResolutionRequest req) {
        int bodyLen = INT_SIZE + // space for the handle length
                req.handle.length + // space for the handle itself

                INT_SIZE + // space for the index list length
                // space for the index list
                ((req.requestedIndexes == null) ? 0 : req.requestedIndexes.length * INT_SIZE) +

                INT_SIZE; // space for the type list length

        if (req.requestedTypes != null) { // add the size for each type item
            for (byte[] requestedType : req.requestedTypes) {
                bodyLen += (requestedType.length + INT_SIZE);
            }
        }

        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        writeHeader(req, msg, bodyLen);

        int loc = Common.MESSAGE_HEADER_SIZE;

        // write the handle
        loc += writeByteArray(msg, loc, req.handle, 0, req.handle.length);

        // write the index list
        loc += writeIntArray(msg, loc, req.requestedIndexes);

        // write the type list
        loc += writeByteArrayArray(msg, loc, req.requestedTypes);

        return msg;
    }

    /*******************************************************************************
     * Encode the given ResolutionResponse and return the resulting buffer
     *******************************************************************************/
    public static final byte[] encodeResolutionResponse(ResolutionResponse res) {
        int bodyLen = INT_SIZE + // space for the handle length
                res.handle.length + // space for the handle itself

                INT_SIZE + // space for value list length
                (res.returnRequestDigest ? 1 + res.requestDigest.length : 0); // request digest

        // add the size for each return value
        for (byte[] value : res.values) {
            bodyLen += (value.length);
        }
        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int offset = writeHeader(res, msg, bodyLen);

        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }

        // write the handle
        offset += writeByteArray(msg, offset, res.handle, 0, res.handle.length);

        // write the values...
        offset += writeInt(msg, offset, res.values.length);

        for (byte[] value : res.values) {
            System.arraycopy(value, 0, msg, offset, value.length);
            offset += value.length;
        }
        return msg;
    }

    public static final byte[] encodeServiceReferralResponse(ServiceReferralResponse res) {
        int bodyLen = INT_SIZE + // space for the handle length
                res.handle.length + // space for the handle itself
                (res.returnRequestDigest ? 1 + res.requestDigest.length : 0); // request digest

        if (res.values != null) {
            bodyLen += INT_SIZE; // space for value list length
            // add the size for each return value
            for (byte[] value : res.values) {
                bodyLen += (value.length);
            }
        }

        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int offset = writeHeader(res, msg, bodyLen);

        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }

        // write the handle
        offset += writeByteArray(msg, offset, res.handle, 0, res.handle.length);

        if (res.values != null) {
            // write the values...
            offset += writeInt(msg, offset, res.values.length);

            for (byte[] value : res.values) {
                System.arraycopy(value, 0, msg, offset, value.length);
                offset += value.length;
            }
        }
        return msg;
    }

    /*******************************************************************************
     * Decode, create, and return a ChallengeResponse object from the given buffer
     *******************************************************************************/
    static final ChallengeResponse decodeChallenge(byte msg[], int offset, int opCode, MessageEnvelope env) throws HandleException {
        byte nonce[] = null;

        if ((env.protocolMajorVersion == 5 && env.protocolMinorVersion == 0) || (env.protocolMajorVersion == 2 && env.protocolMinorVersion == 0)) {
            // keep so we stay compatible with 2.0 clients (which were incorrectly identified as 5.0)
            nonce = readByteArray(msg, offset);
            offset += INT_SIZE + nonce.length;
            ChallengeResponse cr = new ChallengeResponse(opCode, nonce);

            cr.rdHashType = Common.HASH_CODE_MD5_OLD_FORMAT;
            cr.requestDigest = readByteArray(msg, offset);
            offset += INT_SIZE + cr.requestDigest.length;

            return cr;
        } else {
            // in newer versions of the protocol the request digest is an optional part
            // of every message, so it is read in decodeMessage()
            nonce = readByteArray(msg, offset);
            offset += INT_SIZE + nonce.length;

            return new ChallengeResponse(opCode, nonce);
        }

    }

    /*******************************************************************************
     * Decode, create, and return a ChallengeAnswerRequest from the given buffer
     *******************************************************************************/
    static final ChallengeAnswerRequest decodeChallengeAnswer(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        byte authType[];
        byte userIdHandle[];
        int userIdIndex;

        authType = readByteArray(msg, offset);
        offset += INT_SIZE + authType.length;

        userIdHandle = readByteArray(msg, offset);
        offset += INT_SIZE + userIdHandle.length;

        userIdIndex = readInt(msg, offset);
        offset += INT_SIZE;
        byte signedResponse[] = readByteArray(msg, offset);
        return new ChallengeAnswerRequest(authType, userIdHandle, userIdIndex, signedResponse, null);
    }

    /*******************************************************************************
     * Decode, create, and return a list of handle value references (handle/index
     * pairs) from the given buffer.
     *******************************************************************************/
    public static final ValueReference[] decodeValueReferenceList(byte buf[], int offset) throws HandleException {
        int numValues = readInt(buf, offset);
        offset += INT_SIZE;

        if (numValues < 0 || numValues > Common.MAX_ARRAY_SIZE)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, MSG_INVALID_ARRAY_SIZE);

        ValueReference values[] = new ValueReference[numValues];
        for (int i = 0; i < numValues; i++) {
            values[i] = new ValueReference();
            values[i].handle = readByteArray(buf, offset);
            offset += INT_SIZE + values[i].handle.length;
            values[i].index = readInt(buf, offset);
            offset += INT_SIZE;
        }
        if (offset < buf.length)
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Unexpected data remaining after decoding");

        return values;
    }

    /*******************************************************************************
     * Encode the given list of handle value references (handle/index pairs)
     * and return the resulting byte array.
     *******************************************************************************/
    public static final byte[] encodeValueReferenceList(ValueReference values[]) {
        int sz = INT_SIZE;
        if (values != null) {
            for (ValueReference value : values) {
                sz += INT_SIZE;
                sz += INT_SIZE;
                sz += value.handle.length;
            }
        }
        byte buf[] = new byte[sz];
        int offset = 0;
        offset += writeInt(buf, offset, values == null ? 0 : values.length);
        if (values != null) {
            for (ValueReference value : values) {
                offset += writeByteArray(buf, offset, value.handle);
                offset += writeInt(buf, offset, value.index);
            }
        }
        return buf;
    }

    /*******************************************************************************
     * Encode the given ChallengeAnswerRequest and return the resulting buffer.
     *******************************************************************************/
    static final byte[] encodeChallengeAnswer(ChallengeAnswerRequest req) {
        byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + INT_SIZE + // size of the handle being operated on
                req.authType.length + INT_SIZE + // size of the user ID handle + length field
                req.userIdHandle.length + INT_SIZE + // user ID index
                INT_SIZE + // response to the challenge + length field
                req.signedResponse.length];
        int offset = writeHeader(req, msg, msg.length - Common.MESSAGE_HEADER_SIZE);
        offset += writeByteArray(msg, offset, req.authType, 0, req.authType.length);
        offset += writeByteArray(msg, offset, req.userIdHandle, 0, req.userIdHandle.length);
        offset += writeInt(msg, offset, req.userIdIndex);
        offset += writeByteArray(msg, offset, req.signedResponse, 0, req.signedResponse.length);
        return msg;
    }

    /*******************************************************************************
     * Encode the given ChallengeResponse object and return the resulting buffer.
     *******************************************************************************/
    static final byte[] encodeChallenge(ChallengeResponse res) {
        byte msg[];

        if (!res.hasEqualOrGreaterVersion(2, 1)) {
            // write the old message format if the message is for an old client
            // (or a new client thinking they are talking to an old server)
            msg = new byte[Common.MESSAGE_HEADER_SIZE + INT_SIZE + // size of the nonce + length field
                    res.nonce.length + INT_SIZE + // size of the request digest + length field
                    res.requestDigest.length];
            int offset = writeHeader(res, msg, msg.length - Common.MESSAGE_HEADER_SIZE);

            offset += writeByteArray(msg, offset, res.nonce, 0, res.nonce.length);
            offset += writeByteArray(msg, offset, res.requestDigest, 0, res.requestDigest.length);
        } else {
            msg = new byte[Common.MESSAGE_HEADER_SIZE + INT_SIZE + // size of the nonce + length field
                    res.nonce.length + (res.returnRequestDigest ? 1 + res.requestDigest.length : 0)];
            int offset = writeHeader(res, msg, msg.length - Common.MESSAGE_HEADER_SIZE);

            if (res.returnRequestDigest) {
                msg[offset++] = res.rdHashType;
                System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
                offset += res.requestDigest.length;
            }

            offset += writeByteArray(msg, offset, res.nonce, 0, res.nonce.length);
        }
        return msg;
    }

    /*******************************************************************************
     * Encode the given ErrorResponse object and return the resulting buffer
     *******************************************************************************/
    static final byte[] encodeErrorMessage(ErrorResponse res) {
        int bodyLen = INT_SIZE;

        if (res.message != null) {
            bodyLen += res.message.length;
        }
        if (res.returnRequestDigest) {
            bodyLen += 1 + res.requestDigest.length;
        }

        byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];
        int offset = writeHeader(res, msg, bodyLen);

        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }

        if (res.message != null) {
            offset += writeInt(msg, offset, res.message.length);
            System.arraycopy(res.message, 0, msg, offset, res.message.length);
        } else {
            offset += writeInt(msg, offset, 0);
        }
        return msg;
    }

    /*******************************************************************************
     * Encode the given ErrorResponse object and return the resulting buffer
     *******************************************************************************/
    static final AbstractResponse decodeErrorMessage(byte[] msg, int loc, MessageEnvelope env, int endOfBuf) throws HandleException {
        if (env.protocolMajorVersion == 5 || (env.protocolMajorVersion == 2 && env.protocolMinorVersion == 0)) {
            if (loc + INT_SIZE < endOfBuf) {
                return new ErrorResponse(readByteArray(msg, loc));
            } else {
                return new ErrorResponse(new byte[0]);
            }
        } else { // talking to a newer client >= 2.1
            return new ErrorResponse(readByteArray(msg, loc));
        }
    }

    /*******************************************************************************
     * Write the message header for the given message into the given buffer,
     * starting at the specified location.  This will return the number of
     * bytes written (which will always be the same as Common.MESSAGE_HEADER_SIZE).
     *******************************************************************************/
    static final int writeHeader(AbstractMessage msg, byte buf[], int bodyLen) {
        int loc = 0;

        // write the opCode
        loc += writeInt(buf, loc, msg.opCode);

        // write the response code
        if (msg.responseCode == AbstractMessage.RC_SERVER_BACKUP && !msg.hasEqualOrGreaterVersion(2, 5)) {
            loc += writeInt(buf, loc, AbstractMessage.RC_PREFIX_REFERRAL);
        } else if (msg.responseCode == AbstractMessage.RC_INVALID_VALUE && !msg.hasEqualOrGreaterVersion(2, 5)) {
            loc += writeInt(buf, loc, AbstractMessage.RC_VALUE_ALREADY_EXISTS);
        } else loc += writeInt(buf, loc, msg.responseCode);

        // write the header flags
        int flags = 0;
        if (msg.authoritative) flags |= MSG_FLAG_AUTH;
        if (msg.certify) flags |= MSG_FLAG_CERT;
        if (msg.encrypt) flags |= MSG_FLAG_ENCR;
        if (msg.recursive) flags |= MSG_FLAG_RECU;
        if (msg.cacheCertify) flags |= MSG_FLAG_CACR;
        if (msg.continuous) flags |= MSG_FLAG_CONT;
        if (msg.keepAlive) flags |= MSG_FLAG_KPAL;
        if (msg.ignoreRestrictedValues) flags |= MSG_FLAG_PUBL;
        if (msg.hasEqualOrGreaterVersion(2, 1) && msg.returnRequestDigest) flags |= MSG_FLAG_RRDG;
        if (msg.overwriteWhenExists) flags |= MSG_FLAG_OVRW; // introduced in 2.3 but we send regardless
        if (msg.mintNewSuffix) flags |= MSG_FLAG_MINT;
        if (msg.doNotRefer) flags |= MSG_FLAG_DNRF;

        loc += writeInt(buf, loc, flags);

        // write the version of the siteInfo that is being used...
        loc += writeInt2(buf, loc, msg.siteInfoSerial);

        buf[loc++] = (byte) msg.recursionCount;
        loc++; // 1 reserved byte

        loc += writeInt(buf, loc, msg.expiration); // message expiration

        // write the body length
        loc += writeInt(buf, loc, bodyLen);

        return Common.MESSAGE_HEADER_SIZE;
    }

    /*******************************************************************************
     * Encode the given handle values into a buffer that can be used as the global
     * (or root) service/site information.
     *******************************************************************************/
    public static final byte[] encodeGlobalValues(HandleValue values[]) {
        int dataLen = INT_SIZE + // the length of the data
                INT_SIZE; // the number of values
        for (HandleValue value : values) {
            dataLen += calcStorageSize(value);
        }
        byte buf[] = new byte[dataLen];
        int offset = 0;

        offset += writeInt(buf, offset, dataLen - INT_SIZE);
        offset += writeInt(buf, offset, values.length);
        for (HandleValue value : values) {
            offset += encodeHandleValue(buf, offset, value);
        }
        return buf;
    }

    /*****************************************************************************
     * Encode the given String[]/SiteInfo pairs into a buffer that can be used
     * as the local service/site information.  For the SiteInfo object at sites[i]
     * the corresponding prefixes should be listed in na[i].
     ****************************************************************************/
    public static final byte[] encodeLocalSites(SiteInfo sites[], String na[][]) throws HandleException {
        if (sites.length != na.length) {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Local site values must have matching NAs");
        }

        byte intbuf[] = new byte[4];
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
            b.write(intbuf); // placeholder for total size
            writeInt(intbuf, 0, sites.length);
            b.write(intbuf); // number of entries
            for (int i = 0; i < sites.length; i++) {
                writeInt(intbuf, 0, na[i].length);
                b.write(intbuf); // number of NAs for this site
                for (int j = 0; j < na[i].length; j++) {
                    writeInt(intbuf, 0, na[i][j].length());
                    b.write(intbuf);
                    b.write(Util.encodeString(na[i][j]));
                }
                byte bsite[] = encodeSiteInfoRecord(sites[i]);
                writeInt(intbuf, 0, bsite.length); // size of SiteInfo struct
                b.write(intbuf);
                b.write(bsite);
            }
            byte buf[] = b.toByteArray();
            writeInt(buf, 0, buf.length);
            return buf;
        } catch (IOException e) {
            e.printStackTrace();
            throw new HandleException(HandleException.INTERNAL_ERROR, "Error writing local site information");
        }
    }

    /*****************************************************************************
     * Reads SiteInfo/NA-list pairs into a hashtable.  Each NA value is used
     * as a key to a SiteInfo[].
     ****************************************************************************/
    public static final Map<String, SiteInfo[]> decodeLocalSites(InputStream in) throws HandleException {
        try {
            byte intbuf[] = new byte[INT_SIZE];
            Util.readFully(in, intbuf);
            //int dataLen = readInt(intbuf, 0);
            Util.readFully(in, intbuf);
            int numSites = readInt(intbuf, 0);
            String na[];
            SiteInfo site;
            SiteInfo sites[];
            HashMap<String, SiteInfo[]> table = new HashMap<>();
            for (int i = 0; i < numSites; i++) {
                Util.readFully(in, intbuf);
                int numNAs = readInt(intbuf, 0);
                na = new String[numNAs];
                for (int j = 0; j < numNAs; j++) {
                    Util.readFully(in, intbuf);
                    int len = readInt(intbuf, 0);
                    byte buf[] = new byte[len];
                    Util.readFully(in, buf);
                    na[j] = Util.decodeString(buf);
                }
                Util.readFully(in, intbuf);
                int len = readInt(intbuf, 0);
                byte b[] = new byte[len];
                Util.readFully(in, b);
                site = new SiteInfo();
                decodeSiteInfoRecord(b, 0, site);
                for (int j = 0; j < numNAs; j++) {
                    sites = table.get(na[j]);
                    if (sites == null) {
                        sites = new SiteInfo[1];
                    } else {
                        SiteInfo newsites[] = new SiteInfo[sites.length + 1];
                        System.arraycopy(sites, 0, newsites, 1, sites.length);
                        sites = newsites;
                    }
                    sites[0] = site;
                    table.put(na[j], sites);
                }
            }
            return table;
        } catch (IOException e) {
            e.printStackTrace();
            throw new HandleException(HandleException.INTERNAL_ERROR, "Error reading local site information");
        }
    }

    /*****************************************************************************
     * Reads pairs of IP addresses into a hashtable.
     ****************************************************************************/
    public static final Map<String, String> decodeLocalAddresses(InputStream in) throws HandleException {
        Map<String, String> map = new HashMap<>();
        try {
            BufferedReader rdr = new BufferedReader(new InputStreamReader(in, "ASCII"));
            while (true) {
                String line = rdr.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.length() <= 0) continue;
                String fromAddr = "";
                String toAddr = "";
                try (Scanner scanner = new Scanner(line)) {
                    if (scanner.hasNext()) fromAddr = scanner.next().trim().toLowerCase();
                    if (scanner.hasNext()) toAddr = scanner.next().trim().toLowerCase();
                }
                if (fromAddr.length() <= 0 || toAddr.length() <= 0) continue;

                map.put(fromAddr, toAddr);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new HandleException(HandleException.INTERNAL_ERROR, "Error reading local site information");
        }
        return map;
    }

    /*****************************************************************************
     * Writes pairs of IP addresses into a hashtable.
     ****************************************************************************/
    public static final void writeLocalAddresses(Map<?, ?> map, OutputStream out) throws IOException {
        OutputStreamWriter w = new OutputStreamWriter(out, "ASCII");
        for (Iterator<?> keys = map.keySet().iterator(); keys.hasNext(); ) {
            Object key = keys.next();
            Object val = map.get(key);
            if (key == null || val == null) continue;
            if (key instanceof java.net.InetAddress) key = Util.rfcIpRepr(((java.net.InetAddress) key));
            if (val instanceof java.net.InetAddress) val = Util.rfcIpRepr(((java.net.InetAddress) val));
            w.write(String.valueOf(key));
            w.write('\t');
            w.write(String.valueOf(val));
            w.write('\n');
        }
        w.close();
    }

    /*******************************************************************************
     * Decode from the given input stream a set of handle values.  The stream
     * should contain data in the format output from the encodeGlobalValues()
     * function.
     *******************************************************************************/
    public static final HandleValue[] decodeGlobalValues(InputStream in) throws HandleException {
        try {
            // read the global info into a buffer to be decoded
            byte buf[] = new byte[Encoder.INT_SIZE];
            Util.readFully(in, buf);
            int dataLen = readInt(buf, 0);
            buf = new byte[dataLen];
            int n = 0;
            int r;
            while ((r = in.read(buf, n, dataLen - n)) > 0 && n < dataLen) {
                n += r;
            }
            // decode the global info from the buffer
            int offset = 0;
            int numValues = Encoder.readInt(buf, offset);
            offset += INT_SIZE;
            if (numValues < 0) throw new Exception("Invalid number of handle values");

            HandleValue values[] = new HandleValue[numValues];
            for (int i = 0; i < numValues; i++) {
                values[i] = new HandleValue();
                offset += decodeHandleValue(buf, offset, values[i]);
            }

            return values;
        } catch (Exception e) {
            if (e instanceof HandleException) {
                throw (HandleException) e;
            }
            throw new HandleException(HandleException.INTERNAL_ERROR, "Error parsing global info", e);
        }
    }

    /*******************************************************************************
     * Decode from the given byte buffer, a set of handle values.
     *******************************************************************************/
    public static final HandleValue[] decodeHandleValues(byte[] buf) throws HandleException {
        try {
            // decode the HandleValue[] from the buffer
            int offset = 0;
            //int dataLen = Encoder.readInt(buf, offset);
            offset += Encoder.INT_SIZE;
            int numValues = Encoder.readInt(buf, offset);
            offset += INT_SIZE;
            if (numValues < 0) throw new Exception("Invalid number of handle values");

            HandleValue values[] = new HandleValue[numValues];
            for (int i = 0; i < numValues; i++) {
                values[i] = new HandleValue();
                offset += decodeHandleValue(buf, offset, values[i]);
            }

            return values;
        } catch (Exception e) {
            if (e instanceof HandleException) {
                throw (HandleException) e;
            }
            throw new HandleException(HandleException.INTERNAL_ERROR, "Error parsing global info: " + e);
        }
    }

    /*******************************************************************************
     * Decode, create, and return a SessionSetupRequest from the given buffer
     *******************************************************************************/
    static final SessionSetupRequest decodeSessionSetupRequest(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        try {

            SessionSetupRequest req = new SessionSetupRequest();
            req.keyExchangeMode = readInt2(msg, offset);
            offset += 2;
            req.timeout = readInt(msg, offset);
            offset += INT_SIZE;
            req.identityHandle = readByteArray(msg, offset);
            offset += INT_SIZE + req.identityHandle.length;
            req.identityIndex = readInt(msg, offset);
            offset += INT_SIZE;

            if (req.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_CLIENT || req.keyExchangeMode == Common.KEY_EXCHANGE_DH) {
                req.publicKey = readByteArray(msg, offset);
                offset += INT_SIZE + req.publicKey.length;
            } else if (req.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_HDL) {
                req.exchangeKeyHandle = readByteArray(msg, offset);
                offset += INT_SIZE + req.exchangeKeyHandle.length;
                req.exchangeKeyIndex = readInt(msg, offset);
                offset += INT_SIZE;
            }
            return req;
        } catch (Exception e) {
            if (e instanceof HandleException) {
                throw (HandleException) e;
            }
            throw new HandleException(HandleException.INTERNAL_ERROR, "Can not decode session setup request: " + e);
        }
    }

    /*******************************************************************************
     * Encode the given SessionSetupRequest for communication with the given site
     * and return the resulting buffer.
     *******************************************************************************/
    static final byte[] encodeSessionSetupRequest(SessionSetupRequest req) throws HandleException {
        int bodyLen = INT_SIZE; // mode
        bodyLen += INT_SIZE; // time out
        bodyLen += INT_SIZE; // identity handle size
        if (req.identityHandle != null) bodyLen += req.identityHandle.length;
        bodyLen += INT_SIZE; // identity index

        try {
            if (req.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_CLIENT || req.keyExchangeMode == Common.KEY_EXCHANGE_DH) {
                if (req.publicKey == null) {
                    throw new HandleException(HandleException.INTERNAL_ERROR, "Session setup request missing exchange key");
                }
                bodyLen += INT_SIZE; // key size
                bodyLen += req.publicKey.length; // client generated public key
            } else if (req.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_HDL) {
                if (req.exchangeKeyHandle == null) {
                    throw new HandleException(HandleException.INTERNAL_ERROR, "Session setup request missing key exchange handle");
                }
                bodyLen += INT_SIZE; // handle size
                bodyLen += req.exchangeKeyHandle.length; // handle with exchange key
                bodyLen += INT_SIZE; // handle index
            } else if (req.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_SERVER) {
                // nothing to add
            } else {
                throw new HandleException(HandleException.INTERNAL_ERROR, "Unknown key exchange mode: " + req.keyExchangeMode);
            }

            byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
            int offset = writeHeader(req, msg, bodyLen);

            offset += writeInt2(msg, offset, req.keyExchangeMode);
            offset += writeInt(msg, offset, req.timeout);
            offset += writeByteArray(msg, offset, req.identityHandle);
            offset += writeInt(msg, offset, req.identityIndex);

            if (req.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_CLIENT || req.keyExchangeMode == Common.KEY_EXCHANGE_DH) {
                offset += writeByteArray(msg, offset, req.publicKey);
            } else if (req.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_HDL) {
                offset += writeByteArray(msg, offset, req.exchangeKeyHandle);
                offset += writeInt(msg, offset, req.exchangeKeyIndex);
            }
            return msg;
        } catch (HandleException e) {
            throw e;
        } catch (Exception e) {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Error encoding session setup request: " + e.getMessage());
        }
    }

    /*******************************************************************************
     * Encode the given SessionExchangeKeyRequest and return the resulting buffer.
     *******************************************************************************/
    static final byte[] encodeSessionExchangeKeyRequest(SessionExchangeKeyRequest req) {
        //where the session setup response contains an encrypted session key
        byte[] encryptedSessionKey = req.getEncryptedSessionKey();
        int bodyLen = 0;
        if (encryptedSessionKey != null) {
            bodyLen = INT_SIZE + // space for the encrypted key length
                    encryptedSessionKey.length; // space for the encrypted key itself
        }
        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int loc = writeHeader(req, msg, bodyLen);
        if (encryptedSessionKey != null) {
            // write the encrypted session key
            loc += writeByteArray(msg, loc, encryptedSessionKey, 0, encryptedSessionKey.length);
        }
        return msg;
    }

    static SessionExchangeKeyRequest decodeSessionExchangeKeyRequest(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) {
        int sessionKeyLen = readInt(msg, offset);
        offset += INT_SIZE;

        byte encryptedSessionKey[] = new byte[sessionKeyLen];
        System.arraycopy(msg, offset, encryptedSessionKey, 0, sessionKeyLen);

        return new SessionExchangeKeyRequest(encryptedSessionKey);
    }

    /*******************************************************************************
     * Encode the given SessionSetupResponse and return the resulting buffer.
     *******************************************************************************/
    static final byte[] encodeSessionSetupResponse(SessionSetupResponse res) {
        int bodyLen = INT_SIZE; // mode
        bodyLen += INT_SIZE + res.data.length; // data

        bodyLen += res.returnRequestDigest ? 1 + res.requestDigest.length : 0;

        byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
        int offset = writeHeader(res, msg, bodyLen);

        //write the request digest if flag is set
        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }

        offset += writeInt2(msg, offset, res.keyExchangeMode);
        offset += writeByteArray(msg, offset, res.data, 0, res.data.length);

        return msg;
    }

    /*
    static final byte[] encodeSessionSetupExchangeKeyResponse(SessionSetupResponse res)
    throws HandleException
    {
    //where the session setup response contains an exchange key
    byte[] publicExchangeKey = res.data;
    int bodyLen = INT_SIZE;     // space for the encrypted key length, 0 for pub exng key
    if (publicExchangeKey != null)
     bodyLen +=
      publicExchangeKey.length;           // space for the encrypted key itself
    
    bodyLen += res.returnRequestDigest ? 1 + res.requestDigest.length : 0;
    
    byte msg[] = new byte[bodyLen + Common.MESSAGE_HEADER_SIZE];
    int loc = writeHeader(res, msg, bodyLen);
    
    //write the request digest if flag is set
    if(res.returnRequestDigest) {
      msg[loc++] = res.rdHashType;
      System.arraycopy(res.requestDigest, 0, msg, loc, res.requestDigest.length);
      loc += res.requestDigest.length;
    }
    
    if (publicExchangeKey != null) {
      // write the encrypted session key
      loc += writeByteArray(msg, loc, publicExchangeKey, 0,
                                      publicExchangeKey.length);
    } else {
      loc += writeInt(msg, loc, 0);
    }
    return msg;
    }
     */

    static SessionSetupResponse decodeSetupSessionResponse(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env) throws HandleException {
        int mode = readInt2(msg, offset);
        offset += 2;

        byte data[] = readByteArray(msg, offset);
        offset += INT_SIZE + data.length;

        return new SessionSetupResponse(mode, data);
    }

    static AbstractResponse decodeCreateHandleResponse(byte msg[], int offset, @SuppressWarnings("unused") MessageEnvelope env, int bodyLength) throws HandleException {
        if (bodyLength == 0) {
            return new CreateHandleResponse(null);
        }

        int handleLen = readInt(msg, offset);
        offset += INT_SIZE;

        if (handleLen < 0 || handleLen > Common.MAX_HANDLE_LENGTH) {
            throw new HandleException(HandleException.MESSAGE_FORMAT_ERROR, "Invalid handle length: " + handleLen);
        }

        byte[] handle = new byte[handleLen];
        System.arraycopy(msg, offset, handle, 0, handleLen);
        offset += handleLen;

        return new CreateHandleResponse(handle);
    }

    public static final byte[] encodeCreateHandleResponse(CreateHandleResponse res) {
        int bodyLen = res.returnRequestDigest ? res.requestDigest.length + 1 : 0;
        if (res.handle != null) bodyLen += INT_SIZE + res.handle.length;
        byte msg[] = new byte[Common.MESSAGE_HEADER_SIZE + bodyLen];
        int offset = writeHeader(res, msg, bodyLen);
        if (res.returnRequestDigest) {
            msg[offset++] = res.rdHashType;
            System.arraycopy(res.requestDigest, 0, msg, offset, res.requestDigest.length);
            offset += res.requestDigest.length;
        }
        if (res.handle != null) {
            // write the handle
            offset += writeByteArray(msg, offset, res.handle, 0, res.handle.length);
        }
        return msg;
    }

}
