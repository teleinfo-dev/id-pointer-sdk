package cn.teleinfo.idpointer.sdk.client.v3;

import java.net.InetSocketAddress;

public interface IdClientFactory {

    IdClient createTcpClient(InetSocketAddress serverAddress, int promiseTimeout, int maxConnections);
    IdClient createUdpClient(InetSocketAddress serverAddress, int promiseTimeout);

}
