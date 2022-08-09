/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/***********************************************************************
 * Interface for the transaction queue scanner that is used as a
 * callback from messages like DumpHandlesRequest.
 ***********************************************************************/

public interface TransactionScannerInterface {
    public Transaction nextTransaction() throws Exception;

    public void close();
}
