/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import cn.teleinfo.idpointer.sdk.core.stream.StreamTable;
import cn.teleinfo.idpointer.sdk.core.stream.StreamVector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/***************************************************************************
 * Response used to send all handles in the database to a replicated site/server.
 * This response is used for server&lt;-&gt;server (or replicator&lt;-&gt;server)
 * communication.
 ***************************************************************************/

public class DumpHandlesIdResponse extends AbstractIdResponse {
    // - settings used only on the server side -
    public DumpHandlesIdRequest req = null;

    // used on the server side as a source for the handle data
    private HandleStorage storage = null;
    private TransactionQueueInterface queue = null;
    private ReplicationDaemonInterface replicationDaemon;

    private byte lastProcessedRecordType = -2; //-2 indicates no records ever processed
    private byte[] lastProcessedRecord = null;

    // renamed from END_TRANSMISSION_RECORD
    public static final byte THIS_SERVER_REPLICATION_INFO_RECORD = 0;
    public static final byte HANDLE_RECORD = 1;
    public static final byte HOMED_PREFIX_RECORD = 2;
    public static final byte HANDLE_DATE_RECORD = 3;
    public static final byte NA_DATE_RECORD = 4;
    public static final byte OTHER_SITE_REPLICATION_INFO_RECORD = 5;
    public static final byte ABSOLUTELY_DONE_RECORD = -1;

    /***************************************************************
     * Constructor for the server side.
     ***************************************************************/
    public DumpHandlesIdResponse(DumpHandlesIdRequest req, HandleStorage storage, TransactionQueueInterface queue, ReplicationDaemonInterface replicationDaemon) throws HandleException {
        super(req, AbstractMessage.RC_SUCCESS);
        this.req = req;
        this.storage = storage;
        this.queue = queue;
        this.replicationDaemon = replicationDaemon;
        this.streaming = true;
    }

    /***************************************************************
     * Constructor for the client side.
     ***************************************************************/
    public DumpHandlesIdResponse() {
        super(AbstractMessage.OC_RETRIEVE_TXN_LOG, AbstractMessage.RC_SUCCESS);
        this.streaming = true;
    }

    public byte getLastProcessedRecordType() {
        return lastProcessedRecordType;
    }

    public byte[] getLastProcessedRecord() {
        return lastProcessedRecord;
    }

    public void setLastProcessedRecordType(byte lastProcessedRecordType) {
        this.lastProcessedRecordType = lastProcessedRecordType;
    }

    public void setLastProcessedRecord(byte[] lastProcessedRecord) {
        this.lastProcessedRecord = lastProcessedRecord;
    }

