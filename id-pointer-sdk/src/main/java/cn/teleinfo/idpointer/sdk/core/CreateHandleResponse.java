/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public class CreateHandleResponse extends AbstractResponse {

    // may be null if the server does not return the created handle
    public byte handle[];

    public CreateHandleResponse(byte handle[]) {
        super(OC_CREATE_HANDLE, AbstractMessage.RC_SUCCESS);
        this.handle = handle;
    }

    public CreateHandleResponse(AbstractRequest req, byte handle[]) throws HandleException {
        super(req, AbstractMessage.RC_SUCCESS);
        this.handle = handle;
    }
}
