/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/**************************************************************************
 * Objects of this class can be used to verify the contents of a stream
 * generated by a SignedOutputStream class.
 *
 * Note: This is not a part of the official handle protocol specification.
 * This was introduced by CNRI solely for replication in the Handle.net software.
 **************************************************************************/
public class SignedInputStream extends FilterInputStream {
    public static final int STREAM_TYPE_PK = 0;
    public static final int STREAM_TYPE_UNSIGNED = -1;
    public static final int STREAM_TYPE_TLS = 0x544C5300;

    private Signature sig;
    private final int streamType;

    /**************************************************************************
     * Create a stream that can verify the data read from the stream in
     * blocks.  The caller should call the verifyBlock method at the end
     * of every block of data that needs to be verified.  verifyBlock must
     * be called at the same position in the stream that signBlock was called
     * in the parallel SignedOutputStream object that generated the stream.
     **************************************************************************/
    public SignedInputStream(PublicKey sourceKey, InputStream in, Socket socket) throws Exception {
        super(in);

        // read the type-of-stream identifier - the first 4 bytes
        byte streamTypeBuf[] = new byte[Encoder.INT_SIZE];
        Util.readFully(in, streamTypeBuf);
        streamType = Encoder.readInt(streamTypeBuf, 0);

        // initialize ourself depending on the type of stream
        switch (streamType) {
        case STREAM_TYPE_UNSIGNED:
            break;
        case STREAM_TYPE_PK:
            // read the hash ID so we know how to verify the signatures...
            byte lenbuf[] = new byte[Encoder.INT_SIZE];
            Util.readFully(in, lenbuf);

            int hashIDLen = Encoder.readInt(lenbuf, 0);
            if (hashIDLen < 0 || hashIDLen > Common.MAX_ARRAY_SIZE) throw new SignatureException("Invalid hash ID - too long");

            byte hashID[] = new byte[hashIDLen];
            Util.readFully(in, hashID);

            sig = Signature.getInstance(Util.getSigIdFromHashAlgId(hashID, sourceKey.getAlgorithm()));
            sig.initVerify(sourceKey);

            break;
        case STREAM_TYPE_TLS:
            if (socket == null) throw new HandleException(HandleException.SECURITY_ALERT, "TLS stream not available");
            SSLContext sslContext = SSLEngineHelper.getClientSSLContext(sourceKey);
            SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, socket.getRemoteSocketAddress().toString(), socket.getPort(), true);
            sslSocket.setEnabledCipherSuites(SSLEngineHelper.ENABLED_CIPHER_SUITES);
            sslSocket.setEnabledProtocols(SSLEngineHelper.ENABLED_CLIENT_PROTOCOLS);
            sslSocket.setUseClientMode(true);
            this.in = new BufferedInputStream(sslSocket.getInputStream());
            break;
        default:
            throw new SignatureException("Unrecognized stream type: " + streamType);
        }

    }

    public boolean isSecure() {
        return streamType != STREAM_TYPE_UNSIGNED;
    }

    /**************************************************************************
     * Read a byte from the stream.
     **************************************************************************/
    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b < 0) return b;
        if (streamType == STREAM_TYPE_PK) {
            try {
                sig.update((byte) b);
            } catch (SignatureException e) {
                throw new IOException("Error updating signature", e);
            }
        }
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int r = in.read(b);
        if (r <= 0) return r;
        if (streamType == STREAM_TYPE_PK) {
            try {
                sig.update(b, 0, r);
            } catch (SignatureException e) {
                throw new IOException("Error updating signature", e);
            }
        }
        return r;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int r = in.read(b, off, len);
        if (r <= 0) return r;
        if (streamType == STREAM_TYPE_PK) {
            try {
                sig.update(b, off, r);
            } catch (SignatureException e) {
                throw new IOException("Error updating signature", e);
            }
        }
        return r;
    }

    /**************************************************************************
     * Reads a signature from the stream and verifies the bytes read since
     * the last verification based on that signature.  This should be called
     * at the exact same point in the stream as the signBlock() in the
     * SignedOutpuStream class.
     **************************************************************************/
    public boolean verifyBlock() throws IOException, SignatureException {
        if (streamType != STREAM_TYPE_PK) return true;
        byte lenbuf[] = new byte[Encoder.INT_SIZE];
        int r;
        int n = 0;
        while (n < lenbuf.length && (r = in.read(lenbuf, n, lenbuf.length - n)) > 0) {
            n += r;
        }
        if (n < Encoder.INT_SIZE) throw new SignatureException("End of stream while reading signature length");

        int sigLen = Encoder.readInt(lenbuf, 0);
        if (sigLen < 0 || sigLen > Common.MAX_ARRAY_SIZE) throw new SignatureException("Invalid signature - too long");

        byte buf[] = new byte[sigLen];
        n = 0;
        while (n < sigLen && (r = in.read(buf, n, sigLen - n)) > 0) {
            n += r;
        }
        if (n < sigLen) throw new SignatureException("End of stream while reading signature." + "  Expected " + sigLen + ",  got " + n);

        return sig.verify(buf);
    }

}