    /**********************************************************************
     * Process the incoming stream and call the given callback for every
     * handle that is retrieved.
     **********************************************************************/
    public void processStreamedPart(DumpHandlesCallback callback, PublicKey sourceKey) throws HandleException {
        if (stream == null) {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Response stream not found");
        }

        // downgrade this threads priority so that request handler threads
        // don't starve.
        /*
        int threadPriority = Thread.currentThread().getPriority();
        
        try {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        } catch (Exception e) {
        System.err.println("Unable to downgrade thread priority: ",e);
        }
         */

        DataInputStream in = null;
        SignedInputStream sin = null;
        try {
            sin = new SignedInputStream(sourceKey, stream, socket);
            if (!secureStream && !sin.isSecure()) {
                throw new HandleException(HandleException.SECURITY_ALERT, "Insecure stream");
            }
            in = new DataInputStream(sin);

            int dataVersion = in.readInt();

            // verify the signature for the header information
            if (!sin.verifyBlock()) {
                throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
            }

            boolean sawAbsolutelyDone;
            while (true) {
                byte recordType;
                try {
                    recordType = in.readByte();
                } catch (EOFException e) {
                    sawAbsolutelyDone = false;
                    break;
                }
                if (recordType == ABSOLUTELY_DONE_RECORD) {
                    sawAbsolutelyDone = true;
                    lastProcessedRecordType = ABSOLUTELY_DONE_RECORD;
                    break;
                } else if (recordType == THIS_SERVER_REPLICATION_INFO_RECORD) {
                    long date = in.readLong();
                    long txnId = in.readLong();
                    // verify the signature for this record
                    if (!sin.verifyBlock()) {
                        throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
                    }
                    callback.processThisServerReplicationInfo(date, txnId);
                    lastProcessedRecordType = THIS_SERVER_REPLICATION_INFO_RECORD;
                    if (dataVersion < 2) return;
                } else if (recordType == HANDLE_RECORD) {
                    // get the record information...
                    // read the handle name
                    byte handleBytes[] = new byte[in.readInt()];
                    in.readFully(handleBytes);
                    // read the number of values
                    HandleValue values[] = new HandleValue[in.readInt()];
                    // read each of the values
                    for (int i = 0; i < values.length; i++) {
                        byte valueBytes[] = new byte[in.readInt()];
                        in.readFully(valueBytes);
                        values[i] = new HandleValue();
                        Encoder.decodeHandleValue(valueBytes, 0, values[i]);
                    }
                    // verify the signature for this record
                    if (!sin.verifyBlock()) {
                        throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
                    }
                    // pass the retrieved handle to the callback
                    callback.addHandle(handleBytes, values);
                    lastProcessedRecordType = HANDLE_RECORD;
                    lastProcessedRecord = handleBytes;
                } else if (recordType == HOMED_PREFIX_RECORD) {
                    byte naHandle[] = new byte[in.readInt()];
                    in.readFully(naHandle);
                    // verify the signature for this record
                    if (!sin.verifyBlock()) {
                        throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
                    }
                    callback.addHomedPrefix(naHandle);
                    lastProcessedRecordType = HOMED_PREFIX_RECORD;
                    lastProcessedRecord = naHandle;
                } else if (recordType == HANDLE_DATE_RECORD) {
                    byte[] handle = new byte[in.readInt()];
                    in.readFully(handle);
                    long date = in.readLong();
                    int priority = in.readInt();
                    // verify the signature for this record
                    if (!sin.verifyBlock()) {
                        throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
                    }
                    callback.setLastCreateOrDeleteDate(handle, date, priority);
                    lastProcessedRecordType = HANDLE_DATE_RECORD;
                    lastProcessedRecord = handle;
                } else if (recordType == NA_DATE_RECORD) {
                    byte[] naHandle = new byte[in.readInt()];
                    in.readFully(naHandle);
                    long date = in.readLong();
                    int priority = in.readInt();
                    // verify the signature for this record
                    if (!sin.verifyBlock()) {
                        throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
                    }
                    callback.setLastHomeOrUnhomeDate(naHandle, date, priority);
                    lastProcessedRecordType = NA_DATE_RECORD;
                    lastProcessedRecord = naHandle;
                } else if (recordType == OTHER_SITE_REPLICATION_INFO_RECORD) {
                    byte statusBytes[] = new byte[in.readInt()];
                    in.readFully(statusBytes);
                    // verify the signature for this record
                    if (!sin.verifyBlock()) {
                        throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
                    }
                    StreamTable replicationConfig = new StreamTable();
                    replicationConfig.readFrom(Util.decodeString(statusBytes));
                    callback.processOtherSiteReplicationInfo(replicationConfig);
                    lastProcessedRecordType = OTHER_SITE_REPLICATION_INFO_RECORD;
                } else {
                    throw new HandleException(HandleException.INVALID_VALUE, "Unknown transmission record type: " + recordType);
                }
                Thread.yield();
            }
            if (!sawAbsolutelyDone) {
                System.err.println(">>> Dump stream ended unexpectedly");
                throw new HandleException(HandleException.SERVER_ERROR, "Dump stream ended unexpectedly");
            }
        } catch (Exception e) {
            System.err.println(">>> Exception receiving dump: " + e);
            if (e instanceof HandleException) throw (HandleException) e;
            throw new HandleException(HandleException.INTERNAL_ERROR, "Exception receiving handle dump", e);
        } finally {
            if (sin != null) {
                try { sin.close(); } catch (Exception e) {}
            }
            if (in != null) {
                try { in.close(); } catch (Exception e) {}
            }
            if (stream != null) {
                try { stream.close(); } catch (Exception e) {}
            }
            //      try {
            //        Thread.currentThread().setPriority(threadPriority);
            //      } catch (Exception e) {
            //        System.err.println("Unable to upgrade thread priority: "+e);
            //      }
        }
    }

