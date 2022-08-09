/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.util.Vector;

/** Instances of ClientSessionTracker are meant to maintain a set of
 *  ClientSideSessionInfo objects for different servers.  When one
 *  of the servers in the list are contacted, the existing session
 *  information associated with that server is used to authenticate.
 *
 *  This object also keeps track of the SessionSetupInfo in case
 *  the resolver needs to create a new session.
 */

public class ClientSessionTracker {
    private static final AuthenticationInfo ANONYMOUS_PLACEHOLDER = new SecretKeyAuthenticationInfo(Common.BLANK_HANDLE, -1, null);
    //  TODO: use maps!
    private final Vector<ServerInfo> serverList = new Vector<>();
    private final Vector<ClientSideSessionInfo> sessionList = new Vector<>();
    private final Vector<AuthenticationInfo> authList = new Vector<>();

    private SessionSetupInfo sessionSetupInfo = null;

    /** Creates a new ClientSessionTracker object that has no sessionSetupInfo.
      With no sessionSetupInfo, requests that use this session tracker will
      not cause new sessions to be initiated, but existing sessions will be
      used.
     */
    public ClientSessionTracker() {
    }

    public ClientSessionTracker(SessionSetupInfo setupInfo) {
        this.sessionSetupInfo = setupInfo;
    }

    /** Sets the session setup information for this session tracker.  This
      makes it so that requests that use this session tracker will cause
      new sessions to be created and stored here.
     */
    public void setSessionSetupInfo(SessionSetupInfo setupInfo) {
        this.sessionSetupInfo = setupInfo;
    }

    /** Gets the session setup information for this session tracker. */
    public SessionSetupInfo getSessionSetupInfo() {
        return this.sessionSetupInfo;
    }

    /** Gets the session that is associated with the given server
      and authentication information.  If there is no session with
      the given server and authentication information then this
      returns null.  If the given authInfo object is null then the
      session is assumed to be anonymous. */
    public synchronized ClientSideSessionInfo getSession(ServerInfo server, AuthenticationInfo authInfo) {
        ClientSideSessionInfo session = null;
        for (int i = authList.size() - 1; i >= 0; i--) {
            if (server.equals(serverList.elementAt(i))) {
                if ((authInfo == null && authList.elementAt(i) == ANONYMOUS_PLACEHOLDER) || (authInfo != null && authList.elementAt(i).equals(authInfo))) {
                    session = sessionList.elementAt(i);
                }
            }

            if (session != null && session.hasExpired()) {
                // the session has expired... let's get rid of it altogether
                for (int j = authList.size() - 1; j >= 0; j--) {
                    if (session.equals(sessionList.elementAt(j))) {
                        sessionList.removeElementAt(j);
                        authList.removeElementAt(j);
                        serverList.removeElementAt(j);
                    }
                }
                session = null;
            } else if (session != null) {
                return session;
            }
        }
        return null;
    }

    /** Gets the session that is associated with the given server
    and authentication information, and removes it so that no other thread
    can use it.  If there is no session with
    the given server and authentication information then this
    returns null.  If the given authInfo object is null then the
    session is assumed to be anonymous. */
    public synchronized ClientSideSessionInfo getAndRemoveSession(ServerInfo server, AuthenticationInfo authInfo) {
        ClientSideSessionInfo session = null;
        for (int i = authList.size() - 1; i >= 0; i--) {
            if (server.equals(serverList.elementAt(i))) {
                if ((authInfo == null && authList.elementAt(i) == ANONYMOUS_PLACEHOLDER) || (authInfo != null && authList.elementAt(i).equals(authInfo))) {
                    session = sessionList.elementAt(i);
                }
            }
            if (session != null) {
                for (int j = authList.size() - 1; j >= 0; j--) {
                    if (session.equals(sessionList.elementAt(j))) {
                        sessionList.removeElementAt(j);
                        authList.removeElementAt(j);
                        serverList.removeElementAt(j);
                    }
                }
                if (session.hasExpired()) session = null;
                else return session;
            }
        }
        return null;
    }

    /** Gets the authentication object that goes with the specified session.
      This should only be used by the HandleResolver to close out a session,
      because in all other cases, the resolver can use the authentication
      info that came with an associated request. */
    synchronized AuthenticationInfo getAuthenticationInfo(ClientSideSessionInfo sessionInfo) {
        if (sessionInfo == null) return null;
        for (int i = sessionList.size() - 1; i >= 0; i--) {
            if (sessionList.elementAt(i).equals(sessionInfo)) return authList.elementAt(i);
        }
        return null;
    }

    /** Returns a list of sessions. */
    public synchronized ClientSideSessionInfo[] getAllSessions() {
        Vector<ClientSideSessionInfo> tempSessions = new Vector<>();
        for (int i = sessionList.size() - 1; i >= 0; i--) {
            ClientSideSessionInfo obj = sessionList.elementAt(i);
            if (!tempSessions.contains(obj)) tempSessions.addElement(obj);
        }
        ClientSideSessionInfo sessions[] = new ClientSideSessionInfo[tempSessions.size()];
        tempSessions.toArray(sessions);
        return sessions;
    }

    /** Stores the given session object and associates it with the given server
      and authInfo objects.  If the authInfo object is null then the session is
      assumed to be anonymous.
     */
    public synchronized void putSession(ClientSideSessionInfo session, ServerInfo server, AuthenticationInfo authInfo) {
        serverList.addElement(server);
        sessionList.addElement(session);
        if (authInfo == null) {
            authList.addElement(ANONYMOUS_PLACEHOLDER);
        } else {
            authList.addElement(authInfo);
        }
    }

    /**
     * Remove all references to the given session from this session tracker.
     **/
    public synchronized void removeSession(ClientSideSessionInfo session) {
        for (int j = authList.size() - 1; j >= 0; j--) {
            if (session.equals(sessionList.elementAt(j))) {
                sessionList.removeElementAt(j);
                authList.removeElementAt(j);
                serverList.removeElementAt(j);
            }
        }
    }

    public static boolean sessionOptionChanged(ClientSideSessionInfo csinfo, SessionSetupInfo option) {

        if (csinfo == null || option == null) return false;

        return (!Util.equals(csinfo.getExchangeKeyRefHandle(), option.exchangeKeyHandle) || csinfo.getExchangeKeyRefindex() != option.exchangeKeyIndex || !Util.equals(csinfo.getExchagePublicKey(), option.publicExchangeKey)
            || csinfo.getTimeOut() != option.timeout || csinfo.encryptMessage != option.encrypted || csinfo.authenticateMessage != option.authenticated);

    }

}
