/**
 * Copyright (C) 2024 teleinfo caict
 * All rights reserved.
 * <p>
 * 版权所有（C）2024 teleinfo caict
 */
package cn.teleinfo.idpointer.sdk.transport.v3;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;

/**
 * @Author: abluepoint
 * @Email: lilong@teleinfo.cn
 * @Create: 2024/12/12
 * @Description: ResponsePromise -
 */
public interface IdPromise<V> extends Promise<V> {

    public Channel getChannel();

    void release();
}
