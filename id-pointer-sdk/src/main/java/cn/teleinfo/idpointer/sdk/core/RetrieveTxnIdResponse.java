/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import cn.teleinfo.idpointer.sdk.core.stream.util.FastDateFormat.FormatSpec;
import cn.teleinfo.idpointer.sdk.core.stream.util.FastDateFormat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/***************************************************************************
 * Response used to forward any new transactions to a replicated site/server. This response is used for server&lt;-&gt;server (or
 * replicator&lt;-&gt;server) communication.
 ***************************************************************************/

public class RetrieveTxnIdResponse extends AbstractIdResponse {
    private static FastDateFormat dateFormat = new FastDateFormat(new FormatSpec("-", " ", ":", "", ".", true, true), TimeZone.getDefault());

    public static final int NEED_TO_REDUMP = 1;
    public static final int SENDING_TRANSACTIONS = 2;

    private static final byte END_TRANSMISSION_RECORD = 0;
    private static final byte HANDLE_RECORD = 1;
    private static final byte NAME_OF_QUEUE_RECORD = 2;
    private static final byte END_OF_QUEUE_LAST_TIMESTAMP_RECORD = 3;

    // - settings used only on the server side -
    public RetrieveTxnIdRequest req = null;
    public TransactionQueueInterface txnQueue = null;

    // used on the server side as a source for the handle data
    private HandleStorage storage = null;
    private boolean caseSensitive;
    // used on the server side to determine where to start sending txns
    private long lastTxnId = 0;
    // txn queue may have not-committed-to-storage items; this is the latest safe one
    private long latestCommittedTxnId;
    private TransactionQueuesInterface txnQueues;
    private String ownReplicationServerName;
    private ReplicationStateInfo serversReplicationSourceSites;

    /***************************************************************
     * Constructor for the client side.
     ***************************************************************/
    public RetrieveTxnIdResponse() {
        super(AbstractMessage.OC_RETRIEVE_TXN_LOG, AbstractMessage.RC_SUCCESS);
        this.streaming = true;
    }

    /***************************************************************
     * Constructor for the server side.
     ***************************************************************/
    public RetrieveTxnIdResponse(TransactionQueueInterface txnQueue, long latestCommittedTxnId, RetrieveTxnIdRequest req, HandleStorage storage, boolean caseSensitive) throws HandleException {
        super(req, AbstractMessage.RC_SUCCESS);
        this.req = req;
        this.txnQueue = txnQueue;
        this.latestCommittedTxnId = latestCommittedTxnId;
        this.storage = storage;
        this.caseSensitive = caseSensitive;
        this.streaming = true;
        this.lastTxnId = req.lastTxnId;
    }

    /***************************************************************
     * Other constructor for the server side.
     ***************************************************************/
    public RetrieveTxnIdResponse(TransactionQueuesInterface allOtherTransactionQueues, String ownReplicationServerName, long latestCommittedTxnId, ReplicationStateInfo serversReplicationSourceSites, RetrieveTxnIdRequest req,
                                 HandleStorage storage, boolean caseSensitive) throws HandleException {
        super(req, AbstractMessage.RC_SUCCESS);
        this.req = req;
        this.txnQueues = allOtherTransactionQueues;
        this.serversReplicationSourceSites = serversReplicationSourceSites;
        this.storage = storage;
        this.caseSensitive = caseSensitive;
        this.streaming = true;
        this.ownReplicationServerName = ownReplicationServerName;
        this.latestCommittedTxnId = latestCommittedTxnId;
    }

