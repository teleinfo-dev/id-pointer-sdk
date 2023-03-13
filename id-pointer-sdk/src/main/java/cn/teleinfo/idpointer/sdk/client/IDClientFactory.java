package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.config.IDClientConfig;
import cn.teleinfo.idpointer.sdk.core.AuthenticationInfo;
import cn.teleinfo.idpointer.sdk.core.HandleException;
import cn.teleinfo.idpointer.sdk.core.PublicKeyAuthenticationInfo;
import cn.teleinfo.idpointer.sdk.core.Util;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.transport.ChannelPoolMapManager;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.PrivateKey;

/**
 * IDClientFactory
 */
public class IDClientFactory {

    private final ChannelPoolMapManager channelPoolMapManager;
    private final IDClientConfig idClientConfig;

    private IDResolver idResolver;

    public IDClientFactory(ChannelPoolMapManager channelPoolMapManager, IDClientConfig idClientConfig) {
        this.channelPoolMapManager = channelPoolMapManager;
        this.idClientConfig = idClientConfig;
    }

    public IDClientConfig getIdClientConfig() {
        return idClientConfig;
    }


    /**
     * @return IDResolver
     */
    public IDResolver getIdResolver() {
        if (idResolver == null) {
            synchronized (this) {
                if (idResolver == null) {
                    idResolver = new DefaultIdResolver(this);
                }
            }
        }
        return idResolver;
    }

    /**
     * @param serverAddress
     * @param adminUserId
     * @param adminUserIndex
     * @param privateKey
     * @return
     * @throws HandleException
     * @throws UnsupportedEncodingException
     * @throws IDException
     */
    public IDClient newInstance(InetSocketAddress serverAddress, String adminUserId, int adminUserIndex, PrivateKey privateKey) {
        AuthenticationInfo authenticationInfo = new PublicKeyAuthenticationInfo(Util.encodeString(adminUserId), adminUserIndex, privateKey);
        return newInstance(serverAddress, authenticationInfo);
    }

    /**
     * @param serverAddress
     * @param authenticationInfo
     * @return
     * @throws HandleException
     * @throws UnsupportedEncodingException
     * @throws IDException
     */
    public IDClient newInstance(InetSocketAddress serverAddress, AuthenticationInfo authenticationInfo) {
        DefaultIdClient defaultIdClient = new DefaultIdClient(serverAddress, channelPoolMapManager, authenticationInfo);
        return defaultIdClient;
    }

    public IDClient newInstance(InetSocketAddress serverAddress, AuthenticationInfo authenticationInfo, boolean encrypt) {
        DefaultIdClient defaultIdClient = new DefaultIdClient(serverAddress, channelPoolMapManager, authenticationInfo, encrypt);
        return defaultIdClient;
    }

    /**
     * @param serverAddress
     * @return
     */
    public IDClient newInstance(InetSocketAddress serverAddress) {
        return new DefaultIdClient(serverAddress, channelPoolMapManager);
    }

    /**
     * @param prefix
     * @return
     * @throws IDException
     */
    public IDClient newInstance(String prefix) throws IDException {
        InetSocketAddress serverAddress = GlobalIdClientFactory.getPrefixTcpInetSocketAddress(prefix);
        return new DefaultIdClient(serverAddress, channelPoolMapManager);
    }

    /**
     * @param prefix
     * @param authenticationInfo
     * @return
     * @throws IDException
     */
    public IDClient newInstance(String prefix, AuthenticationInfo authenticationInfo) throws IDException {
        InetSocketAddress serverAddress = GlobalIdClientFactory.getPrefixTcpInetSocketAddress(prefix);
        DefaultIdClient idClient = new DefaultIdClient(serverAddress, channelPoolMapManager, authenticationInfo);
        return idClient;
    }

    /**
     * @param prefix
     * @param adminUserId
     * @param adminUserIndex
     * @param privateKey
     * @return
     * @throws IDException
     * @throws HandleException
     * @throws UnsupportedEncodingException
     */
    public IDClient newInstance(String prefix, String adminUserId, int adminUserIndex, PrivateKey privateKey) throws IDException {
        InetSocketAddress serverAddress = GlobalIdClientFactory.getPrefixTcpInetSocketAddress(prefix);
        AuthenticationInfo authenticationInfo = new PublicKeyAuthenticationInfo(Util.encodeString(adminUserId), adminUserIndex, privateKey);
        DefaultIdClient idClient = new DefaultIdClient(serverAddress, channelPoolMapManager, authenticationInfo);
        return idClient;
    }


}
