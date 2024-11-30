package cn.teleinfo.idpointer.sdk.transport.sample;

import cn.hutool.core.map.WeakConcurrentMap;
import cn.teleinfo.idpointer.sdk.core.AbstractResponse;
import cn.teleinfo.idpointer.sdk.exception.IDRuntimeException;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.ConcurrentHashMap;

import static cn.teleinfo.idpointer.sdk.exception.IDRuntimeException.CLIENT_ERROR;

public class MessagePromiseManagerImpl implements MessagePromiseManager {

    private final ConcurrentHashMap<Integer, Promise<AbstractResponse>> promiseMap;

    public MessagePromiseManagerImpl() {
        this.promiseMap = new ConcurrentHashMap<>();
    }

    @Override
    public Promise<AbstractResponse> createPromise(int requestId, EventLoop eventLoop) {
        Promise<AbstractResponse> promise = eventLoop.newPromise();

        Promise<AbstractResponse> preValue = promiseMap.putIfAbsent(requestId, promise);

        if (preValue != null) {
            throw new IDRuntimeException(CLIENT_ERROR,"requestId already exists");
        }

        return promise;
    }

    @Override
    public Promise<AbstractResponse> getPromiseAndRemove(int requestId) {
        return promiseMap.remove(requestId);
    }
}