    /**********************************************************************
     * Process the incoming stream and call the given callback for every transaction that is retrieved. The status codes that this function can return
     * include SENDING_TRANSACTIONS, or NEED_TO_REDUMP. If NEED_TO_REDUMP is returned, all of the handles should be requested from all of the servers
     * in the primary site.
     **********************************************************************/
    public int processStreamedPart(TransactionCallback callback, PublicKey sourceKey) throws HandleException {
        if (stream == null) {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Response stream not found");
        }

        // downgrade this threads priority so that request handler threads
        // don't starve.
        int threadPriority = Thread.currentThread().getPriority();
        /*
         * try { Thread.currentThread().setPriority(Thread.MIN_PRIORITY); } catch (Exception e) {
         * System.err.println("Unable to downgrade thread priority: "+e); }
         */

        DataInputStream in = null;
        SignedInputStream sin = null;
        long start = System.currentTimeMillis();
        int recordCount = 0;
        try {
            sin = new SignedInputStream(sourceKey, stream, socket);
            if (!secureStream && !sin.isSecure()) {
                throw new HandleException(HandleException.SECURITY_ALERT, "Insecure stream");
            }
            in = new DataInputStream(sin);

            int dataFormatVersion = in.readInt();
            int status = in.readInt();

            // verify the signature for the header information
            if (!sin.verifyBlock()) {
                throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
            }

            // if the status is NEED_TO_REDUMP, then only an END_TRANSMISSION_RECORD will
            // be sent
            String currentQueueName = null;
            while (true) {
                byte recordType = in.readByte();
                if (recordType == END_TRANSMISSION_RECORD) {
                    if (dataFormatVersion <= 1) {
                        long sourceDate = in.readLong();
                        if (!sin.verifyBlock()) {
                            throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
                        }
                        if (status == SENDING_TRANSACTIONS) callback.finishProcessing(sourceDate);
                        else if (status == NEED_TO_REDUMP) callback.finishProcessing();
                    } else {
                        if (!sin.verifyBlock()) {
                            throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
                        }
                        callback.finishProcessing();
                    }
                    return status;
                } else if (recordType == HANDLE_RECORD) {
                    recordCount++;
                    // get the record information...
                    Transaction txn = new Transaction();
                    txn.txnId = in.readLong();
                    txn.handle = new byte[in.readInt()];
                    in.readFully(txn.handle);
                    txn.action = in.readByte();
                    txn.date = in.readLong();

                    // get the current value of the handle record
                    switch (txn.action) {
                    case Transaction.ACTION_CREATE_HANDLE:
                    case Transaction.ACTION_UPDATE_HANDLE:
                        int numValues = in.readInt();
                        txn.values = new HandleValue[numValues];
                        for (int i = 0; i < numValues; i++) {
                            int valueSize = in.readInt();
                            byte buf[] = new byte[valueSize];
                            int r, n = 0;
                            while (n < valueSize && (r = in.read(buf, n, valueSize - n)) >= 0) {
                                n += r;
                            }
                            txn.values[i] = new HandleValue();
                            Encoder.decodeHandleValue(buf, 0, txn.values[i]);
                        }
                        break;
                    case Transaction.ACTION_DELETE_HANDLE:
                    case Transaction.ACTION_HOME_NA:
                    case Transaction.ACTION_UNHOME_NA:
                    default:
                        break;
                    }

                    // verify the signature for this record
                    if (!sin.verifyBlock()) {
                        throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
                    }
                    if (dataFormatVersion <= 1) {
                        callback.processTransaction(txn);
                    } else {
                        callback.processTransaction(currentQueueName, txn);
                    }
                    Thread.yield();
                } else if (recordType == NAME_OF_QUEUE_RECORD) {
                    int length = in.readInt();
                    byte[] bytes = new byte[length];
                    in.readFully(bytes);
                    if (!sin.verifyBlock()) {
                        throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
                    }
                    currentQueueName = Util.decodeString(bytes);
                } else if (recordType == END_OF_QUEUE_LAST_TIMESTAMP_RECORD) {
                    long sourceDate = in.readLong();
                    if (!sin.verifyBlock()) {
                        throw new HandleException(HandleException.SECURITY_ALERT, "Invalid signature on replication stream");
                    }
                    callback.setQueueLastTimestamp(currentQueueName, sourceDate);
                } else {
                    throw new HandleException(HandleException.INVALID_VALUE, "Unknown transmission record type: " + recordType);
                }
            }
        } catch (Exception e) {
            // the replication stream can be broken in the middle... no big deal
            //            System.err.println(">>> Exception streaming: " + e);
            //            e.printStackTrace(System.err);
            if (e instanceof HandleException) {
                throw (HandleException) e;
            }
            throw new HandleException(HandleException.INTERNAL_ERROR, "Exception receiving transactions", e);
        } finally {
            if (recordCount > 20) {
                long end = System.currentTimeMillis();
                long durationInSeconds = (end - start) / 1000L;
                long millis = (end - start) % 1000L;
                System.err.println("\"" + dateFormat.formatNow() + "\" Processed " + recordCount + " records in " + durationInSeconds + "." + pad3(millis) + " seconds");
            }
            if (in != null) { try { in.close(); } catch (Exception e) { } }
            if (sin != null) { try { sin.close(); } catch (Exception e) { } }
            if (stream != null) { try { stream.close(); } catch (Exception e) { } }
            try {
                Thread.currentThread().setPriority(threadPriority);
            } catch (Exception e) {
                System.err.println("Unable to upgrade thread priority: " + e);
            }
        }
    }

