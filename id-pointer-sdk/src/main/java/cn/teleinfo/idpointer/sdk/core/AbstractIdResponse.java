/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import cn.teleinfo.idpointer.sdk.client.v3.IdResponse;

import java.io.InputStream;
import java.net.Socket;

public abstract class AbstractIdResponse extends AbstractMessage implements IdResponse {

    // holder for input stream, so that client app can just
    // call processStreamedPart() to process the stream
    // if this is a "streaming" response.
    public InputStream stream = null;
    public Socket socket = null;
    public boolean secureStream = false; // streamed over TLS, noted by client
    public boolean streaming = false;

    public AbstractIdResponse() {
        super();
    }

    public AbstractIdResponse(int opCode, int responseCode) {
        super(opCode);
        this.responseCode = responseCode;
    }

    public AbstractIdResponse(AbstractIdRequest req, int responseCode) throws HandleException {
        super(req.opCode);
        this.requestId = req.requestId;
        this.responseCode = responseCode;

        //session id passed to response here
        this.sessionId = req.sessionId;

        takeValuesFrom(req);

        this.majorProtocolVersion = req.suggestMajorProtocolVersion;
        this.minorProtocolVersion = req.suggestMinorProtocolVersion;
        this.suggestMajorProtocolVersion = Common.MAJOR_VERSION;
        this.suggestMinorProtocolVersion = Common.MINOR_VERSION;
        setSupportedProtocolVersion();

        if (this.returnRequestDigest) {
            // if this is a response to the given message, attach a digest of
            // the request to this message.
            takeDigestOfRequest(req);
        }
    }



    /** If this message is to-be-continued, this method is called to get
      subsequent messages until it returns null which will indicate that
      the current message is the last. */
    public AbstractIdResponse getContinuedResponse() {
        return null;
    }

    public final void takeDigestOfRequest(AbstractMessage req) throws HandleException {
        // create a digest of the original message
        if (!this.hasEqualOrGreaterVersion(2, 1)) {
            requestDigest = Util.doMD5Digest(req.getEncodedMessageBody());
            rdHashType = Common.HASH_CODE_MD5_OLD_FORMAT;
        } else if (this.hasEqualOrGreaterVersion(2, 7)) {
            requestDigest = Util.doSHA256Digest(req.getEncodedMessageBody());
            rdHashType = Common.HASH_CODE_SHA256;
        } else {
            requestDigest = Util.doSHA1Digest(req.getEncodedMessageBody());
            rdHashType = Common.HASH_CODE_SHA1;
        }
    }

    /*************************************************************
     * Write the response to the specified output stream.
     * By default this does nothing.  This should be over-ridden
     * by responses that set <I>streaming</i> to true.
     *************************************************************/
    @SuppressWarnings("unused")
    public void streamResponse(SignedOutputStream out) throws HandleException {
    }
}
