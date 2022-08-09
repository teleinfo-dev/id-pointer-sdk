/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.security.HdlSecurityProvider;

import java.util.Arrays;

public class ClientSideSessionInfo extends SessionInfo {
    private final ServerInfo server;
    private byte[] exchangeKeyHandle = null;
    private int exchangeKeyIndex = -1;
    private byte[] exchangePublicKey = null;

    @Deprecated
    public ClientSideSessionInfo(int sessionid, byte[] sessionkey, byte[] identityHandle, int identityindex, ServerInfo server, int majorProtocolVersion, int minorProtocolVersion) {
        this(sessionid, sessionkey, identityHandle, identityindex, HdlSecurityProvider.ENCRYPT_ALG_DES, server, majorProtocolVersion, minorProtocolVersion);
    }

    public ClientSideSessionInfo(int sessionid, byte[] sessionkey, byte[] identityHandle, int identityindex, int algorithmCode, ServerInfo server, int majorProtocolVersion, int minorProtocolVersion) {
        super(sessionid, sessionkey, identityHandle, identityindex, algorithmCode, majorProtocolVersion, minorProtocolVersion);
        this.server = server;
    }

    public void setExchangeKeyRef(byte keyrefHandle[], int keyrefindex) {
        this.exchangeKeyHandle = keyrefHandle;
        this.exchangeKeyIndex = keyrefindex;
    }

    public void setExchangePublicKey(byte key[]) {
        this.exchangePublicKey = key;

    }

    public byte[] getExchangeKeyRefHandle() {
        return exchangeKeyHandle;
    }

    public int getExchangeKeyRefindex() {
        return exchangeKeyIndex;
    }

    public byte[] getExchagePublicKey() {
        return exchangePublicKey;
    }

    public void takeValuesFromOption(SessionSetupInfo option) {
        this.exchangeKeyHandle = option.exchangeKeyHandle;
        this.exchangeKeyIndex = option.exchangeKeyIndex;
        this.exchangePublicKey = option.publicExchangeKey;
        this.timeOut = option.timeout;
        this.encryptMessage = option.encrypted;
        this.authenticateMessage = option.authenticated;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(exchangeKeyHandle);
        result = prime * result + exchangeKeyIndex;
        result = prime * result + Arrays.hashCode(exchangePublicKey);
        result = prime * result + ((server == null) ? 0 : server.hashCode());
        return result;
    }

    /** Returns true if the given object is an equivalent ClientSideSessionInfo
    object */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj.getClass() != this.getClass()) return false;

        ClientSideSessionInfo info = (ClientSideSessionInfo) obj;

        if ((server == null && info.server != null) || (server != null && info.server == null) || (server != null && !server.equals(info.server))) return false;

        if (exchangeKeyIndex != info.getExchangeKeyRefindex()) return false;

        if (!Util.equals(exchangeKeyHandle, info.getExchangeKeyRefHandle())) return false;

        if (!Util.equals(exchangePublicKey, info.getExchagePublicKey())) return false;

        return super.equals(info);
    }

}
