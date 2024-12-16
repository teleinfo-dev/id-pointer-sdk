/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/***********************************************************************
 * ResponseMessageCallback is an interface that is used to get
 * continuation messages from multi-message responses.
 ***********************************************************************/

public interface ResponseMessageCallback {

    /*********************************************************************
     * This is called when a message has been received and needs to be
     * handled. <i>message</i> is the message that has been received.
     * Messages are received and processed in order.
     *********************************************************************/
    public void handleResponse(AbstractIdResponse message) throws HandleException;

}
