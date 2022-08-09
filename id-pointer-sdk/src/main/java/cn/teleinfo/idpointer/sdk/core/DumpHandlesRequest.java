/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/***************************************************************************
 * Request used to retrieve all handles from a server.  This
 * request is used for server&lt;-&gt;server (or replicator&lt;-&gt;server)
 * communication.
 ***************************************************************************/

public class DumpHandlesRequest extends AbstractRequest {

    public static final int HANDLE_REPLICATION_DB = 0;
    public static final int NA_REPLICATION_DB = 1;
    public static final int HANDLE = 2;
    public static final int NA = 3;

    // to specify which handles to send (filtered by how the handles are hashed)
    public int serverNum;
    public byte rcvrHashType;
    public int numServers;
    public byte[] startingPoint = null; //Optional handle used to resume the dump from a particular point.
    public int startingPointType;

    public DumpHandlesRequest(byte rcvrHashType, int numServers, int serverNum, AuthenticationInfo authInfo) {
        super(Common.BLANK_HANDLE, OC_DUMP_HANDLES, authInfo);
        this.rcvrHashType = rcvrHashType;
        this.numServers = numServers;
        this.serverNum = serverNum;
        this.certify = true;
        this.isAdminRequest = true;
        this.streaming = true;
    }

    public DumpHandlesRequest(byte rcvrHashType, int numServers, int serverNum, AuthenticationInfo authInfo, byte[] startingPoint, int startingPointType) {
        this(rcvrHashType, numServers, serverNum, authInfo);
        this.startingPoint = startingPoint;
        this.startingPointType = startingPointType;
    }
}
