package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.config.IDClientConfig;
import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.transport.ChannelPoolMapManager;
import cn.teleinfo.idpointer.sdk.util.SiteUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.PrivateKey;

public class GlobalIdClientFactory {

    private static ChannelPoolMapManager globalChannelPoolMapManager;
    private static final IDClientConfig ID_CLIENT_CONFIG = IDClientConfig.builder().prdEnv().build();
    private static IDResolver idResolver = new GlobalIdResolver();
    private static IDClientFactory clientFactory = new IDClientFactory(GlobalIdClientFactory.getGlobalChannelPoolMapManager(),ID_CLIENT_CONFIG);

    public static ChannelPoolMapManager getGlobalChannelPoolMapManager() {
        if (globalChannelPoolMapManager == null) {
            synchronized (IDClientFactory.class) {
                if (globalChannelPoolMapManager == null) {
                    globalChannelPoolMapManager = new ChannelPoolMapManager(ID_CLIENT_CONFIG.getNioThreads(), ID_CLIENT_CONFIG.getMinConnectionsPerServer(), ID_CLIENT_CONFIG.getMaxConnectionsPerServer());
                }
            }
        }
        return globalChannelPoolMapManager;
    }

    public static IDClientConfig getIdClientConfig() {
        return ID_CLIENT_CONFIG;
    }

    public static IDResolver getIdResolver() {
        return idResolver;
    }

    public static InetSocketAddress getPrefixTcpInetSocketAddress(String prefix) throws IDException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(ID_CLIENT_CONFIG.getRecursionServerIp(), ID_CLIENT_CONFIG.getRecursionServerPort());
        try (IDClient idClient = getClientFactory().newInstance(inetSocketAddress);) {
            HandleValue[] hvs = idClient.resolveHandle(prefix, new String[]{Common.STR_SITE_INFO_TYPE}, null);
            SiteInfo[] siteInfos = Util.getSitesFromValues(hvs);
            if (siteInfos.length == 0) {
                throw new IDException(0, "can not resolve prefix");
            }
            SiteInfo siteInfo = siteInfos[0];
            InetSocketAddress address = SiteUtils.getFirstInetSocketAddressByProtocol(siteInfo, "TCP");
            return address;
        } catch (IOException e) {
            throw new IDException(0, "close error", e);
        }
    }

    public static IDClientFactory getClientFactory() {
        return clientFactory;
    }
}