    private static String pad3(long millis) {
        if (millis < 10) return "00" + millis;
        if (millis < 100) return "0" + millis;
        return "" + millis;
    }

    /***********************************************************************
     * Write the response to the specified output stream. This will send all of the transactions that hash to the requestor beginning with the
     * specified transaction ID. This method is typically called on the server side.
     ***********************************************************************/
    @Override
    public void streamResponse(SignedOutputStream sout) throws HandleException {
        // need to get all of the transactions starting with req.lastTxnId
        try {
            if (req.replicationStateInfo != null) {
                streamResponseForAllTransactions(sout);
            } else {
                streamResponseForThisServersTransactions(sout);
            }
        } catch (Exception e) {
            // the replication stream can be broken in the middle... no biggie
            throw new HandleException(HandleException.INTERNAL_ERROR, "Exception sending transactions: ", e);
        }
    }

    private boolean needsRedump(TransactionQueueInterface queue, long lastSafeTxnIdInQueue, @SuppressWarnings("hiding") long lastTxnId, long lastTimestamp) {
        // don't require redump if there are no transactions at all
        // this allows to upgrade a secondary to a primary without dumping
        if (lastSafeTxnIdInQueue <= 0) return false;
        // server doesn't have queue, and mirror wants txns
        if (queue == null) {
            if (lastSafeTxnIdInQueue > lastTxnId) return true;
            return false;
        }
        // Typical case: client has pulled since queue started, so no redump
        if (queue.getFirstDate() <= lastTimestamp) return false;
        // don't require redump if asking for all transactions and we have transaction 1; but only if less than ten days of transactions
        // (this is a minor convenience for setting up mirrors of new servers)
        if (lastTxnId <= 0 && (queue.getFirstDate() + 864000000L > System.currentTimeMillis()) && queueHasTransactionNumberOne(queue)) {
            return false;
        }
        // similarly if asking for all transactions since the first available transaction and mirror was already in contact in last day
        if (lastTxnId <= 0 && (queue.getFirstDate() <= lastTimestamp + 86400000L) && queueHasTransactionNumberOne(queue)) {
            return false;
        }
        if (lastTxnId > 0 && (queue.getFirstDate() <= lastTimestamp + 86400000L) && queueStartsWithTransaction(queue, lastTxnId + 1)) {
            return false;
        }
        return true;
    }

