/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public class Interface {
    // types of services
    public static final byte ST_OUT_OF_SERVICE = 0;
    public static final byte ST_ADMIN = 1;
    public static final byte ST_QUERY = 2;
    public static final byte ST_ADMIN_AND_QUERY = 3;

    public static final byte SP_HDL_UDP = 0;
    public static final byte SP_HDL_TCP = 1;
    public static final byte SP_HDL_HTTP = 2;
    public static final byte SP_HDL_HTTPS = 3;

    public byte type; // OUT_OF_SERVICE, ADMIN, QUERY, ADMIN_AND_QUERY
    public int port; // usually 2641
    public byte protocol; // UDP, TCP, HTTP

    public Interface(byte type, byte protocol, int port) {
        this.type = type;
        this.port = port;
        this.protocol = protocol;
    }

    public Interface cloneInterface() {
        Interface i2 = new Interface();
        i2.type = type;
        i2.port = port;
        i2.protocol = protocol;
        return i2;
    }

    public Interface() {
    }

    /** Return true if this interface will respond to request */
    public boolean canHandleRequest(AbstractIdRequest req) {
        if ((req.streaming || req.requiresConnection) && protocol != SP_HDL_TCP && protocol != SP_HDL_HTTP && protocol != SP_HDL_HTTPS) {
            return false;
        }
        if (req.isAdminRequest) {
            return (type == ST_ADMIN || type == ST_ADMIN_AND_QUERY);
        } else {
            return (type == ST_QUERY || type == ST_ADMIN_AND_QUERY);
        }
    }

    @Override
    public String toString() {
        return typeName(type) + '/' + protocolName(protocol) + '/' + port;
    }

    public static final String typeName(byte type) {
        switch (type) {
        case ST_OUT_OF_SERVICE:
            return "out-of-service";
        case ST_ADMIN_AND_QUERY:
            return "adm+qry";
        case ST_QUERY:
            return "qry";
        case ST_ADMIN:
            return "adm";
        default:
            return "UNKNOWN";
        }
    }

    public static final String protocolName(byte protocol) {
        switch (protocol) {
        case SP_HDL_HTTPS:
            return "HTTPS";
        case SP_HDL_HTTP:
            return "HTTP";
        case SP_HDL_TCP:
            return "TCP";
        case SP_HDL_UDP:
            return "UDP";
        default:
            return "UNKNOWN";
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + port;
        result = prime * result + protocol;
        result = prime * result + type;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Interface other = (Interface) obj;
        if (port != other.port) return false;
        if (protocol != other.protocol) return false;
        if (type != other.type) return false;
        return true;
    }

    public static String canProcessMsg(AbstractIdRequest req, boolean processQueries, boolean processAdminRequests) {
        switch (req.opCode) {
        // the following are always considered admin requests
        case AbstractMessage.OC_GET_NEXT_TXN_ID:
        case AbstractMessage.OC_HOME_NA:
        case AbstractMessage.OC_UNHOME_NA:
        case AbstractMessage.OC_LIST_HOMED_NAS:
        case AbstractMessage.OC_BACKUP_SERVER:
        case AbstractMessage.OC_CREATE_HANDLE:
        case AbstractMessage.OC_DELETE_HANDLE:
        case AbstractMessage.OC_ADD_VALUE:
        case AbstractMessage.OC_REMOVE_VALUE:
        case AbstractMessage.OC_MODIFY_VALUE:
        case AbstractMessage.OC_LIST_HANDLES:
        case AbstractMessage.OC_RETRIEVE_TXN_LOG:
        case AbstractMessage.OC_DUMP_HANDLES:
            return processAdminRequests ? null : "Received admin request on non-admin interface";

        // these are always considered queries
        case AbstractMessage.OC_RESOLUTION:
            return processQueries ? null : "Received query request on non-query interface";

        // the following could be considered either, so we'll accept them no matter what
        case AbstractMessage.OC_RESPONSE_TO_CHALLENGE:
        case AbstractMessage.OC_GET_SITE_INFO:
        case AbstractMessage.OC_SESSION_SETUP:
        case AbstractMessage.OC_SESSION_TERMINATE:
        case AbstractMessage.OC_SESSION_EXCHANGEKEY:
        case AbstractMessage.OC_VERIFY_CHALLENGE:
            return null;
        default:
            System.err.println("Warning: cannot tell whether msg " + req + " is admin or not: unrecognized opcode!");
        }
        return null;
    }
}
