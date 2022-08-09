package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.core.AbstractRequest;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import io.netty.channel.Channel;

import java.io.Closeable;

/**
 * 消息管理
 * 1. 为请求创建promise对象
 * 2. 通过channel发送请求数据到服务端
 */
public interface MessageManager extends Closeable {

    ResponsePromise createResponsePromise(Integer requestId);

    ResponsePromise getResponsePromise(Integer requestId);

    ResponsePromise process(AbstractRequest request, Channel channel) throws IDException;

}
