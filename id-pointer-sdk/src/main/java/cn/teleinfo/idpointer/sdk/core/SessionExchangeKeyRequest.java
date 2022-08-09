/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/******************************************************************************
 * Request used to setup a new session.  Holds the identity of the client
 * exchange key (either a public key or a handle/index pair).
 ******************************************************************************/

public class SessionExchangeKeyRequest extends AbstractRequest {
    //client send encrypted session key to server
    //server uses its RSA private key to decrypt
    private byte encryptedSessionKey[] = null;

    public SessionExchangeKeyRequest(byte encryptedSessionKey[]) {
        super(Common.BLANK_HANDLE, AbstractMessage.OC_SESSION_EXCHANGEKEY, null);
        this.encryptedSessionKey = encryptedSessionKey;
    }

    public byte[] getEncryptedSessionKey() {
        return encryptedSessionKey;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append(' ');

        return sb.toString();
    }
}
