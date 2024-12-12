package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.client.LoginInfoPoolKey;
import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.session.SessionDefault;
import cn.teleinfo.idpointer.sdk.session.SessionIdFactory;
import cn.teleinfo.idpointer.sdk.session.SessionIdFactoryDefault;
import cn.teleinfo.idpointer.sdk.transport.v3.IdTcpTransport;
import cn.teleinfo.idpointer.sdk.transport.v3.RequestIdFactory;
import cn.teleinfo.idpointer.sdk.util.ResponseUtils;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.util.Attribute;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TransportOnTcpLogin {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TransportOnTcpLogin.class);
    private final ChannelPoolMap<LoginInfoPoolKey, TimedChannelPool> idChannelPoolMap;
    private final MessageManager messageManager;
    private final RequestIdFactory requestIdGenerate;

    private final TransportEncryptHandler encryptHandler;

    public TransportOnTcpLogin(ChannelPoolMap<LoginInfoPoolKey, TimedChannelPool> idChannelPoolMap, MessageManager messageManager, RequestIdFactory requestIdGenerate) {
        this.idChannelPoolMap = idChannelPoolMap;
        this.messageManager = messageManager;
        this.requestIdGenerate = requestIdGenerate;
        this.encryptHandler = new TransportEncryptHandler(requestIdGenerate);
    }

    public ResponsePromise process(AbstractIdRequest request, LoginInfoPoolKey loginInfoPoolKey, AuthenticationInfo authenticationInfo) throws IDException {

        ChannelPool fixedChannelPool = idChannelPoolMap.get(loginInfoPoolKey);
        log.debug("login fixedChannelPool: {}", fixedChannelPool);
        Future<Channel> channelFuture = fixedChannelPool.acquire();
        Channel channel = null;
        try {
            channel = channelFuture.get();
            Attribute<SessionDefault> attr = channel.attr(IdTcpTransport.SESSION_KEY);
            SessionDefault sessionDefault = attr.get();

            if (sessionDefault == null) {
                SessionIdFactory sessionIdFactory = SessionIdFactoryDefault.getInstance();
                int newSessionId = sessionIdFactory.getNextInteger();
                sessionDefault = new SessionDefault(newSessionId);
                attr.set(sessionDefault);
            }

            encryptHandler.handle(channel, request, messageManager, authenticationInfo);

            if (!sessionDefault.isAuthenticated()) {

                // 没有登录,去登录
                try {

                    InetSocketAddress serverAddress = loginInfoPoolKey.getAddress();
                    String userIdHandle = Util.decodeString(authenticationInfo.getUserIdHandle());
                    log.info("channel {},user {}:{} ,server {}:{} login begin", channel.localAddress(), authenticationInfo.getUserIdIndex(), userIdHandle,
                            serverAddress.getAddress(), serverAddress.getPort());

                    login(channel, sessionDefault, authenticationInfo);

                    sessionDefault.setIdUserId(loginInfoPoolKey.getUserId());

                    log.info("channel {},user {}:{} ,server {}:{} login success", channel.localAddress(), authenticationInfo.getUserIdIndex(), userIdHandle, serverAddress.getAddress(), serverAddress.getPort());

                    attr.set(sessionDefault);

                } catch (Exception e) {
                    if (channel != null) {
                        fixedChannelPool.release(channel);
                    }
                    // 登录失败
                    throw new IDException(IDException.SERVER_CANNOT_PROCESS_SESSION, "登录失败",e);
                }
            }

            request.sessionId = sessionDefault.getSessionId();

            ResponsePromise promise = messageManager.process(request, channel);

            final Channel finalChannel = channel;

            promise.addListener(future -> {
                // 使用监听器处理
                if (future.isDone()) {
                    if (finalChannel != null) {
                        fixedChannelPool.release(finalChannel);
                    }
                }
            });
            return promise;
        } catch (ExecutionException | InterruptedException e) {
            throw new IDException(IDException.CHANNEL_GET_ERROR, "channel get error", e);
        }
    }

    public ChannelPoolMap<LoginInfoPoolKey, TimedChannelPool> getIdChannelPoolMap() {
        return idChannelPoolMap;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    private void login(Channel channel, SessionDefault sessionDefault, AuthenticationInfo authenticationInfo) throws IDException, HandleException, UnsupportedEncodingException {

        try {

            // 获取SITE_INFO
            GenericIdRequest getSiteInfoRequest = new GenericIdRequest(Util.encodeString("/"), AbstractMessage.OC_GET_SITE_INFO, null);
            getSiteInfoRequest.requestId = requestIdGenerate.getNextInteger();
            getSiteInfoRequest.sessionId = sessionDefault.getSessionId();
            getSiteInfoRequest.encrypt = sessionDefault.isEncryptMessage();
            ResponsePromise getSiteInfoResponsePromise = messageManager.process(getSiteInfoRequest, channel);
            getSiteInfoResponsePromise.get(10,TimeUnit.SECONDS);

            LoginIDSystemIdRequest loginIDSystemRequest = new LoginIDSystemIdRequest(authenticationInfo.getUserIdHandle(), authenticationInfo.getUserIdIndex(), authenticationInfo);
            loginIDSystemRequest.requestId = requestIdGenerate.getNextInteger();
            // 该位不能改
            loginIDSystemRequest.returnRequestDigest = true;
            loginIDSystemRequest.rdHashType = Common.HASH_CODE_SHA256;
            loginIDSystemRequest.sessionId = sessionDefault.getSessionId();
            loginIDSystemRequest.ignoreRestrictedValues = false;
            loginIDSystemRequest.cacheCertify = false;
            loginIDSystemRequest.certify = false;
            loginIDSystemRequest.encrypt = sessionDefault.isEncryptMessage();

            ResponsePromise responsePromise = messageManager.process(loginIDSystemRequest, channel);
            AbstractIdResponse loginResponse = responsePromise.get(10, TimeUnit.SECONDS);
            if (loginResponse instanceof LoginIDSystemIdResponse) {
                throw new IDException(loginResponse.responseCode, "login user id error");
            }

            if (loginResponse instanceof ChallengeIdResponse) {
                ChallengeIdResponse challengeResponse = (ChallengeIdResponse) loginResponse;
                ResponseUtils.checkResponseCode(challengeResponse);

                byte[] signature = authenticationInfo.authenticate(challengeResponse, loginIDSystemRequest);

                ChallengeAnswerIdRequest challengeAnswerRequest = new ChallengeAnswerIdRequest(authenticationInfo.getAuthType(), authenticationInfo.getUserIdHandle(), authenticationInfo.getUserIdIndex(), signature, authenticationInfo);
                challengeAnswerRequest.requestId = requestIdGenerate.getNextInteger();
                challengeAnswerRequest.sessionId = challengeResponse.sessionId;
                challengeAnswerRequest.rdHashType = challengeResponse.rdHashType;
                challengeAnswerRequest.returnRequestDigest = true;
                challengeAnswerRequest.ignoreRestrictedValues = false;
                challengeAnswerRequest.cacheCertify = false;
                challengeAnswerRequest.certify = false;

                challengeAnswerRequest.encrypt = sessionDefault.isEncryptMessage();

                ResponsePromise challengeAnswerResponsePromise = messageManager.process(challengeAnswerRequest, channel);
                AbstractIdResponse challengeAnswerResponse = challengeAnswerResponsePromise.get(10000, TimeUnit.SECONDS);
                ResponseUtils.checkResponseCode(challengeAnswerResponse);

            } else {
                throw new IDException(loginResponse.responseCode, loginResponse.toString());
            }

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IDException(IDException.CHANNEL_GET_ERROR, "channel_get_error on login", e);
        }

    }


}
