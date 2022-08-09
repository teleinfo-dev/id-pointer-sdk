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
import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;

/**************************************************************************
 * Objects of this class can be used to sign the contents of a stream
 * that can be verified by a SignedInputStream class.
 *
 * Note: This is not a part of the official handle protocol specification.
 * This was introduced by CNRI solely for replication in the Handle.net software.
 **************************************************************************/
public class SignedOutputStream extends FilterOutputStream {
    private Signature sig;
    private int streamType;

    /**************************************************************************
     * Create a stream that can verify the data read from the stream in
     * blocks.  The caller should call the verifyBlock method at the end
     * of every block of data that needs to be verified.  verifyBlock must
     * be called at the same position in the stream that signBlock was called
     * in the parallel SignedOutputStream object that generated the stream.
     **************************************************************************/
    public SignedOutputStream(PrivateKey sourceKey, OutputStream out) throws HandleException, IOException {
        super(out);
        initializeSigned(sourceKey);
    }

    private void initializeSigned(PrivateKey sourceKey) throws HandleException, IOException {
        // write the stream type identifier
        setAndWriteStreamType(SignedInputStream.STREAM_TYPE_PK);

        // write the header info specific to this stream type...

        // ...write the signature hash algorithm type
        byte hashID[] = Common.HASH_ALG_SHA1;
        byte hashIDLen[] = new byte[Encoder.INT_SIZE];
        Encoder.writeInt(hashIDLen, 0, hashID.length);
        out.write(hashIDLen);
        out.write(hashID);

        // initialize the signature
        try {
            sig = Signature.getInstance(Util.getSigIdFromHashAlgId(hashID, sourceKey.getAlgorithm()));
            sig.initSign(sourceKey);
        } catch (InvalidKeyException e) {
            throw new HandleException(HandleException.SECURITY_ALERT, "Error initializing SignedOutputStream", e);
        } catch (NoSuchAlgorithmException e) {
            throw new HandleException(HandleException.SECURITY_ALERT, "Error initializing SignedOutputStream", e);
        }
    }

    private void setAndWriteStreamType(int streamType) throws IOException {
        this.streamType = streamType;
        byte streamTypeBuf[] = new byte[Encoder.INT_SIZE];
        Encoder.writeInt(streamTypeBuf, 0, streamType);
        out.write(streamTypeBuf);
    }

    public SignedOutputStream(OutputStream out) throws IOException {
        super(out);
        setAndWriteStreamType(SignedInputStream.STREAM_TYPE_UNSIGNED);
    }

    public SignedOutputStream(X509Certificate certificate, PrivateKey sourceKey, OutputStream out, Socket socket) throws HandleException, IOException {
        super(out);
        try {
            SSLContext sslContext = SSLEngineHelper.getServerSSLContext(certificate, sourceKey);
            @SuppressWarnings("resource")
            SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, socket.getRemoteSocketAddress().toString(), socket.getPort(), true);
            sslSocket.setEnabledCipherSuites(SSLEngineHelper.ENABLED_CIPHER_SUITES);
            sslSocket.setEnabledProtocols(SSLEngineHelper.ENABLED_SERVER_PROTOCOLS);
            sslSocket.setUseClientMode(false);
            setAndWriteStreamType(SignedInputStream.STREAM_TYPE_TLS);
            out.flush(); // important to flush the non-TLS stream type before requiring the TLS handshake
            this.out = new BufferedOutputStream(sslSocket.getOutputStream());
        } catch (KeyManagementException e) {
            // fall back
            initializeSigned(sourceKey);
        }
    }

    /**************************************************************************
     * Write a byte to the stream.
     **************************************************************************/
    @Override
    public void write(int b) throws IOException {
        if (streamType == SignedInputStream.STREAM_TYPE_PK) {
            try {
                sig.update((byte) b);
            } catch (SignatureException e) {
                throw new IOException("Error updating signature", e);
            }
        }
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (streamType == SignedInputStream.STREAM_TYPE_PK) {
            try {
                sig.update(b);
            } catch (SignatureException e) {
                throw new IOException("Error updating signature", e);
            }
        }
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (streamType == SignedInputStream.STREAM_TYPE_PK) {
            try {
                sig.update(b, off, len);
            } catch (SignatureException e) {
                throw new IOException("Error updating signature", e);
            }
        }
        out.write(b, off, len);
    }

    /**************************************************************************
     * Signs the bytes written since the last signature on the stream.  This
     * should be called at the exact same point in the stream as the
     * verifyBlock method in the SignedInputStream class.
     **************************************************************************/
    public void signBlock() throws IOException, SignatureException {
        if (streamType != SignedInputStream.STREAM_TYPE_PK) return;
        byte lenbuf[] = new byte[4];
        byte sigBytes[] = sig.sign();
        Encoder.writeInt(lenbuf, 0, sigBytes.length);
        out.write(lenbuf);
        out.write(sigBytes);
    }

}
