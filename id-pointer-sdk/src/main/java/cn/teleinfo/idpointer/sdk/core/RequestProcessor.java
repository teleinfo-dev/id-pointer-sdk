/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.net.InetAddress;

public interface RequestProcessor {
    AbstractIdResponse processRequest(AbstractIdRequest req, InetAddress caller) throws HandleException;

    void processRequest(AbstractIdRequest req, InetAddress caller, ResponseMessageCallback callback) throws HandleException;
}
