package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.core.AbstractResponse;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;

import java.util.concurrent.TimeoutException;

public class ResponsePromise extends DefaultPromise<AbstractResponse> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ResponsePromise.class);
    private int requestId;
    private boolean isBind = false;
    private long beginTimestamp;
    private long sendTimestamp = 0;

    private final GenericFutureListener<? extends Future<? super Void>> closeListener;

    private final GenericFutureListener<? extends Future<? super Void>> writeListener;

    private String handle;
    private int opCode;

    public ResponsePromise(EventExecutor executor, int requestId) {
        super(executor);
        this.requestId = requestId;
        this.beginTimestamp = System.currentTimeMillis();
        this.writeListener = future -> {
            if (future.isDone()) {
                //Channel channel = channelFuture.channel();
                if (future.isSuccess()) {
                    this.setSendTimestamp(System.currentTimeMillis());
                    //log.info("{} send handle {}, requestId {}", channel.localAddress(), handle, requestId);
                } else {
                    //log.info("{} send fail handle {}, requestId {}", channel.localAddress(), handle, requestId);
                    this.setFailure(new IDException(IDException.PROMISE_GET_ERROR, "send error",future.cause()));
                }
            }
        };
        this.closeListener = future -> {
            if (future.isDone()) {
                if (!this.isDone()) {
                    this.setFailure(new TimeoutException("promise channel reset by daemon,"));
                }
            }
        };
    }


    private ChannelFuture channelFuture;

    public void bindWriteFuture(ChannelFuture channelFuture) {
        if (isBind) {
            throw new IllegalStateException("channelFuture already bind");
        }
        this.isBind = true;

        this.channelFuture = channelFuture;

        this.channelFuture.channel().closeFuture().addListener(this.closeListener);

        this.channelFuture.addListener(this.writeListener);
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public int getOpCode() {
        return opCode;
    }

    public void setOpCode(int opCode) {
        this.opCode = opCode;
    }

    public void clear() {
        if (channelFuture != null) {
            channelFuture.channel().closeFuture().removeListener(closeListener);
            channelFuture.removeListener(this.writeListener);
        }
        this.channelFuture = null;
    }

    public int getRequestId() {
        return requestId;
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    void setSendTimestamp(long sendTimestamp) {
        this.sendTimestamp = sendTimestamp;
    }

    public long getSendTimestamp() {
        return sendTimestamp;
    }

}
