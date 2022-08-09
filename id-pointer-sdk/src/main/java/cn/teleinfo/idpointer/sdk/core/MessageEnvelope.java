/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public class MessageEnvelope {
    public byte protocolMajorVersion = Common.COMPATIBILITY_MAJOR_VERSION;
    public byte protocolMinorVersion = Common.COMPATIBILITY_MINOR_VERSION;

    public byte suggestMajorProtocolVersion = Common.MAJOR_VERSION;
    public byte suggestMinorProtocolVersion = Common.MINOR_VERSION;

    public int sessionId = 0; // the ID of the multi-connection session
    public int requestId; // the ID that identifies the request/response pair
    public int messageId = 0; // the message ID (essentially the packet# where applicable);
    public int messageLength;

    public boolean truncated = false;
    public boolean encrypted = false;
    public boolean compressed = false;

    @Override
    public String toString() {
        return "protocol=" + protocolMajorVersion + '.' + protocolMinorVersion + "; session=" + sessionId + "; req=" + Integer.toHexString(requestId) + "h; msg=" + messageId + "; len=" + messageLength;
    }
}