    private static void logAboutNeedToRedumpResponse(String queueName, TransactionQueueInterface queue, long serversLastTxnId, long lastTxnId, long lastTimestamp) {
        System.err.println("NEED_TO_REDUMP sent about " + queueName + ": queue.lastTxnId=" + (queue == null ? "null" : queue.getLastTxnId()) + " serversLastTxnId=" + serversLastTxnId + " clientsLastTxnId=" + lastTxnId + " queue.firstDate="
            + (queue == null ? "null" : queue.getFirstDate()) + " clientsLastTimestamp=" + lastTimestamp);
    }

    private void streamResponseForAllTransactions(SignedOutputStream sout) throws Exception {
        DataOutputStream out = new DataOutputStream(sout);
        out.writeInt(2); // send a 4-byte data format version number
        ReplicationStateInfo sourceReplicationStateInfo = req.replicationStateInfo;

        long clientsLastTxnIdForThisServer = sourceReplicationStateInfo.getLastTxnId(ownReplicationServerName);
        long clientsLastTimestampForThisServer = sourceReplicationStateInfo.getLastTimestamp(ownReplicationServerName);
        TransactionQueueInterface thisServersTransactionQueue = txnQueues.getThisServersTransactionQueue();
        boolean needToRedump = false;
        if (thisServersTransactionQueue != null) {
            long serversLastTxnId = latestCommittedTxnId;
            needToRedump = needsRedump(thisServersTransactionQueue, serversLastTxnId, clientsLastTxnIdForThisServer, clientsLastTimestampForThisServer);
            if (needToRedump) {
                logAboutNeedToRedumpResponse("this server", thisServersTransactionQueue, serversLastTxnId, clientsLastTxnIdForThisServer, clientsLastTimestampForThisServer);
            }
        }
        Set<String> queueNames = new HashSet<>(serversReplicationSourceSites.keySet());
        queueNames.addAll(txnQueues.listQueueNames());
        for (String queueName : queueNames) {
            if (sourceReplicationStateInfo.isQueueNameInOwnSite(queueName)) continue;
            long clientsLastTxnId = sourceReplicationStateInfo.getLastTxnId(queueName);
            long clientsLastTimestamp = sourceReplicationStateInfo.getLastTimestamp(queueName);
            TransactionQueueInterface queue = txnQueues.getQueue(queueName);
            long serversLastTxnId = serversReplicationSourceSites.getLastTxnId(queueName);
            boolean thisQueueNeedsRedump = needsRedump(queue, serversLastTxnId, clientsLastTxnId, clientsLastTimestamp);
            if (thisQueueNeedsRedump) {
                logAboutNeedToRedumpResponse(queueName, queue, serversLastTxnId, clientsLastTxnId, clientsLastTimestamp);
            }
            needToRedump = needToRedump || thisQueueNeedsRedump;
        }

        if (needToRedump) {
            // The receiver may have missed some transactions that this
            // server doesn't have anymore. The receiver needs to
            // re-retrieve all handles! (as a separate request)
            out.writeInt(NEED_TO_REDUMP);

            // sign the header info
            sout.signBlock();

        } else {
            // we will forward all of the transactions that the requestor
            // doesn't have yet.
            out.writeInt(SENDING_TRANSACTIONS);

            // sign the header info
            sout.signBlock();

            if (thisServersTransactionQueue != null) {
                out.writeByte(NAME_OF_QUEUE_RECORD);
                out.writeInt(Util.encodeString(ownReplicationServerName).length);
                out.write(Util.encodeString(ownReplicationServerName));
                sout.signBlock();
                forwardTransactions(txnQueues.getThisServersTransactionQueue(), latestCommittedTxnId, clientsLastTxnIdForThisServer, out, sout);
                out.writeByte(END_OF_QUEUE_LAST_TIMESTAMP_RECORD);
                out.writeLong(System.currentTimeMillis());
                sout.signBlock();
            }
            for (String queueName : queueNames) {
                if (sourceReplicationStateInfo.isQueueNameInOwnSite(queueName)) continue;
                long serversLastTimestamp = getProxiedLastTimestampForQueueName(queueName);
                // avoid race condition where sender has transactions but last timestamp is -1
                if (serversLastTimestamp <= 0) continue;
                long clientsLastTxnId = sourceReplicationStateInfo.getLastTxnId(queueName);
                out.writeByte(NAME_OF_QUEUE_RECORD);
                out.writeInt(Util.encodeString(queueName).length);
                out.write(Util.encodeString(queueName));
                sout.signBlock();
                TransactionQueueInterface queue = txnQueues.getQueue(queueName);
                if (queue != null) forwardTransactions(queue, serversReplicationSourceSites.getLastTxnId(queueName), clientsLastTxnId, out, sout);
                out.writeByte(END_OF_QUEUE_LAST_TIMESTAMP_RECORD);
                out.writeLong(serversLastTimestamp);
                sout.signBlock();
            }
        }
        // write the ending summary record...
        out.writeByte(END_TRANSMISSION_RECORD);
        sout.signBlock();
    }

