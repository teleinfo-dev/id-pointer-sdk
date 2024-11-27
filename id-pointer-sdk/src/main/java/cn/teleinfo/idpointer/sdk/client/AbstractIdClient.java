package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.transport.ResponsePromise;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public abstract class AbstractIdClient implements IDClient{

    private final InetSocketAddress serverAddress;
    private final int promiseTimeout;
    /**
     * 传进来的身份认证信息
     */
    private final AuthenticationInfo authenticationInfo;
    /**
     * DefaultIdClient内部使用,连接池的key
     */
    private final LoginInfoPoolKey loginInfoPoolKey;
    private final boolean encrypt;

    protected AbstractIdClient(InetSocketAddress serverAddress, int promiseTimeout,AuthenticationInfo authenticationInfo, LoginInfoPoolKey loginInfoPoolKey, boolean encrypt) {
        this.serverAddress = serverAddress;
        this.promiseTimeout = promiseTimeout;
        this.loginInfoPoolKey = loginInfoPoolKey;
        this.encrypt = encrypt;
        this.authenticationInfo = authenticationInfo;
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

    protected abstract AbstractResponse doRequest(AbstractRequest request) throws IDException;
    protected abstract ResponsePromise doRequestInternal(AbstractRequest request) throws IDException;

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

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes, boolean auth) throws IDException {
        if (auth) {
            if (loginInfoPoolKey == null) {
                throw new IDException(0, "not auth");
            }
        }
        byte[][] reqTypes = getTypeStringBytes(types);

        ResolutionRequest request = new ResolutionRequest(handle.getBytes(StandardCharsets.UTF_8), reqTypes, indexes, null);
        return doResolve(request);
    }

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes, String authString) throws IDException {

        byte[][] reqTypes = getTypeStringBytes(types);

        ResolutionRequest request = new ResolutionRequest(handle.getBytes(StandardCharsets.UTF_8), reqTypes, indexes, null, authString);
        request.recursionAuth = true;
        return doResolve(request);
    }

    @Override
    public HandleValue[] resolveHandle(String handle) throws IDException {
        return resolveHandle(handle, null, null);
    }

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes) throws IDException {
        byte reqTypes[][] = getTypeStringBytes(types);

        ResolutionRequest request = new ResolutionRequest(handle.getBytes(StandardCharsets.UTF_8), reqTypes, indexes, null);
        return doResolve(request);
    }

    private HandleValue[] doResolve(ResolutionRequest request) throws IDException {
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
        byte reqTypes[][] = getTypeStringBytes(types);
        ResolutionRequest request = new ResolutionRequest(handle.getBytes(StandardCharsets.UTF_8), reqTypes, indexes, null);
        ResponsePromise responsePromise = doRequestInternal(request);
        return responsePromise;
    }

    @Override
    public ResponsePromise resolveHandleAsync(String handle, String[] types, int[] indexes) throws IDException {
        byte reqTypes[][] = getTypeStringBytes(types);
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

    public LoginInfoPoolKey getLoginInfoPoolKey() {
        return loginInfoPoolKey;
    }

    public boolean isEncrypt() {
        return encrypt;
    }

    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }

    public int getPromiseTimeout() {
        return promiseTimeout;
    }

    public AuthenticationInfo getAuthenticationInfo() {
        return authenticationInfo;
    }
}
