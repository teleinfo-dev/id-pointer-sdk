/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/***************************************************************************
 * Response used to forward all handles for a specific prefix.
 * This message will usually be broken up into many messages, each of
 * which contains a bunch of handles.  Clients who receive this message
 * should use a callback to process the continuation messages.
 ***************************************************************************/
public class ListHandlesIdResponse extends AbstractIdResponse {
    public byte handles[][];

    /***************************************************************
     * Constructor for the server side.
     ***************************************************************/
    public ListHandlesIdResponse(ListHandlesIdRequest req, byte handles[][]) throws HandleException {
        super(req, AbstractMessage.RC_SUCCESS);
        this.handles = handles;
    }

    /***************************************************************
     * Constructor for the client side.
     ***************************************************************/
    public ListHandlesIdResponse() {
        super(AbstractMessage.OC_LIST_HANDLES, AbstractMessage.RC_SUCCESS);
    }

}
