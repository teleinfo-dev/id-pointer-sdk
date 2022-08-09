/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/***************************************************************************
 * Request used to retrieve any new transactions from a server.  This
 * request is used for server&lt;-&gt;server (or replicator&lt;-&gt;server)
 * communication.
 ***************************************************************************/

public class RetrieveTxnRequest extends AbstractRequest {

    // The following field will allow the server being queried
    // to determine which handles need to be sent to the
    // requestor.  The queried server will send every handle that
    // has a transaction ID greater than the given lastTxnId and
    // hashes to the requesting server.
    public long lastTxnId; // the last transaction ID that was queried

    // The following field will allow the server being queried
    // to determine if the entire set of handles needs to be
    // "dumped" again.
    public long lastQueryDate; // The last time transactions were queried

    // A RetrieveTxnRequest EITHER has lastTxnId and lastQueryDate, OR it has replicationStateInfo
    public ReplicationStateInfo replicationStateInfo = null;

    // request always has following fields
    // to specify which server in the site this is...
    public int serverNum;
    public byte rcvrHashType;
    public int numServers;

    public RetrieveTxnRequest(long lastTxnId, long lastQueryDate, byte rcvrHashType, int numServers, int serverNum, AuthenticationInfo authInfo) {
        super(Common.BLANK_HANDLE, OC_RETRIEVE_TXN_LOG, authInfo);
        this.lastTxnId = lastTxnId;
        this.lastQueryDate = lastQueryDate;
        this.rcvrHashType = rcvrHashType;
        this.numServers = numServers;
        this.serverNum = serverNum;
        this.certify = true;
        this.isAdminRequest = true;
        this.streaming = true;
    }

    public RetrieveTxnRequest(ReplicationStateInfo replicationStateInfo, byte rcvrHashType, int numServers, int serverNum, AuthenticationInfo authInfo) {
        super(Common.BLANK_HANDLE, OC_RETRIEVE_TXN_LOG, authInfo);
        this.replicationStateInfo = replicationStateInfo;
        this.rcvrHashType = rcvrHashType;
        this.numServers = numServers;
        this.serverNum = serverNum;
        this.certify = true;
        this.isAdminRequest = true;
        this.streaming = true;
    }

    @Override
    public String toString() {
        return super.toString() + " [retrieve-txns lasttxn=" + lastTxnId + " lastdate=" + (new java.util.Date(lastQueryDate)) + " svr=" + serverNum + "/" + numServers + " hash=" + rcvrHashType;
    }

}
