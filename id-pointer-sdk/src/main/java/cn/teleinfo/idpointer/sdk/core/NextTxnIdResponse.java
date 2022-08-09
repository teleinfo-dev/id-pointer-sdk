/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public class NextTxnIdResponse extends AbstractResponse {
    public long nextTxnId;

    public NextTxnIdResponse(long nextTxnId) {
        super(AbstractMessage.OC_GET_NEXT_TXN_ID, AbstractMessage.RC_SUCCESS);
        this.nextTxnId = nextTxnId;
    }

    public NextTxnIdResponse(AbstractRequest req, long nextTxnId) throws HandleException {
        super(req, AbstractMessage.RC_SUCCESS);
        this.nextTxnId = nextTxnId;
    }
}