    /***********************************************************************
     * Write the response to the specified output stream.  This will
     * send all of the handles that hash to the requestor beginning with
     * the specified transaction ID.  This method is typically called
     * on the server side.
     ***********************************************************************/
    @Override
    public void streamResponse(SignedOutputStream sout) throws HandleException {
        // stop doing replication while dumping
        if (replicationDaemon != null) replicationDaemon.pauseReplication();

        // need to get all of the handles
        try {
            // use our private key to sign the response stream
            DataOutputStream out = new DataOutputStream(sout);

            // send a 4-byte data format version number
            out.writeInt(2); // 2 is for the new potentially multi-master dump
            sout.signBlock();

            if (req.startingPoint != null) {
                System.err.println("Resuming dump.");
                resumeDumpSendFromStartingPoint(sout, out);
            } else { //dump everything
                // Because we suspend replication from other primaries but NOT new transactions on THIS primary,
                // we need to make sure the information we send is consistent.
                // We first calculate the last transaction number.
                // We then send the last update/delete/home/unhome dates; some of these will occur after the calculated last transaction.
                // We then send the handles and NAs; some values will reflect things that occurred after the last transaction and last dates sent previously.
                // We then send the last transaction number and the replication status of other primaries (which hasn't changed).
                // The dumpee will start after the calculated last transaction, which means re-doing some transactions that were really already included
                // in what was sent, but that does not pose a problem.
                if (replicationDaemon != null) {
                    StreamTable replicationStatus = replicationDaemon.replicationStatus();
                    replicationStatus = omitEmptyQueues(replicationStatus);
                    byte[] statusBytes = Util.encodeString(replicationStatus.writeToString());
                    out.writeByte(OTHER_SITE_REPLICATION_INFO_RECORD);
                    out.writeInt(statusBytes.length);
                    out.write(statusBytes);
                    sout.signBlock();
                }
                if (queue != null) {
                    long time = System.currentTimeMillis();
                    long txnId = queue.getLastTxnId();

                    out.writeByte(THIS_SERVER_REPLICATION_INFO_RECORD);

                    // Write the date that the requestor should use as the lastQueryDate
                    // for their next RetrieveHandlesRequest to this server.
                    // This is needed because if there are no transactions in a while,
                    // we don't want the receiver to have to redump the entire database.
                    out.writeLong(time);

                    // Write the transaction ID that the requestor should use as the
                    // lastTransactionID for their next RetrieveHandlesRequest to this server.
                    out.writeLong(txnId);

                    // sign this record
                    sout.signBlock();
                }

                if (replicationDaemon != null) {
                    // send handle and NA last change information
                    Iterator<byte[]> iter = replicationDaemon.handleIterator();
                    while (iter.hasNext()) {
                        out.writeByte(HANDLE_DATE_RECORD);
                        out.write(iter.next());
                        sout.signBlock();
                    }
                    iter = replicationDaemon.naIterator();
                    while (iter.hasNext()) {
                        out.writeByte(NA_DATE_RECORD);
                        out.write(iter.next());
                        sout.signBlock();
                    }
                }
                storage.scanHandles(new HdlForwarder(out, sout, false));
                storage.scanNAs(new HdlForwarder(out, sout, true));
            }
            out.writeByte(ABSOLUTELY_DONE_RECORD);
        } catch (Exception e) {
            // the replication stream can be broken in the middle... no biggie
            throw new HandleException(HandleException.INTERNAL_ERROR, "Exception sending transactions: ", e);
        } finally {
            if (replicationDaemon != null) replicationDaemon.unpauseReplication();
        }
    }

