/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for all request types.  Holds the Handle to which the request
 * applies as well as the HS_NAMESPACE information that was acquired
 * during the resolution process
 */
public abstract class AbstractRequest extends AbstractMessage {
    public byte handle[];
    public boolean isAdminRequest = false;
    public boolean requiresConnection = false;

    // For use with "Happy Eyeballs."
    public boolean multithread = false;
    public ReentrantLock connectionLock = new ReentrantLock();
    public AtomicBoolean completed = new AtomicBoolean();
    public AtomicReference<Socket> socketRef = new AtomicReference<>();

    public AuthenticationInfo authInfo = null;

    // if there is a session object associated with this
    // request, sessionInfo will be set and used to begin
    // (or continue) a session with the destination server.
    // If this is set, the ClientSessionTracker setting is
    // ignored.
    public ClientSideSessionInfo sessionInfo = null;

    // This is used to keep track of sessions so that
    // if a session has been created with whatever server
    // this is sent to, that same session can be used to
    // send this message.  If a session is found, then
    // it is used to send the message.  If a session is
    // not found and sessionInfo is not null, a new session
    // is created and used to send this message.
    // If sessionInfo is non-null and authInfo is null
    // then an anonymous session is used to send this message.
    public ClientSessionTracker sessionTracker = null;

    // these are for use by the resolver for verifying the
    // signatues of responses.
    byte serverPubKeyBytes[] = null;

    // Indicates whether or not the response to this request
    // is "streamable."  If so, the request/response can only
    // be sent via a TCP or other stream-oriented transport.
    // This 'streaming' flag is not a part of the official handle
    // protocol or API specification.
    public boolean streaming = false;

    // The most specific namespace containing this handle.  Parent namespaces
    // are accessible by calling the getParentNamespace() method on this namespace.
    private NamespaceInfo namespace = null;

    public AbstractRequest(byte handle[], int opCode, AuthenticationInfo authInfo) {
        super(opCode);
        this.authInfo = authInfo;
        this.handle = handle;
        this.responseCode = AbstractMessage.RC_RESERVED;
    }

    @Override
    public AbstractRequest clone() {
        AbstractRequest req = (AbstractRequest) super.clone();
        req.socketRef = new AtomicReference<>(this.socketRef.get());
        return req;
    }

    /** Returns the information for the most specific namespace that was
     * encountered when performing this resolution.  Higher level namespaces
     * can be accessed using the getParentNamespace() method of NamespaceInfo. */
    public NamespaceInfo getNamespace() {
        return this.namespace;
    }

    /** Set the most specific namespace containing the identifier being
     * resolved.  This will set the parent of the given namespace to the
     * current namespace. */
    public void setNamespace(NamespaceInfo namespace) {
        if (namespace != null) namespace.setParentNamespace(this.namespace);
        this.namespace = namespace;
    }

    /** Set the most exact namespace containing the identifier being
     * resolved.  The current namespace is discarded and the entire hierarchy
     * of the given namespace is retained. */
    public void setNamespaceExactly(NamespaceInfo namespace) {
        this.namespace = namespace;
    }

    /** Override the clearing of buffers to also clear the namespace information */
    @Override
    public void clearBuffers() {
        this.namespace = null;
        this.multithread = false;
        this.completed.set(false);
        this.socketRef.set(null);
        super.clearBuffers();
    }

    @Override
    public String toString() {
        return super.toString() + (isAdminRequest ? " adm" : "") + ' ' + Util.decodeString(handle);
    }

    public void signMessageForSession() throws HandleException {
        try {
            this.sessionCounter = this.sessionInfo.getNextSessionCounter();
            this.signMessage(this.sessionInfo.getSessionKey());
        } catch (Exception e) {
            if (e instanceof HandleException) throw (HandleException) e;
            throw new HandleException(HandleException.UNABLE_TO_SIGN_REQUEST, "Unable to sign original request with session key: ", e);
        }
    }

    /** Used in Happy Eyeballs where several requests might be used but only one actually ends up resolving */
    void takeValuesFromRequestActuallyUsed(AbstractRequest req) {
        this.siteInfoSerial = req.siteInfoSerial;
        this.sessionInfo = req.sessionInfo;
        this.requestId = req.requestId;
        this.sessionId = req.sessionId;
        this.certify = req.certify;
        this.encrypt = req.encrypt;
        this.sessionCounter = req.sessionCounter;
        this.serverPubKeyBytes = req.serverPubKeyBytes;
        this.majorProtocolVersion = req.majorProtocolVersion;
        this.minorProtocolVersion = req.minorProtocolVersion;
        this.suggestMajorProtocolVersion = req.suggestMajorProtocolVersion;
        this.suggestMinorProtocolVersion = req.suggestMinorProtocolVersion;
        this.rdHashType = req.rdHashType;
        this.requestDigest = req.requestDigest;
        this.namespace = req.namespace;
    }
}
