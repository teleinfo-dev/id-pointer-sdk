package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.transport.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultIdClient implements IDClient {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DefaultIdClient.class);
    private final TransportOnTcp transportOnTcp;
    private final TransportOnTcpLogin transportOnTcpLogin;
    private final InetSocketAddress serverAddress;
    private final RequestIdFactory requestIdGenerate;
    private final int promiseTimeout;
    private final ChannelPoolMapManager channelPoolMapManager;

    /**
     * 传进来的身份认证信息
     */
    private final AuthenticationInfo authenticationInfo;

    /**
     * DefaultIdClient内部使用,连接池的key
     */
    private final LoginInfoPoolKey loginInfoPoolKey;

    private final boolean encrypt;

    public DefaultIdClient(InetSocketAddress serverAddress, ChannelPoolMapManager channelPoolMapManager) {
        this.channelPoolMapManager = channelPoolMapManager;
        this.transportOnTcp = new TransportOnTcp(channelPoolMapManager.getChannelPoolMap(), channelPoolMapManager.getMessageManager());
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();
        this.transportOnTcpLogin = new TransportOnTcpLogin(channelPoolMapManager.getLoginChannelPoolMap(), channelPoolMapManager.getMessageManager(), requestIdGenerate);
        this.serverAddress = serverAddress;
        this.promiseTimeout = 60;
        this.authenticationInfo = null;
        this.loginInfoPoolKey = null;
        this.encrypt = false;
    }

    public DefaultIdClient(InetSocketAddress serverAddress, ChannelPoolMapManager channelPoolMapManager, AuthenticationInfo authenticationInfo, boolean encrypt) {
        this.channelPoolMapManager = channelPoolMapManager;
        this.transportOnTcp = new TransportOnTcp(channelPoolMapManager.getChannelPoolMap(), channelPoolMapManager.getMessageManager());
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();
        this.transportOnTcpLogin = new TransportOnTcpLogin(channelPoolMapManager.getLoginChannelPoolMap(), channelPoolMapManager.getMessageManager(), requestIdGenerate);
        this.serverAddress = serverAddress;
        this.promiseTimeout = 60;
        this.authenticationInfo = authenticationInfo;
        this.loginInfoPoolKey = new LoginInfoPoolKey(serverAddress, new IdUserId(Util.decodeString(authenticationInfo.getUserIdHandle()), authenticationInfo.getUserIdIndex()));
        this.encrypt = encrypt;
    }

    /**
     * 默认不开加密传输
     * @param serverAddress
     * @param channelPoolMapManager
     * @param authenticationInfo
     */
    public DefaultIdClient(InetSocketAddress serverAddress, ChannelPoolMapManager channelPoolMapManager, AuthenticationInfo authenticationInfo) {
        this(serverAddress,channelPoolMapManager,authenticationInfo,false);
    }

    @Override
    public void addHandleValues(String handle, HandleValue[] values) throws IDException {
        addHandleValues(handle, values, false);
    }

    @Override
    public void addHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        AddValueRequest request = new AddValueRequest(Util.encodeString(handle), values, null);
        request.overwriteWhenExists = overwrite;
        doRequest(request);
    }

    @Override
    public void createHandle(String handle, HandleValue[] values) throws IDException {
        createHandle(handle, values, false);
    }

    @Override
    public void createHandle(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        CreateHandleRequest request = new CreateHandleRequest(Util.encodeString(handle), values, null);
        request.overwriteWhenExists = overwrite;
        doRequest(request);
    }

    @Override
    public void deleteHandle(String handle) throws IDException {
        DeleteHandleRequest request = new DeleteHandleRequest(handle.getBytes(StandardCharsets.UTF_8), null);
        doRequest(request);
    }

    @Override
    public void deleteHandleValues(String handle, HandleValue[] values) throws IDException {
        int[] indexes = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            indexes[i] = values[i].getIndex();
        }
        RemoveValueRequest request = new RemoveValueRequest(handle.getBytes(StandardCharsets.UTF_8), indexes, null);
        doRequest(request);
    }

    @Override
    public void deleteHandleValues(String handle, int[] indexes) throws IDException {
        RemoveValueRequest request = new RemoveValueRequest(handle.getBytes(StandardCharsets.UTF_8), indexes, null);
        doRequest(request);
    }

    private AbstractResponse doRequest(AbstractRequest request) throws IDException {
        AbstractResponse response = null;
        ResponsePromise responsePromise = doRequestInternal(request);
        try {
            response = responsePromise.get(promiseTimeout, TimeUnit.SECONDS);
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
    private ResponsePromise doRequestInternal(AbstractRequest request) throws IDException {
        request.requestId = requestIdGenerate.getNextInteger();
        request.encrypt = encrypt;
        if (loginInfoPoolKey == null) {
            ResponsePromise responsePromise = null;
            try {
                responsePromise = transportOnTcp.process(request, serverAddress);
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
                responsePromise = transportOnTcpLogin.process(request, loginInfoPoolKey, authenticationInfo);
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
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes, boolean auth) throws IDException {
        if (auth) {
            if (loginInfoPoolKey == null) {
                throw new IDException(0, "not auth");
            }
        }
        byte[][] reqTypes = getTypeStringBytes(types);

        ResolutionRequest request = new ResolutionRequest(handle.getBytes(StandardCharsets.UTF_8), reqTypes, indexes, null);
        AbstractResponse response = doRequest(request);

        HandleValue[] hvs = null;
        if (response instanceof ResolutionResponse) {
            try {
                hvs = ((ResolutionResponse) response).getHandleValues();
                return hvs;
            } catch (HandleException e) {
                throw new IDException(e, response);
            }
        } else {
            throw new IDException("not resolution response", response);
        }
    }

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes, String authString) throws IDException {

        byte[][] reqTypes = getTypeStringBytes(types);

        ResolutionRequest request = new ResolutionRequest(handle.getBytes(StandardCharsets.UTF_8), reqTypes, indexes, null, authString);
        request.recursionAuth = true;
        AbstractResponse response = doRequest(request);

        HandleValue[] hvs = null;
        if (response instanceof ResolutionResponse) {
            try {
                hvs = ((ResolutionResponse) response).getHandleValues();
                return hvs;
            } catch (HandleException e) {
                throw new IDException(e, response);
            }
        } else {
            throw new IDException("not resolution response", response);
        }
    }

    private static byte[][] getTypeStringBytes(String[] types) {
        byte[][] reqTypes = null;
        if (types != null) {
            reqTypes = new byte[types.length][];
            for (int i = 0; i < types.length; i++) {
                reqTypes[i] = types[i].getBytes(StandardCharsets.UTF_8);
            }
        }
        return reqTypes;
    }

    @Override
    public HandleValue[] resolveHandle(String handle) throws IDException {
        return resolveHandle(handle, null, null);
    }

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes) throws IDException {
        byte reqTypes[][] = null;
        if (types != null) {
            reqTypes = new byte[types.length][];
            for (int i = 0; i < types.length; i++) {
                reqTypes[i] = types[i].getBytes(StandardCharsets.UTF_8);
            }
        }
        ResolutionRequest request = new ResolutionRequest(handle.getBytes(StandardCharsets.UTF_8), reqTypes, indexes, null);
        AbstractResponse response = doRequest(request);

        HandleValue[] hvs = null;
        if (response instanceof ResolutionResponse) {
            try {
                hvs = ((ResolutionResponse) response).getHandleValues();
                return hvs;
            } catch (HandleException e) {
                throw new IDException(e, response);
            }
        } else {
            throw new IDException("not resolution response", response);
        }
    }

    @Override
    public void updateHandleValues(String handle, HandleValue[] values) throws IDException {
        updateHandleValues(handle, values, false);
    }

    @Override
    public void updateHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        ModifyValueRequest request = new ModifyValueRequest(handle.getBytes(StandardCharsets.UTF_8), values, null);
        request.overwriteWhenExists = overwrite;
        doRequest(request);
    }

    @Override
    public void homeNa(String na) throws IDException {
        HomeNaRequest request = new HomeNaRequest(na.getBytes(StandardCharsets.UTF_8), null);
        doRequest(request);
    }

    @Override
    public void unhomeNa(String na) throws IDException {
        UnhomeNaRequest request = new UnhomeNaRequest(na.getBytes(StandardCharsets.UTF_8), null);
        doRequest(request);
    }

    @Override
    public ResponsePromise addHandleValuesAsync(String handle, HandleValue[] values) throws IDException {
        return addHandleValuesAsync(handle, values, false);
    }

    @Override
    public ResponsePromise addHandleValuesAsync(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        AddValueRequest request = new AddValueRequest(handle.getBytes(StandardCharsets.UTF_8), values, null);
        request.overwriteWhenExists = overwrite;
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }

    @Override
    public ResponsePromise createHandleAsync(String handle, HandleValue[] values) throws IDException {
        return createHandleAsync(handle, values, false);
    }

    @Override
    public ResponsePromise createHandleAsync(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        CreateHandleRequest request = new CreateHandleRequest(handle.getBytes(StandardCharsets.UTF_8), values, null);
        request.overwriteWhenExists = overwrite;
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }

    @Override
    public ResponsePromise deleteHandleAsync(String handle) throws IDException {
        DeleteHandleRequest request = new DeleteHandleRequest(handle.getBytes(StandardCharsets.UTF_8), null);
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }

    @Override
    public ResponsePromise deleteHandleValuesAsync(String handle, HandleValue[] values) throws IDException {
        int[] indexes = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            indexes[i] = values[i].getIndex();
        }
        RemoveValueRequest request = new RemoveValueRequest(handle.getBytes(StandardCharsets.UTF_8), indexes, null);
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }

    @Override
    public ResponsePromise deleteHandleValuesAsync(String handle, int[] indexes) throws IDException {
        RemoveValueRequest request = new RemoveValueRequest(handle.getBytes(StandardCharsets.UTF_8), indexes, null);
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }

    @Override
    public ResponsePromise resolveHandleAsync(String handle, String[] types, int[] indexes, boolean auth) throws IDException {
        if (auth) {
            if (loginInfoPoolKey == null) {
                throw new IDException(0, "not auth");
            }
        }
        byte reqTypes[][] = null;
        if (types != null) {
            reqTypes = new byte[types.length][];
            for (int i = 0; i < types.length; i++) {
                reqTypes[i] = types[i].getBytes(StandardCharsets.UTF_8);
            }
        }
        ResolutionRequest request = new ResolutionRequest(handle.getBytes(StandardCharsets.UTF_8), reqTypes, indexes, null);
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }

    @Override
    public ResponsePromise resolveHandleAsync(String handle, String[] types, int[] indexes) throws IDException {
        byte reqTypes[][] = null;
        if (types != null) {
            reqTypes = new byte[types.length][];
            for (int i = 0; i < types.length; i++) {
                reqTypes[i] = types[i].getBytes(StandardCharsets.UTF_8);
            }
        }
        ResolutionRequest request = new ResolutionRequest(handle.getBytes(StandardCharsets.UTF_8), reqTypes, indexes, null);
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }

    @Override
    public ResponsePromise updateHandleValuesAsync(String handle, HandleValue[] values) throws IDException {
        ModifyValueRequest request = new ModifyValueRequest(handle.getBytes(StandardCharsets.UTF_8), values, null);
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }

    @Override
    public ResponsePromise updateHandleValuesAsync(String handle, HandleValue[] values, boolean overwrite) throws IDException {
        ModifyValueRequest request = new ModifyValueRequest(handle.getBytes(StandardCharsets.UTF_8), values, null);
        request.overwriteWhenExists = overwrite;
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }

    @Override
    public ResponsePromise homeNaAsync(String na) throws IDException {
        HomeNaRequest request = new HomeNaRequest(na.getBytes(StandardCharsets.UTF_8), null);
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }

    @Override
    public ResponsePromise unhomeNaAsync(String na) throws IDException {
        UnhomeNaRequest request = new UnhomeNaRequest(na.getBytes(StandardCharsets.UTF_8), null);
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }


    @Override
    public void close() throws IOException {
        if (isLogin()) {
            channelPoolMapManager.getLoginChannelPoolMap().remove(loginInfoPoolKey);
        } else {
            channelPoolMapManager.getChannelPoolMap().remove(serverAddress);
        }
    }

    private boolean isLogin() {
        return loginInfoPoolKey != null;
    }

}
