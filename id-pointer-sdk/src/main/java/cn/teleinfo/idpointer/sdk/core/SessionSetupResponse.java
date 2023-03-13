/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public class SessionSetupResponse extends AbstractResponse {

    /**
     * @see cn.teleinfo.idpointer.sdk.core.Common
     * {@link Common.KEY_EXCHANGE_NONE}
     */
    //member variable
    public int keyExchangeMode; // KEY_EXCHANGE_* from Common.java
    // one of: encrypted session key, exchange pubkey, or DH params
    public byte data[] = null;

    public SessionSetupResponse(int mode, byte data[]) {
        super(OC_SESSION_SETUP, AbstractMessage.RC_SUCCESS);
        this.data = data;
        this.keyExchangeMode = mode;
    }

    public SessionSetupResponse(SessionSetupRequest req, byte data[]) throws HandleException {
        super(req, AbstractMessage.RC_SUCCESS);
        this.data = data;
        this.keyExchangeMode = req.keyExchangeMode;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append(' ');

        return sb.toString();
    }
}
