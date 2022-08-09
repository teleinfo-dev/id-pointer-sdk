/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/***********************************************************************
 * Interface for the transaction queue that is used as a callback from
 * messages like DumpHandlesRequest.
 ***********************************************************************/
public interface TransactionQueueInterface {

    public long getLastTxnId();

    public void addQueueListener(TransactionQueueListener l);

    public void removeQueueListener(TransactionQueueListener l);

    /** Log the specified transaction to the current queue or throw an exception
     * if there is an error or if this is a read-only queue.
     */
    public void addTransaction(long txnId, byte handle[], HandleValue[] values, byte action, long date) throws Exception;

    public void addTransaction(Transaction txn) throws Exception;

    public TransactionScannerInterface getScanner(long lastTxnId) throws Exception;

    public long getFirstDate();

    /** Close any open files or resources in use by the queue. */
    public void shutdown();

    public void deleteUntilDate(long date);
}
