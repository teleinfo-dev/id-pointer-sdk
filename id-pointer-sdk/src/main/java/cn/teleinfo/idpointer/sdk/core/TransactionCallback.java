/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/************************************************************************
 * Interface used to define objects that want to receive the streamed
 * results from RetrieveTxnResponse messages.
 ************************************************************************/
public interface TransactionCallback {

    /********************************************************************
     * Process the given transaction which was received via the stream
     * in the RetrieveTxnResponse message.
     ********************************************************************/
    public void processTransaction(Transaction txn) throws HandleException;

    /********************************************************************
     * Finish processing this request.  The given date (or more specifically,
     * the minimum date returned from all replicated servers) should be
     * used the next time that a RetrieveTxnRequest is sent.
     ********************************************************************/
    public void finishProcessing(long sourceDate);

    public void finishProcessing();

    public void setQueueLastTimestamp(String queueName, long sourceDate);

    public void processTransaction(String queueName, Transaction txn) throws HandleException;

}
