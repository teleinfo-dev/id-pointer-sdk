package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.core.AbstractIdRequest;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * @author bluepoint
 * @since 2021-08-25
 * 提高清理promiseMap的可靠性
 * 1. 当前是通过promise监听器清理promiseMap
 * 2. 连接断开后,绑定到连接的promise失败
 */
public class MessageManagerImpl implements MessageManager {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MessageManagerImpl.class);
    private final ConcurrentHashMap<Integer, ResponsePromise> promiseMap;

    private final EventLoopGroup eventLoopGroup;

    private final long promiseTimeoutMs;

    /**
     * 秒
     *
     * @param promiseTimeout
     */
    public MessageManagerImpl(int promiseTimeout) {
        this(new NioEventLoopGroup(), promiseTimeout);
    }

    public MessageManagerImpl(EventLoopGroup eventLoopGroup, int promiseTimeout) {
        this.promiseMap = new ConcurrentHashMap<>(5000);
        this.eventLoopGroup = eventLoopGroup;
        this.promiseTimeoutMs = (promiseTimeout + 1) * 1000;
        Thread cleanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    long current = System.currentTimeMillis();
                    Iterator<ResponsePromise> iterator = promiseMap.values().iterator();
                    ResponsePromise promise = null;

                    while (iterator.hasNext()) {
                        promise = iterator.next();
                        if (promise.getSendTimestamp() != 0 && current - promise.getSendTimestamp() > promiseTimeoutMs) {
                            log.debug("promiseTimeoutMs {},getSendTimestamp {},current {}", promiseTimeoutMs, promise.getSendTimestamp(), current);
                            log.info("promise timeout {} by daemon,requestId {},sendTimestamp: {}",promiseTimeoutMs ,promise.getRequestId(),promise.getSendTimestamp());
                            promise.setFailure(new TimeoutException("promise timeout by daemon"));
                        }
                    }

                    // 如果打断不再执行
                    try {
                        Thread.sleep(1000l);
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                    }

                }
            }
        }, "promise-clean-1");
        cleanThread.setDaemon(true);
        cleanThread.start();
    }


    @Override
    public ResponsePromise createResponsePromise(Integer requestId) {
        ResponsePromise responsePromise = new ResponsePromise(eventLoopGroup.next(), requestId);
        // 添监听器
        responsePromise.addListener(future -> {
            if (future.isDone()) {
                log.debug("clean request id: {}", requestId);
                promiseMap.remove(requestId);
                responsePromise.clear();
            }
        });

        ResponsePromise preValue = null;
        do {
            preValue = promiseMap.putIfAbsent(requestId, responsePromise);
            try {
                Thread.sleep(1L);
            } catch (InterruptedException e) {
            }
        } while (preValue != null);

        return responsePromise;
    }

    @Override
    public ResponsePromise getResponsePromise(Integer requestId) {
        return promiseMap.get(requestId);
    }

    /**
     * 保证非io线程执行
     * @param request
     * @param channel
     * @return
     * @throws IDException
     */
    public ResponsePromise process(AbstractIdRequest request, Channel channel) throws IDException {
        ResponsePromise responsePromise = createResponsePromise(request.requestId);
        String handle = new String(request.handle);

        responsePromise.setHandle(handle);
        responsePromise.setOpCode(request.opCode);

        if(!channel.isWritable()){
            throw new IDException(IDException.CHANNEL_GET_ERROR,"not writable");
        }

        ChannelFuture writeFuture = channel.writeAndFlush(request);
        responsePromise.bindWriteFuture(writeFuture);

        return responsePromise;
    }


    @Override
    public void close() throws IOException {
        eventLoopGroup.shutdownGracefully();
    }
}