    private static StreamTable omitEmptyQueues(StreamTable replicationStatus) {
        List<String> namesToDelete = new ArrayList<>();
        for (Enumeration<String> e = replicationStatus.keys(); e.hasMoreElements();) {
            String name = e.nextElement();
            StreamVector serverStates = (StreamVector) replicationStatus.get(name);
            boolean found = false;
            for (int i = 0; i < serverStates.size(); i++) {
                StreamTable serverState = (StreamTable) serverStates.get(i);
                long lastTxnId = serverState.getLong(ReplicationStateInfo.LAST_TXN_ID, -1);
                if (lastTxnId >= 0) {
                    found = true;
                    break;
                }
            }
            if (!found) namesToDelete.add(name);
        }
        for (String name : namesToDelete) {
            replicationStatus.remove(name);
        }
        return replicationStatus;
    }

    private void resumeDumpSendFromStartingPoint(SignedOutputStream sout, DataOutputStream out) throws IOException, SignatureException, HandleException {
        if (!storage.supportsDumpResumption()) {
            throw new HandleException(HandleException.SERVER_ERROR, "Cannot resume dump from storage " + storage.getClass());
        }
        int startingPointType = req.startingPointType;
        if (startingPointType == DumpHandlesIdRequest.HANDLE_REPLICATION_DB) {
            if (replicationDaemon != null) {
                Iterator<byte[]> iter = replicationDaemon.handleIteratorFrom(req.startingPoint, false);
                while (iter.hasNext()) {
                    out.writeByte(HANDLE_DATE_RECORD);
                    out.write(iter.next());
                    sout.signBlock();
                }
                iter = replicationDaemon.naIterator();
                while (iter.hasNext()) {
                    out.writeByte(NA_DATE_RECORD);
                    out.write(iter.next());
                    sout.signBlock();
                }
            }
            storage.scanHandles(new HdlForwarder(out, sout, false));
            storage.scanNAs(new HdlForwarder(out, sout, true));
        } else if (startingPointType == DumpHandlesIdRequest.NA_REPLICATION_DB) {
            if (replicationDaemon != null) {
                Iterator<byte[]> iter = replicationDaemon.naIteratorFrom(req.startingPoint, false);
                while (iter.hasNext()) {
                    out.writeByte(NA_DATE_RECORD);
                    out.write(iter.next());
                    sout.signBlock();
                }
            }
            storage.scanHandles(new HdlForwarder(out, sout, false));
            storage.scanNAs(new HdlForwarder(out, sout, true));
        } else if (startingPointType == DumpHandlesIdRequest.HANDLE) {
            storage.scanHandlesFrom(req.startingPoint, false, new HdlForwarder(out, sout, false));
            storage.scanNAs(new HdlForwarder(out, sout, true));
        } else if (startingPointType == DumpHandlesIdRequest.NA) {
            storage.scanNAsFrom(req.startingPoint, false, new HdlForwarder(out, sout, true));
        }
    }

    private class HdlForwarder implements ScanCallback {
        private final boolean scanningNAs;
        private final DataOutputStream out;
        private final SignedOutputStream sout;

        public HdlForwarder(DataOutputStream out, SignedOutputStream sout, boolean scanningNAs) {
            this.out = out;
            this.sout = sout;
            this.scanningNAs = scanningNAs;
        }

        /***************************************************************************
         * Called by database scanner and will forward the handle and its value
         * over the connection.
         ***************************************************************************/
        @Override
        public void scanHandle(byte handle[]) throws HandleException {
            if (!scanningNAs && req.serverNum != SiteInfo.determineServerNum(handle, req.rcvrHashType, req.numServers)) {
                // this handle doesn't belong on the requesting server, so don't send it
                return;
            }

            try {
                if (scanningNAs) {
                    out.write(HOMED_PREFIX_RECORD);
                    out.writeInt(handle.length);
                    out.write(handle);
                } else {
                    byte values[][] = storage.getRawHandleValues(handle, null, null);
                    if (values == null) {
                        System.err.println("Unexpected null values for handle " + Util.decodeString(handle));
                        return;
                    }

                    out.write(HANDLE_RECORD);
                    out.writeInt(handle.length);
                    out.write(handle);
                    out.writeInt(values.length);

                    for (byte[] value : values) {
                        out.writeInt(value.length);
                        out.write(value);
                    }
                }
                // sign this record...
                sout.signBlock();
            } catch (Exception e) {
                if (e instanceof HandleException) throw (HandleException) e;
                throw new HandleException(HandleException.INTERNAL_ERROR, e);
            }
        }
    }

}
