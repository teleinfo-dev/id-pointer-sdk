/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.core.stream.StreamTable;

/************************************************************************
 * Interface used to define objects that want to receive the streamed
 * results from DumpHandlesResponse messages.
 ************************************************************************/
public interface DumpHandlesCallback {

    /********************************************************************
     * Process the given transaction which was received via the stream
     * in the DumpHandlesResponse message.
     ********************************************************************/
    public void addHandle(byte handle[], HandleValue values[]) throws Exception;

    /********************************************************************
     * Process the given prefix which was received via the
     * stream in the DumpHandlesResponse message.  If this message is
     * called, that means that the server is responsible for this prefix.
     ********************************************************************/
    public void addHomedPrefix(byte naHandle[]) throws Exception;

    public void processThisServerReplicationInfo(long retrievalDate, long currentTxnId);

    public void processOtherSiteReplicationInfo(StreamTable replicationConfig) throws HandleException;

    public void setLastCreateOrDeleteDate(byte[] handle, long date, int priority) throws Exception;

    public void setLastHomeOrUnhomeDate(byte[] handle, long date, int priority) throws Exception;
}
