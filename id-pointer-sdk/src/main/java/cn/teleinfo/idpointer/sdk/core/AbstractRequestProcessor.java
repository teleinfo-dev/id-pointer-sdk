/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.net.InetAddress;

public abstract class AbstractRequestProcessor implements RequestProcessor {

    @Override
    public AbstractIdResponse processRequest(AbstractIdRequest req, InetAddress caller) {
        SimpleResponseMessageCallback callback = new SimpleResponseMessageCallback();
        try {
            processRequest(req, caller, callback);
        } catch (HandleException e) {
            return HandleException.toErrorResponse(req, e);
        }
        return callback.getResponse();
    }

}
