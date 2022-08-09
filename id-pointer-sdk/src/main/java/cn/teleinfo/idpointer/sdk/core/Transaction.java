/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/**
 * Class that holds the information known about a single transaction
 * on a handle server.  This is generally never used on the client side.
 */

public class Transaction {
    public static final byte ACTION_PLACEHOLDER = 0;
    public static final byte ACTION_CREATE_HANDLE = 1;
    public static final byte ACTION_DELETE_HANDLE = 2;
    public static final byte ACTION_UPDATE_HANDLE = 3;
    public static final byte ACTION_HOME_NA = 4;
    public static final byte ACTION_UNHOME_NA = 5;
    public static final byte ACTION_DELETE_ALL = 6;

    public long txnId;
    public byte handle[];
    public byte action;
    public long date;
    public int hashOnAll;
    public int hashOnNA;
    public int hashOnId;

    // this is only used on the receiving side
    // also used in Storage transaction log
    public HandleValue values[] = null;

    @Override
    public String toString() {
        return "Transaction[txn: id=" + txnId + "; action=" + actionToString(action) + "; hdl=" + Util.decodeString(handle) + "; date=" + (new java.util.Date(date)) + ";]";
    }

    public static final String actionToString(byte action) {
        switch (action) {
        case ACTION_PLACEHOLDER:
            return "placeholder";
        case ACTION_CREATE_HANDLE:
            return "create";
        case ACTION_DELETE_HANDLE:
            return "delete";
        case ACTION_UPDATE_HANDLE:
            return "update";
        case ACTION_HOME_NA:
            return "home";
        case ACTION_UNHOME_NA:
            return "unhome";
        case ACTION_DELETE_ALL:
            return "delete_all!";
        default:
            return "unknown";
        }
    }

    public static final byte stringToAction(String action) {
        if ("placeholder".equals(action)) return ACTION_PLACEHOLDER;
        if ("create".equals(action)) return ACTION_CREATE_HANDLE;
        if ("delete".equals(action)) return ACTION_DELETE_HANDLE;
        if ("update".equals(action)) return ACTION_UPDATE_HANDLE;
        if ("home".equals(action)) return ACTION_HOME_NA;
        if ("unhome".equals(action)) return ACTION_UNHOME_NA;
        if ("delete_all!".equals(action)) return ACTION_DELETE_ALL;
        throw new IllegalArgumentException();
    }

    public Transaction() {
    }

    public Transaction(long txnId, byte[] handle, HandleValue[] values, byte action, long date) {
        this(txnId, handle, action, date);
        this.values = values;
    }

    public Transaction(long txnId, byte[] handle, byte action, long date) {
        this.txnId = txnId;
        this.handle = handle;
        this.action = action;
        this.date = date;
        // generate each type of hash for the handle so that we don't have to
        // re-generate it for every retrieve-transaction request.
        try {
            this.hashOnAll = SiteInfo.getHandleHash(handle, SiteInfo.HASH_TYPE_BY_ALL);
            this.hashOnNA = SiteInfo.getHandleHash(handle, SiteInfo.HASH_TYPE_BY_PREFIX);
            this.hashOnId = SiteInfo.getHandleHash(handle, SiteInfo.HASH_TYPE_BY_SUFFIX);
        } catch (HandleException e) {
            throw new AssertionError(e);
        }
    }

}