    private long getProxiedLastTimestampForQueueName(String queueName) {
        return serversReplicationSourceSites.getLastTimestamp(queueName);
    }

    private void streamResponseForThisServersTransactions(SignedOutputStream sout) throws Exception {
        DataOutputStream out = new DataOutputStream(sout);
        out.writeInt(1); // send a 4-byte data format version number

        // write a status code that will indicate if the receiver needs
        // to re-retrieve the entire database or not.

        long serversLastTxnId = latestCommittedTxnId;
        boolean needToRedump = needsRedump(txnQueue, serversLastTxnId, lastTxnId, this.req.lastQueryDate);

        if (needToRedump) {
            // The receiver may have missed some transactions that this
            // server doesn't have anymore. The receiver needs to
            // re-retrieve all handles! (as a separate request)
            logAboutNeedToRedumpResponse("this server", txnQueue, serversLastTxnId, lastTxnId, this.req.lastQueryDate);

            out.writeInt(NEED_TO_REDUMP);

            // sign the header info
            sout.signBlock();
        } else {
            // we will forward all of the transactions that the requestor
            // doesn't have yet.
            out.writeInt(SENDING_TRANSACTIONS);

            // sign the header info
            sout.signBlock();

            forwardTransactions(txnQueue, latestCommittedTxnId, req.lastTxnId, out, sout);
        }

        // write the ending summary record...
        out.writeByte(END_TRANSMISSION_RECORD);

        // write the date that the requestor should use
        // as the lastQueryDate for their next RetrieveTxnRequest.
        // This is needed because if there are no transactions in a while,
        // we don't want the receiver to have to redump the entire database.
        if (needToRedump) {
            out.writeLong(this.req.lastQueryDate);
        } else {
            out.writeLong(System.currentTimeMillis());
        }

        // sign this last record
        sout.signBlock();
    }

    private static boolean queueHasTransactionNumberOne(TransactionQueueInterface queue) {
        try {
            TransactionScannerInterface scanner = queue.getScanner(0);
            try {
                Transaction txn;
                while ((txn = scanner.nextTransaction()) != null) {
                    if (txn.txnId == 1) {
                        return true;
                    }
                    if (txn.txnId > 1) {
                        return false;
                    }
                }
                return false;
            } finally {
                scanner.close();
            }
        } catch (Exception e) {
            System.err.println("Exception checking first transaction");
            e.printStackTrace();
            return false;
        }
    }

