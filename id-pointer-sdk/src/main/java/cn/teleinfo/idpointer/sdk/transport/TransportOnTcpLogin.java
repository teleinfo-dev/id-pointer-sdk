package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.client.DefaultUserId;
import cn.teleinfo.idpointer.sdk.client.LoginInfo;
import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.session.SessionIdFactory;
import cn.teleinfo.idpointer.sdk.session.SessionIdFactoryDefault;
import cn.teleinfo.idpointer.sdk.util.ResponseUtils;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static cn.teleinfo.idpointer.sdk.exception.IDException.SERVER_CANNOT_PROCESS_SESSION;

public class TransportOnTcpLogin {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TransportOnTcpLogin.class);
    private final ChannelPoolMap<LoginInfo, TimedChannelPool> idChannelPoolMap;
    private final MessageManager messageManager;
    private final RequestIdFactory requestIdGenerate;


    public TransportOnTcpLogin(ChannelPoolMap<LoginInfo, TimedChannelPool> idChannelPoolMap, MessageManager messageManager, RequestIdFactory requestIdGenerate) {
        this.idChannelPoolMap = idChannelPoolMap;
        this.messageManager = messageManager;
        this.requestIdGenerate = requestIdGenerate;
    }

    public ResponsePromise process(AbstractRequest request, LoginInfo loginInfo, AuthenticationInfo authenticationInfo) throws IDException {

        ChannelPool fixedChannelPool = idChannelPoolMap.get(loginInfo);
        log.debug("login fixedChannelPool: {}", fixedChannelPool);
        Future<Channel> channelFuture = fixedChannelPool.acquire();
        Channel channel = null;
        try {
            channel = channelFuture.get();

            Attribute<Integer> attr = channel.attr(Transport.SESSION_ID_KEY);
            Integer sessionId = attr.get();

            if (sessionId == null) {
                // 没有登录,去登录
                try {
                    InetSocketAddress serverAddress = loginInfo.getAddress();
                    String userIdHandle = Util.decodeString(authenticationInfo.getUserIdHandle());
                    log.info("channel {},user {}:{} ,server {}:{} login begin", channel.localAddress() ,authenticationInfo.getUserIdIndex(), userIdHandle,
                            serverAddress.getAddress(), serverAddress.getPort());

                    sessionId = login(channel, authenticationInfo);

                    log.info("channel {},user {}:{} ,server {}:{} login success",channel.localAddress() , authenticationInfo.getUserIdIndex(), userIdHandle, serverAddress.getAddress(), serverAddress.getPort());

                    attr.set(sessionId);
                } catch (Exception e) {
                    if (channel != null) {
                        fixedChannelPool.release(channel);
                    }
                    // 登录失败
                    throw new IDException(IDException.SERVER_CANNOT_PROCESS_SESSION,"登录失败");
                }
            }

            request.sessionId = sessionId;

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

    public ChannelPoolMap<LoginInfo, TimedChannelPool> getIdChannelPoolMap() {
        return idChannelPoolMap;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    private int login(Channel channel, AuthenticationInfo authenticationInfo) throws IDException, HandleException, UnsupportedEncodingException {

        SessionIdFactory sessionIdFactory = SessionIdFactoryDefault.getInstance();
        int newSessionId = sessionIdFactory.getNextInteger();

        // 加密连接备用
        GenericRequest getSiteInfoRequest = new GenericRequest(Util.encodeString("/"), AbstractMessage.OC_GET_SITE_INFO, null);
        messageManager.process(getSiteInfoRequest, channel);

        LoginIDSystemRequest loginIDSystemRequest = new LoginIDSystemRequest(authenticationInfo.getUserIdHandle(), authenticationInfo.getUserIdIndex(), authenticationInfo);
        loginIDSystemRequest.requestId = requestIdGenerate.getNextInteger();
        loginIDSystemRequest.returnRequestDigest = true;//该位不能改
        loginIDSystemRequest.rdHashType = Common.HASH_CODE_SHA256;
        loginIDSystemRequest.sessionId = newSessionId;
        loginIDSystemRequest.ignoreRestrictedValues = false;
        loginIDSystemRequest.cacheCertify = false;
        loginIDSystemRequest.certify = false;

        try {
            ResponsePromise responsePromise = messageManager.process(loginIDSystemRequest, channel);
            AbstractResponse loginResponse = responsePromise.get(10, TimeUnit.SECONDS);
            if (loginResponse instanceof LoginIDSystemResponse) {
                throw new IDException(loginResponse.responseCode, "login user id error");
            }

            if (loginResponse instanceof ChallengeResponse) {
                ChallengeResponse challengeResponse = (ChallengeResponse) loginResponse;
                ResponseUtils.checkResponse(challengeResponse);

                byte[] signature = authenticationInfo.authenticate(challengeResponse, loginIDSystemRequest);

                ChallengeAnswerRequest challengeAnswerRequest = new ChallengeAnswerRequest(authenticationInfo.getAuthType(), authenticationInfo.getUserIdHandle(), authenticationInfo.getUserIdIndex(), signature, authenticationInfo);
                challengeAnswerRequest.requestId = requestIdGenerate.getNextInteger();
                challengeAnswerRequest.sessionId = challengeResponse.sessionId;
                challengeAnswerRequest.rdHashType = challengeResponse.rdHashType;
                challengeAnswerRequest.returnRequestDigest = true;
                challengeAnswerRequest.ignoreRestrictedValues = false;
                challengeAnswerRequest.cacheCertify = false;
                challengeAnswerRequest.certify = false;

                ResponsePromise challengeAnswerResponsePromise = messageManager.process(challengeAnswerRequest, channel);
                AbstractResponse challengeAnswerResponse = challengeAnswerResponsePromise.get(10, TimeUnit.SECONDS);
                ResponseUtils.checkResponse(challengeAnswerResponse);
            } else {
                throw new IDException(loginResponse.responseCode, loginResponse.toString());
            }

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IDException(IDException.CHANNEL_GET_ERROR, "channel_get_error on login", e);
        }

        return newSessionId;
    }


}
