package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.transport.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultIdClient extends AbstractIdClient {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DefaultIdClient.class);
    private final TransportOnTcp transportOnTcp;
    private final TransportOnTcpLogin transportOnTcpLogin;
    private final RequestIdFactory requestIdGenerate;
    private final ChannelPoolMapManager channelPoolMapManager;


    public DefaultIdClient(InetSocketAddress serverAddress, ChannelPoolMapManager channelPoolMapManager) {
        super(serverAddress,60,null,null,false);
        this.channelPoolMapManager = channelPoolMapManager;
        this.transportOnTcp = new TransportOnTcp(channelPoolMapManager.getChannelPoolMap(), channelPoolMapManager.getMessageManager());
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();
        this.transportOnTcpLogin = new TransportOnTcpLogin(channelPoolMapManager.getLoginChannelPoolMap(), channelPoolMapManager.getMessageManager(), requestIdGenerate);
    }

    public DefaultIdClient(InetSocketAddress serverAddress, ChannelPoolMapManager channelPoolMapManager, AuthenticationInfo authenticationInfo, boolean encrypt) {
        super(serverAddress,60,authenticationInfo,new LoginInfoPoolKey(serverAddress, new IdUserId(Util.decodeString(authenticationInfo.getUserIdHandle()), authenticationInfo.getUserIdIndex())), encrypt);
        this.channelPoolMapManager = channelPoolMapManager;
        this.transportOnTcp = new TransportOnTcp(channelPoolMapManager.getChannelPoolMap(), channelPoolMapManager.getMessageManager());
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();
        this.transportOnTcpLogin = new TransportOnTcpLogin(channelPoolMapManager.getLoginChannelPoolMap(), channelPoolMapManager.getMessageManager(), requestIdGenerate);
    }

    /**
     * 默认不开加密传输
     *
     * @param serverAddress
     * @param channelPoolMapManager
     * @param authenticationInfo
     */
    public DefaultIdClient(InetSocketAddress serverAddress, ChannelPoolMapManager channelPoolMapManager, AuthenticationInfo authenticationInfo) {
        this(serverAddress, channelPoolMapManager, authenticationInfo, false);
    }

    protected AbstractResponse doRequest(AbstractRequest request) throws IDException {
        AbstractResponse response = null;
        ResponsePromise responsePromise = doRequestInternal(request);
        try {
            response = responsePromise.get(getPromiseTimeout(), TimeUnit.SECONDS);
            if (response.responseCode != AbstractMessage.RC_SUCCESS && response.responseCode != AbstractMessage.RC_AUTHENTICATION_NEEDED) {
                throw new IDException("", response);
            }
            return response;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (responsePromise != null && !responsePromise.isDone()) {
                responsePromise.setFailure(e);
            }
            log.warn("promise get error,handle {},request id is {},", Util.decodeString(request.handle), request.requestId);
            throw new IDException(IDException.PROMISE_GET_ERROR, "promise get error", e);
        }
    }

    /**
     * @param request
     * @return
     * @throws IDException
     */
    protected ResponsePromise doRequestInternal(AbstractRequest request) throws IDException {
        request.requestId = requestIdGenerate.getNextInteger();
        request.encrypt = isEncrypt();
        if (getLoginInfoPoolKey() == null) {
            ResponsePromise responsePromise = null;
            try {
                responsePromise = transportOnTcp.process(request, getServerAddress());
            } catch (IDException e) {
                throw e;
            } catch (Exception e) {
                if (responsePromise != null && !responsePromise.isDone()) {
                    responsePromise.setFailure(e);
                }
                log.warn("process error,request id is {}", request.requestId, e);
                throw new IDException(IDException.PROMISE_GET_ERROR, "process error", e);
            }
            return responsePromise;
        } else {
            ResponsePromise responsePromise = null;
            try {
                responsePromise = transportOnTcpLogin.process(request, getLoginInfoPoolKey(), getAuthenticationInfo());
            } catch (IDException e) {
                throw e;
            } catch (Exception e) {
                if (responsePromise != null && !responsePromise.isDone()) {
                    responsePromise.setFailure(e);
                }
                log.warn("process error,request id is {}", request.requestId, e);
                throw new IDException(IDException.PROMISE_GET_ERROR, "process error", e);
            }
            return responsePromise;
        }
    }


    @Override
    public void close() throws IOException {
        if (getLoginInfoPoolKey() != null) {
            channelPoolMapManager.getLoginChannelPoolMap().remove(getLoginInfoPoolKey());
        } else {
            channelPoolMapManager.getChannelPoolMap().remove(getServerAddress());
        }
    }
}
