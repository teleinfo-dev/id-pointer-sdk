package cn.teleinfo.idpointer.sdk.transport;

import io.netty.channel.pool.ChannelPool;

public interface TimedChannelPool extends ChannelPool {

    long getLastActiveTime();

    void setLastActiveTime(long lastActiveTime);

    int acquiredChannelCount();
}