    private static boolean queueStartsWithTransaction(TransactionQueueInterface queue, long n) {
        try {
            TransactionScannerInterface scanner = queue.getScanner(0);
            try {
                Transaction txn;
                while ((txn = scanner.nextTransaction()) != null) {
                    if (txn.txnId == n) {
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            } finally {
                scanner.close();
            }
        } catch (Exception e) {
            System.err.println("Exception checking first transaction");
            e.printStackTrace();
            return false;
        }
    }

    /***************************************************************************
     * Forward all of the transactions that the requestor doesn't already have.
     ***************************************************************************/
    private void forwardTransactions(TransactionQueueInterface queue, long latestSafeTxnIdInQueue, @SuppressWarnings("hiding") long lastTxnId, DataOutputStream out, SignedOutputStream sout) throws Exception {
        TransactionScannerInterface scanner = queue.getScanner(lastTxnId);
        try {
            Transaction txn = null;
            while ((txn = scanner.nextTransaction()) != null) {
                // ///// This currently doesn't handle wrap-around transaction IDs
                if (txn.txnId <= lastTxnId) {
                    continue;
                }
                if (txn.txnId > latestSafeTxnIdInQueue) {
                    break;
                }

                if (txn.action != Transaction.ACTION_UNHOME_NA && txn.action != Transaction.ACTION_HOME_NA) {
                    // only hash to the appropriate site, unless it is a NA home or unhome
                    // transaction. In that case, send the message to all servers!
                    boolean hashes;
                    switch (req.rcvrHashType) {
                    case SiteInfo.HASH_TYPE_BY_PREFIX:
                        hashes = (Math.abs(txn.hashOnNA % req.numServers) == req.serverNum);
                        break;
                    case SiteInfo.HASH_TYPE_BY_SUFFIX:
                        hashes = (Math.abs(txn.hashOnId % req.numServers) == req.serverNum);
                        break;
                    case SiteInfo.HASH_TYPE_BY_ALL:
                        hashes = (Math.abs(txn.hashOnAll % req.numServers) == req.serverNum);
                        break;
                    default:
                        System.err.println("Warning: unknown hash type (" + req.rcvrHashType + ") in RetrieveTxnRequest");
                        hashes = true; // if we're not sure.. just send it
                    }

                    if (!hashes) {
                        continue;
                    }
                }

                byte[][] hdlValue = null;
                if (txn.handle != null) {
                    hdlValue = storageGetRawHandleValues(txn.handle, null, null);
                }
                if (txn.action == Transaction.ACTION_CREATE_HANDLE || txn.action == Transaction.ACTION_UPDATE_HANDLE) {
                    if (hdlValue == null) {
                        // don't send an update transaction for a missing handle record
                        continue;
                    }
                } else if (txn.action == Transaction.ACTION_DELETE_HANDLE) {
                    if (hdlValue != null) {
                        // don't send a delete transaction for a handle record that does exist
                        continue;
                    }
                }

                // write a transaction record
                out.writeByte(HANDLE_RECORD);
                out.writeLong(txn.txnId);
                out.writeInt(txn.handle == null ? 0 : txn.handle.length);
                if (txn.handle != null) {
                    out.write(txn.handle, 0, txn.handle.length);
                }
                out.writeByte(txn.action);
                out.writeLong(txn.date);

                // send the current value of the handle record
                switch (txn.action) {
                case Transaction.ACTION_CREATE_HANDLE:
                case Transaction.ACTION_UPDATE_HANDLE:
                    if (hdlValue == null) {
                        out.writeInt(0);
                    } else {
                        out.writeInt(hdlValue.length);
                        for (byte[] element : hdlValue) {
                            if (element == null) {
                                out.writeInt(0);
                            } else {
                                out.writeInt(element.length);
                                out.write(element);
                            }
                        }
                    }
                    break;
                case Transaction.ACTION_DELETE_HANDLE:
                default:
                    break;
                }
                // sign this transaction record
                sout.signBlock();
            }
        } finally {
            scanner.close();
        }
    }

    // needed for custom storage modules
    private byte[][] storageGetRawHandleValues(byte[] handle, int[] indexList, byte[][] typeList) throws HandleException {
        try {
            return storage.getRawHandleValues(caseSensitive ? handle : Util.upperCase(handle), indexList, typeList);
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return null;
            }
            throw e;
        }
    }
}
