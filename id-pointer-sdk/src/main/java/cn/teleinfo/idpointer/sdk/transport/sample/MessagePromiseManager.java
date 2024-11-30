package cn.teleinfo.idpointer.sdk.transport.sample;

import cn.teleinfo.idpointer.sdk.core.AbstractResponse;
import io.netty.channel.EventLoop;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Promise;

public interface MessagePromiseManager {

    public static AttributeKey<Promise<AbstractResponse>> PROMISE_ATTRIBUTE_KEY = AttributeKey.valueOf("promise");

    Promise<AbstractResponse> createPromise(int requestId, EventLoop eventLoop);

    Promise<AbstractResponse> getPromiseAndRemove(int requestId);

}
