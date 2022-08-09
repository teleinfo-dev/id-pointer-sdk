package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.session.IdUser;
import cn.teleinfo.idpointer.sdk.session.SessionIdFactory;
import cn.teleinfo.idpointer.sdk.session.SessionIdFactoryDefault;
import cn.teleinfo.idpointer.sdk.transport.*;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultIdClient implements IDClient {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DefaultIdClient.class);
    private final TransportOnTcp transportOnTcp;
    private final InetSocketAddress serverAddress;
    private final RequestIdFactory requestIdGenerate;
    private final int promiseTimeout;
    private final ChannelPoolMapManager channelPoolMapManager;

    private byte minorVersion = Common.TELEINFO_MINOR_VERSION;

    private int sessionId = 0;
    private IdUser idUser;

    public DefaultIdClient(InetSocketAddress serverAddress, ChannelPoolMapManager channelPoolMapManager) {
        this.channelPoolMapManager = channelPoolMapManager;
        this.transportOnTcp = new TransportOnTcp(channelPoolMapManager.getChannelPoolMap(), channelPoolMapManager.getMessageManager());
        this.serverAddress = serverAddress;
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();
        this.promiseTimeout = 60;
    }

    public DefaultIdClient(InetSocketAddress serverAddress, int nThreads, int maxConnectionsPerServer, int promiseTimeout) {
        this.channelPoolMapManager = new ChannelPoolMapManager(nThreads, maxConnectionsPerServer, promiseTimeout);
        this.transportOnTcp = new TransportOnTcp(channelPoolMapManager.getChannelPoolMap(), channelPoolMapManager.getMessageManager());
        this.serverAddress = serverAddress;
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();
        this.promiseTimeout = promiseTimeout;
    }

    @Override
    public int login(AuthenticationInfo authenticationInfo) throws IDException, HandleException, UnsupportedEncodingException {
        return loginByOneChannle(authenticationInfo);
    }

    private int loginByChannelPool(AuthenticationInfo authenticationInfo) throws IDException, HandleException, UnsupportedEncodingException {
        SessionIdFactory sessionIdFactory = SessionIdFactoryDefault.getInstance();
        int newSessionId = sessionIdFactory.getNextInteger();
        String userIdHandle = new String(authenticationInfo.getUserIdHandle(), Common.TEXT_ENCODING);

        log.info("user {}:{} login {}:{} begin", authenticationInfo.getUserIdIndex(), userIdHandle, serverAddress.getAddress(), serverAddress.getPort());

        // 加密连接备用
        GenericRequest getSiteInfoRequest = new GenericRequest(Util.encodeString("/"), AbstractMessage.OC_GET_SITE_INFO, null);
        AbstractResponse siteResponse = doRequest(getSiteInfoRequest);

        LoginIDSystemRequest loginIDSystemRequest = new LoginIDSystemRequest(authenticationInfo.getUserIdHandle(), authenticationInfo.getUserIdIndex(), authenticationInfo);
        loginIDSystemRequest.returnRequestDigest = true;//该位不能改
        loginIDSystemRequest.minorProtocolVersion = minorVersion;
        loginIDSystemRequest.rdHashType = Common.HASH_CODE_SHA256;
        loginIDSystemRequest.sessionId = newSessionId;
        loginIDSystemRequest.ignoreRestrictedValues = false;
        loginIDSystemRequest.cacheCertify = false;
        loginIDSystemRequest.certify = false;

        AbstractResponse loginResponse = doRequest(loginIDSystemRequest);

        ChallengeResponse challengeResponse = (ChallengeResponse) loginResponse;

        byte[] signature = authenticationInfo.authenticate(challengeResponse, loginIDSystemRequest);

        ChallengeAnswerRequest challengeAnswerRequest = new ChallengeAnswerRequest(authenticationInfo.getAuthType(), authenticationInfo.getUserIdHandle(), authenticationInfo.getUserIdIndex(), signature, authenticationInfo);
        challengeAnswerRequest.minorProtocolVersion = minorVersion;
        challengeAnswerRequest.sessionId = challengeResponse.sessionId;
        challengeAnswerRequest.rdHashType = challengeResponse.rdHashType;
        challengeAnswerRequest.returnRequestDigest = true;
        challengeAnswerRequest.ignoreRestrictedValues = false;
        challengeAnswerRequest.cacheCertify = false;
        challengeAnswerRequest.certify = false;

        AbstractResponse challengeAnswerResponse = doRequest(challengeAnswerRequest);

        this.sessionId = newSessionId;
        this.idUser = new IdUser(userIdHandle, authenticationInfo.getUserIdIndex());

        log.info("user {}:{} ,login {}:{} success", authenticationInfo.getUserIdIndex(), userIdHandle, serverAddress.getAddress(), serverAddress.getPort());
        return newSessionId;
    }

    private int loginByOneChannle(AuthenticationInfo authenticationInfo) throws IDException, HandleException, UnsupportedEncodingException {
        SessionIdFactory sessionIdFactory = SessionIdFactoryDefault.getInstance();
        int newSessionId = sessionIdFactory.getNextInteger();
        String userIdHandle = new String(authenticationInfo.getUserIdHandle(), Common.TEXT_ENCODING);

        log.info("user {}:{} login {}:{} begin", authenticationInfo.getUserIdIndex(), userIdHandle, serverAddress.getAddress(), serverAddress.getPort());

        // 加密连接备用
        GenericRequest getSiteInfoRequest = new GenericRequest(Util.encodeString("/"), AbstractMessage.OC_GET_SITE_INFO, null);
        AbstractResponse siteResponse = doRequest(getSiteInfoRequest);

        LoginIDSystemRequest loginIDSystemRequest = new LoginIDSystemRequest(authenticationInfo.getUserIdHandle(), authenticationInfo.getUserIdIndex(), authenticationInfo);
        loginIDSystemRequest.requestId = requestIdGenerate.getNextInteger();
        loginIDSystemRequest.minorProtocolVersion = minorVersion;
        loginIDSystemRequest.returnRequestDigest = true;//该位不能改
        loginIDSystemRequest.rdHashType = Common.HASH_CODE_SHA256;
        loginIDSystemRequest.sessionId = newSessionId;
        loginIDSystemRequest.ignoreRestrictedValues = false;
        loginIDSystemRequest.cacheCertify = false;
        loginIDSystemRequest.certify = false;

        ChannelPool channelPool = transportOnTcp.getIdChannelPoolMap().get(serverAddress);
        Future<Channel> channelFuture = channelPool.acquire();
        Channel channel = null;

        try {
            channel = channelFuture.get();
            ResponsePromise responsePromise = transportOnTcp.getMessageManager().process(loginIDSystemRequest, channel);
            AbstractResponse loginResponse = responsePromise.get(10, TimeUnit.SECONDS);
            if (loginResponse instanceof LoginIDSystemResponse) {
                throw new IDException(loginResponse.responseCode, "login user id error");
            }

            if (loginResponse instanceof ChallengeResponse) {
                ChallengeResponse challengeResponse = (ChallengeResponse) loginResponse;
                checkResponse(challengeResponse);

                byte[] signature = authenticationInfo.authenticate(challengeResponse, loginIDSystemRequest);

                ChallengeAnswerRequest challengeAnswerRequest = new ChallengeAnswerRequest(authenticationInfo.getAuthType(), authenticationInfo.getUserIdHandle(), authenticationInfo.getUserIdIndex(), signature, authenticationInfo);
                challengeAnswerRequest.requestId = requestIdGenerate.getNextInteger();
                challengeAnswerRequest.minorProtocolVersion = minorVersion;
                challengeAnswerRequest.sessionId = challengeResponse.sessionId;
                challengeAnswerRequest.rdHashType = challengeResponse.rdHashType;
                challengeAnswerRequest.returnRequestDigest = true;
                challengeAnswerRequest.ignoreRestrictedValues = false;
                challengeAnswerRequest.cacheCertify = false;
                challengeAnswerRequest.certify = false;

                ResponsePromise challengeAnswerResponsePromise = transportOnTcp.getMessageManager().process(challengeAnswerRequest, channel);
                AbstractResponse challengeAnswerResponse = challengeAnswerResponsePromise.get(10, TimeUnit.SECONDS);
                checkResponse(challengeAnswerResponse);
            } else {
                throw new IDException(loginResponse.responseCode, loginResponse.toString());
            }

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IDException(IDException.CHANNEL_GET_ERROR, "channel_get_error on login", e);
        } finally {
            if (channel != null) {
                channelPool.release(channel);
            }
        }

        this.sessionId = newSessionId;
        this.idUser = new IdUser(userIdHandle, authenticationInfo.getUserIdIndex());

        log.info("user {}:{} ,login {}:{} success", authenticationInfo.getUserIdIndex(), userIdHandle, serverAddress.getAddress(), serverAddress.getPort());
        return newSessionId;
    }

    private void checkResponse(AbstractResponse response) throws IDException {
        if (response.responseCode != AbstractMessage.RC_SUCCESS && response.responseCode != AbstractMessage.RC_AUTHENTICATION_NEEDED) {
            throw new IDException(IDException.RC_INVALID_RESPONSE_CODE, response);
        }
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
                throw new IDException(IDException.RC_INVALID_RESPONSE_CODE, response);
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
     * todo: 响应码梳理
     *
     * @param request
     * @return
     * @throws IDException
     */
    private ResponsePromise doRequestInternal(AbstractRequest request) throws IDException {

        request.requestId = requestIdGenerate.getNextInteger();
        request.minorProtocolVersion = minorVersion;
        if (this.sessionId != 0) {
            request.sessionId = this.sessionId;
        }

        ResponsePromise responsePromise = null;
        try {
            responsePromise = transportOnTcp.process(request, serverAddress);
        } catch (Exception e) {
            if (responsePromise != null && !responsePromise.isDone()) {
                responsePromise.setFailure(e);
            }
            log.warn("process error,request id is {}", request.requestId, e);
            throw new IDException(IDException.PROMISE_GET_ERROR, "process error", e);
        }
        return responsePromise;
    }

    @Override
    public HandleValue[] resolveHandle(String handle, String[] types, int[] indexes, boolean auth) throws IDException {
        if (auth) {
            if (idUser == null) {
                throw new IDException(0, "not auth");
            }
        }
        byte[][] reqTypes = null;
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
                throw new IDException(IDException.RC_INVALID_VALUE, e, response);
            }
        } else {
            throw new IDException(IDException.RC_ILLEGAL_RESPONSE, "not resolution response", response);
        }
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
                throw new IDException(IDException.RC_INVALID_VALUE, e, response);
            }
        } else {
            throw new IDException(IDException.RC_ILLEGAL_RESPONSE, "not resolution response", response);
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
            if (idUser == null) {
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
    public int getSessionId() {
        return sessionId;
    }

    @Override
    public IdUser getIdUser() {
        return idUser;
    }

    @Override
    public void close() throws IOException {
        channelPoolMapManager.getChannelPoolMap().remove(serverAddress);
    }

    @Override
    public void setMinorVersion(byte minorVersion) {
        this.minorVersion = minorVersion;
    }
}
