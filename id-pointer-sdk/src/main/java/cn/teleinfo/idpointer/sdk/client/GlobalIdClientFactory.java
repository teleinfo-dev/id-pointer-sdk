package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.config.IDClientConfig;
import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.exception.IDRuntimeException;
import cn.teleinfo.idpointer.sdk.transport.ChannelPoolConfig;
import cn.teleinfo.idpointer.sdk.transport.ChannelPoolMapManager;
import cn.teleinfo.idpointer.sdk.util.SiteUtils;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * 全局的IdClientFactory
 * 提供全局的IDResolver与IDClientFactory
 */
public class GlobalIdClientFactory {

    private static GlobalIdClientFactory instance;
    private static IDClientConfig DEFAULT_CONFIG = IDClientConfig.builder().prdEnv().build();

    private final IDResolver idResolver;
    private IDClientConfig idClientConfig;
    private ChannelPoolMapManager globalChannelPoolMapManager;
    private IDClientFactory idClientFactory;

    private GlobalIdClientFactory(IDClientConfig idClientConfig) {
        this.idClientConfig = idClientConfig;

        ChannelPoolConfig poolConfig = ChannelPoolConfig.builder()
                .idleTimeSeconds(idClientConfig.getIdleTimeSeconds())
                .heatBeatRunning(idClientConfig.isHeatBeatRunning())
                .minConnectionsPerServer(idClientConfig.getMinConnectionsPerServer())
                .maxConnectionsPerServer(idClientConfig.getMaxConnectionsPerServer())
                .build();

        ChannelPoolConfig loginPoolConfig = ChannelPoolConfig.builder()
                .idleTimeSeconds(idClientConfig.getLoginIdleTimeSeconds())
                .heatBeatRunning(idClientConfig.isLoginHeatBeatRunning())
                .minConnectionsPerServer(idClientConfig.getLoginMinConnectionsPerServer())
                .maxConnectionsPerServer(idClientConfig.getLoginMaxConnectionsPerServer())
                .build();

        this.globalChannelPoolMapManager = new ChannelPoolMapManager(idClientConfig.getNioThreads(), idClientConfig.getPromiseTimeout(), poolConfig, loginPoolConfig);
        this.idClientFactory = new IDClientFactory(globalChannelPoolMapManager, idClientConfig);
        this.idResolver = new DefaultIdResolver(this.idClientFactory);
    }

    /**
     * 初始化GlobalIdClientFactory使用的idClientConfig
     * @param idClientConfig
     */
    public static void init(IDClientConfig idClientConfig) {
        if (instance != null) {
            throw new IDRuntimeException(IDException.INTERNAL_ERROR);
        }
        DEFAULT_CONFIG = idClientConfig;
    }

    public static GlobalIdClientFactory getInstance() {
        if (instance == null) {
            synchronized (GlobalIdClientFactory.class) {
                if (instance == null) {
                    instance = new GlobalIdClientFactory(DEFAULT_CONFIG);
                }
            }
        }
        return instance;
    }

    public static ChannelPoolMapManager getGlobalChannelPoolMapManager() {
        return getInstance().globalChannelPoolMapManager;
    }

    public static IDClientConfig getIdClientConfig() {
        return getInstance().idClientConfig;
    }

    public static IDResolver getIdResolver() {
        return getInstance().idResolver;
    }

    public HandleValue[] getPrefixHandleValues(String prefix,String[] types, int[] indexes) throws IDException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(idClientConfig.getRecursionServerIp(), idClientConfig.getRecursionServerPort());
        try (IDClient idClient = getIdClientFactory().newInstance(inetSocketAddress);) {
            HandleValue[] hvs = idClient.resolveHandle(prefix, types, indexes);
            return hvs;
        } catch (IOException e) {
            throw new IDException(0, "close error", e);
        }
    }

    public static InetSocketAddress getPrefixTcpInetSocketAddress(String prefix) throws IDException {
        HandleValue[] hvs = getInstance().getPrefixHandleValues(prefix,new String[]{Common.STR_SITE_INFO_TYPE}, null);
        SiteInfo[] siteInfos = Util.getSitesFromValues(hvs);
        if (siteInfos.length == 0) {
            throw new IDException(0, "can not resolve prefix");
        }
        SiteInfo siteInfo = siteInfos[0];
        InetSocketAddress address = SiteUtils.getFirstInetSocketAddressByProtocol(siteInfo, "TCP");
        return address;
    }


    /**
     * 获取全局的IDClientFactory
     * @return
     */
    public static IDClientFactory getIdClientFactory() {
        return getInstance().idClientFactory;
    }
}
