/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.Arrays;

public class ServerInfo {
    public int serverId; // The "ID" of this server
    public byte[] ipAddress; // IP Address of the server
    public byte publicKey[]; // The public key used to verify responses from the server
    public Interface[] interfaces;// The "interfaces" presented by this server (port,protocol,etc)

    private String addrString = null;

    /**
     * Return the server's Interface for the given protocol which can handle the
     * given request; assume server has only 1 such interface.
     */
    public Interface interfaceWithProtocol(int desiredProtocol, AbstractIdRequest req) {
        for (Interface interface1 : this.interfaces) {
            if (interface1.protocol == desiredProtocol && interface1.canHandleRequest(req)) {
                return interface1;
            }
        }

        return null;
    }

    private static final int IPV6_ONLY = Common.IP_ADDRESS_LENGTH - 4;

    public boolean isIPv4() {
        for (int i = 0; i < IPV6_ONLY; i++) {
            if (ipAddress[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public InetAddress getInetAddress() {
        if (ipAddress == null || ipAddress.length != Common.IP_ADDRESS_LENGTH) throw new IllegalStateException("ServerInfo with ipAddress not 16 bytes");
        try {
            if (isIPv4()) return InetAddress.getByAddress(Arrays.copyOfRange(ipAddress, IPV6_ONLY, Common.IP_ADDRESS_LENGTH));
            else return InetAddress.getByAddress(ipAddress);
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        }
    }

    public String getAddressString() {
        if (addrString != null) return addrString;

        StringBuffer sb = new StringBuffer();
        if (ipAddress == null) return "";

        if (isIPv4()) {
            for (int i = IPV6_ONLY; i < ipAddress.length; i++) {
                if (sb.length() > 0) sb.append('.');
                sb.append(0x00ff & ipAddress[i]);
            }
        } else if (ipAddress.length == 4) {
            for (byte ipAddressByte : ipAddress) {
                if (sb.length() > 0) sb.append('.');
                sb.append(0x00ff & ipAddressByte);
            }
        } else if (ipAddress.length != 16) {
            for (int i = 0; i < ipAddress.length; i += 2) {
                if (sb.length() > 0) sb.append(':');
                sb.append(Util.decodeHexString(ipAddress, i, 2, false));
            }
        } else {
            return Util.rfcIpRepr(ipAddress);
        }

        addrString = sb.toString();
        return addrString;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(serverId);
        sb.append(' ');
        sb.append(getAddressString());
        sb.append(' ');
        Interface ifcs[] = interfaces;
        boolean hasInterfaces = false;
        for (int i = 0; ifcs != null && i < ifcs.length; i++) {
            Interface ifc = ifcs[i];
            if (ifc == null) continue;
            if (!hasInterfaces) {
                sb.append("(");
                hasInterfaces = true;
            } else {
                sb.append(",");
            }
            sb.append(Interface.protocolName(ifc.protocol));
            sb.append('/');
            sb.append(ifc.port);
            sb.append('/');
            sb.append(Interface.typeName(ifc.type));
        }
        if (hasInterfaces) sb.append(")");
        return sb.toString();
    }

    public ServerInfo cloneServerInfo() {
        ServerInfo si2 = new ServerInfo();
        si2.serverId = serverId;
        byte tmp[] = ipAddress;
        if (tmp != null) {
            si2.ipAddress = new byte[tmp.length];
            System.arraycopy(tmp, 0, si2.ipAddress, 0, tmp.length);
        } else {
            si2.ipAddress = null;
        }
        tmp = publicKey;
        if (tmp != null) {
            si2.publicKey = new byte[tmp.length];
            System.arraycopy(tmp, 0, si2.publicKey, 0, tmp.length);
        } else {
            si2.publicKey = null;
        }
        Interface tmpIA[] = interfaces;
        si2.interfaces = tmpIA == null ? null : new Interface[tmpIA.length];
        for (int i = 0; tmpIA != null && i < tmpIA.length; i++) {
            Interface tmpI = tmpIA[i];
            si2.interfaces[i] = tmpI == null ? null : tmpI.cloneInterface();
        }
        return si2;
    }

    public PublicKey getPublicKey() throws Exception {
        return Util.getPublicKeyFromBytes(publicKey, 0);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(interfaces);
        result = prime * result + Arrays.hashCode(ipAddress);
        result = prime * result + Arrays.hashCode(publicKey);
        result = prime * result + serverId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ServerInfo other = (ServerInfo) obj;
        if (serverId != other.serverId) return false;
        if (!Arrays.equals(ipAddress, other.ipAddress)) return false;
        if (!Arrays.equals(publicKey, other.publicKey)) return false;
        if (!Arrays.equals(interfaces, other.interfaces)) return false;
        return true;
    }

    public boolean hasAllZerosAddress() {
        if (ipAddress == null) return true;
        for (byte b : ipAddress) {
            if (b != 0) return false;
        }
        return true;
    }
}
