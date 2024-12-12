package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.AuthenticationInfo;

import java.net.InetSocketAddress;

public class DefaultIdClient extends SampleIdClient {

    public DefaultIdClient(InetSocketAddress serverAddress, int promiseTimeout) {
        super(serverAddress, promiseTimeout);
    }

    public DefaultIdClient(InetSocketAddress serverAddress, int promiseTimeout, int maxConnections) {
        super(serverAddress, promiseTimeout, maxConnections);
    }

    public DefaultIdClient(InetSocketAddress serverAddress, int promiseTimeout, int maxConnections, AuthenticationInfo authenticationInfo, boolean encrypt) {
        super(serverAddress, promiseTimeout, maxConnections, authenticationInfo, encrypt);
    }
}
