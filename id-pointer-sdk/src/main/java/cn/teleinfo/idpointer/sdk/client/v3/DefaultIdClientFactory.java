/**
 * Copyright (C) 2024 teleinfo caict
 * All rights reserved.
 * <p>
 * 版权所有（C）2024 teleinfo caict
 */
package cn.teleinfo.idpointer.sdk.client.v3;

import java.net.InetSocketAddress;

/**
 * @Author: abluepoint
 * @Email: lilong@teleinfo.cn
 * @Create: 2024/12/12
 * @Description: DefaultIdClientFactory -
 */
public class DefaultIdClientFactory implements IdClientFactory {

    @Override
    public IdClient createTcpClient(InetSocketAddress serverAddress, int promiseTimeout, int maxConnections) {
        return new IdTcpClient(serverAddress, promiseTimeout, maxConnections);
    }

    @Override
    public IdClient createUdpClient(InetSocketAddress serverAddress, int promiseTimeout) {
        return null;
    }

}
