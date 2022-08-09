package cn.teleinfo.idpointer.sdk.util;

import cn.teleinfo.idpointer.sdk.core.Interface;
import cn.teleinfo.idpointer.sdk.core.ServerInfo;
import cn.teleinfo.idpointer.sdk.core.SiteInfo;

import java.net.InetSocketAddress;

public abstract class SiteUtils {
    public static InetSocketAddress getFirstInetSocketAddressByProtocol(SiteInfo siteInfo, String protocolName) {
        for (ServerInfo serverInfo : siteInfo.servers) {
            for (Interface s : serverInfo.interfaces) {
                if (protocolName.equals(Interface.protocolName(s.protocol))) {
                    return new InetSocketAddress(serverInfo.getInetAddress(), s.port);
                }
            }
        }
        return null;
    }
}
