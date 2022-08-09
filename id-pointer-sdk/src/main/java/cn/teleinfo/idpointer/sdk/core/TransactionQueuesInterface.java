/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.util.List;

public interface TransactionQueuesInterface {

    public List<String> listQueueNames();

    public TransactionQueueInterface getQueue(String name);

    public TransactionQueueInterface getThisServersTransactionQueue();

    public TransactionQueueInterface createNewQueue(String name) throws Exception;

    public TransactionQueueInterface getOrCreateTransactionQueue(String name) throws Exception;

    //    public void addQueueListener(TransactionQueueListener l);
    //
    //    public void removeQueueListener(TransactionQueueListener l);
    
    /** Close any open files or resources in use by the queues. */
    public void shutdown();
}
