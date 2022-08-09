package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.core.ResolutionRequest;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class HeartBeatHandler extends ChannelDuplexHandler {

    private final MessageManager messageManager;
    private final RequestIdFactory requestIdGenerate = RequestIdFactoryDefault.getInstance();
    private final ExecutorService executorService = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(200), new ThreadPoolExecutor.DiscardPolicy());


    public HeartBeatHandler(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        ResolutionRequest request = new ResolutionRequest("0".getBytes(StandardCharsets.UTF_8), null, null, null);
                        request.requestId = requestIdGenerate.getNextInteger();
                        messageManager.process(request, ctx.channel());
                        ctx.fireChannelActive();
                    } catch (IDException e) {
                        int code = e.getCode();
                        if (code == IDException.CHANNEL_GET_ERROR || code == IDException.PROMISE_GET_ERROR || code == IDException.CLIENT_ERROR) {
                            ctx.fireChannelInactive();
                        }
                    }
                }
            });
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
