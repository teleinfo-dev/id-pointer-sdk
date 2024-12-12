package cn.teleinfo.idpointer.sdk.client;

import cn.hutool.crypto.asymmetric.SM2;
import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.protocol.decoder.HandleDecoder;
import cn.teleinfo.idpointer.sdk.protocol.encoder.HandleEncoder;
import cn.teleinfo.idpointer.sdk.session.SessionDefault;
import cn.teleinfo.idpointer.sdk.session.SessionIdFactory;
import cn.teleinfo.idpointer.sdk.session.SessionIdFactoryDefault;
import cn.teleinfo.idpointer.sdk.transport.RequestIdFactoryDefault;
import cn.teleinfo.idpointer.sdk.transport.ResponsePromise;
import cn.teleinfo.idpointer.sdk.transport.sample.MessagePromiseManager;
import cn.teleinfo.idpointer.sdk.transport.sample.SimpleMessageHandler;
import cn.teleinfo.idpointer.sdk.transport.v3.IdTcpTransport;
import cn.teleinfo.idpointer.sdk.transport.v3.RequestIdFactory;
import cn.teleinfo.idpointer.sdk.util.EncryptionUtils;
import cn.teleinfo.idpointer.sdk.util.ResponseUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SampleIdClient extends AbstractIdClient {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractIdClient.class);
    private final RequestIdFactory requestIdGenerate;
    private final FixedChannelPool fixedChannelPool;

    /**
     * @param serverAddress  服务器地址
     * @param promiseTimeout 超时时间
     */
    public SampleIdClient(InetSocketAddress serverAddress, int promiseTimeout) {
        this(serverAddress, promiseTimeout, 50);
    }

    /**
     * @param serverAddress  服务器地址
     * @param promiseTimeout 超时时间
     * @param maxConnections 最大连接数
     */
    public SampleIdClient(InetSocketAddress serverAddress, int promiseTimeout, int maxConnections) {
        super(serverAddress, promiseTimeout, null, null, false);
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();

        SimpleMessageHandler messageHandler = new SimpleMessageHandler();

        ChannelPoolHandler channelPoolHandler = new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel ch) throws Exception {
                ch.pipeline().addLast(new HandleEncoder());
                ch.pipeline().addLast(new HandleDecoder());
                ch.pipeline().addLast(messageHandler);
            }

        };

        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .remoteAddress(getServerAddress())
        ;

        this.fixedChannelPool = new FixedChannelPool(bootstrap, channelPoolHandler, maxConnections);
    }

    public SampleIdClient(InetSocketAddress serverAddress, int promiseTimeout, int maxConnections, AuthenticationInfo authenticationInfo, boolean encrypt) {
        super(serverAddress, promiseTimeout, authenticationInfo, new LoginInfoPoolKey(serverAddress, new ValueReference(Util.decodeString(authenticationInfo.getUserIdHandle()), authenticationInfo.getUserIdIndex())), encrypt);
        this.requestIdGenerate = RequestIdFactoryDefault.getInstance();
        SimpleMessageHandler messageHandler = new SimpleMessageHandler();

        ChannelPoolHandler channelPoolHandler = new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel ch) throws Exception {
                ch.pipeline().addLast(new HandleEncoder());
                ch.pipeline().addLast(new HandleDecoder());
                ch.pipeline().addLast(messageHandler);
            }

        };

        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .remoteAddress(getServerAddress())
        ;

        this.fixedChannelPool = new FixedChannelPool(bootstrap, channelPoolHandler, maxConnections);
    }

    @Override
    protected AbstractIdResponse doRequest(AbstractIdRequest request) throws IDException {
        int nextInteger = requestIdGenerate.getNextInteger();
        request.requestId = nextInteger;

        Future<Channel> channelFuture = fixedChannelPool.acquire();

        Channel channel = null;
        try {
            channel = channelFuture.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IDException(IDException.CHANNEL_GET_ERROR, "Can't get channel from pool", e);
        }

        AuthenticationInfo authenticationInfo = getAuthenticationInfo();


        try {
            SessionDefault sessionDefault = handleLogin(channel, authenticationInfo);
            if (sessionDefault != null) {
                request.sessionId = sessionDefault.getSessionId();
            }
            handle(channel, request, authenticationInfo);

            Promise<AbstractIdResponse> promise = getResponsePromise(request, channel);
            AbstractIdResponse response = promise.sync().get(getPromiseTimeout(), TimeUnit.SECONDS);

            if (response.responseCode != AbstractMessage.RC_SUCCESS && response.responseCode != AbstractMessage.RC_AUTHENTICATION_NEEDED) {
                throw new IDException(IDException.RC_INVALID_RESPONSE_CODE,"response code error", response);
            }
            return response;

        } catch (InterruptedException e) {
            log.warn("Interrupted", e);
            throw new RuntimeException(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            if (channel != null) {
                fixedChannelPool.release(channel);
            }
        }
    }

    private SessionDefault handleLogin(Channel channel, AuthenticationInfo authenticationInfo) throws IDException {
        if (authenticationInfo != null) {

            Attribute<SessionDefault> attr = channel.attr(IdTcpTransport.SESSION_KEY);
            SessionDefault sessionDefault = attr.get();
            if (sessionDefault == null) {
                SessionIdFactory sessionIdFactory = SessionIdFactoryDefault.getInstance();
                int newSessionId = sessionIdFactory.getNextInteger();
                sessionDefault = new SessionDefault(newSessionId);
                attr.set(sessionDefault);
            }
            if (!sessionDefault.isAuthenticated()) {

                // 没有登录,去登录
                try {

                    String userIdHandle = Util.decodeString(authenticationInfo.getUserIdHandle());
                    log.info("channel {},user {}:{} ,server {}:{} login begin", channel.localAddress(), authenticationInfo.getUserIdIndex(), userIdHandle,
                            getServerAddress().getAddress(), getServerAddress().getPort());

                    login(channel, sessionDefault, authenticationInfo);

                    sessionDefault.setIdUserId(authenticationInfo.getUserValueReference());

                    log.info("channel {},user {}:{} ,server {}:{} login success", channel.localAddress(), authenticationInfo.getUserIdIndex(), userIdHandle, getServerAddress().getAddress(), getServerAddress().getPort());

                    attr.set(sessionDefault);

                } catch (Exception e) {
                    // 登录失败
                    throw new IDException(IDException.SERVER_CANNOT_PROCESS_SESSION, "登录失败", e);
                }
            }
            return sessionDefault;

        }
        return null;
    }

    private void login(Channel channel, SessionDefault sessionDefault, AuthenticationInfo authenticationInfo) throws IDException, HandleException, UnsupportedEncodingException {

        try {
            // 获取SITE_INFO
            GenericIdRequest getSiteInfoRequest = new GenericIdRequest(Util.encodeString("/"), AbstractMessage.OC_GET_SITE_INFO, null);
            getSiteInfoRequest.requestId = requestIdGenerate.getNextInteger();
            getSiteInfoRequest.sessionId = sessionDefault.getSessionId();
            getSiteInfoRequest.encrypt = sessionDefault.isEncryptMessage();
            Promise<AbstractIdResponse> getSiteInfoResponsePromise = getResponsePromise(getSiteInfoRequest, channel);
            getSiteInfoResponsePromise.get(10, TimeUnit.SECONDS);

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

            Promise<AbstractIdResponse> responsePromise = getResponsePromise(loginIDSystemRequest, channel);
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

                Promise<AbstractIdResponse> challengeAnswerResponsePromise = getResponsePromise(challengeAnswerRequest, channel);
                AbstractIdResponse challengeAnswerResponse = challengeAnswerResponsePromise.get(10000, TimeUnit.SECONDS);
                ResponseUtils.checkResponseCode(challengeAnswerResponse);

            } else {
                throw new IDException(loginResponse.responseCode, loginResponse.toString());
            }

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IDException(IDException.CHANNEL_GET_ERROR, "channel_get_error on login", e);
        }

    }


    private Promise<AbstractIdResponse> getResponsePromise(AbstractIdRequest request, Channel channel) {
        Promise<AbstractIdResponse> promise = new DefaultPromise<>(channel.eventLoop());
        channel.attr(MessagePromiseManager.PROMISE_ATTRIBUTE_KEY).set(promise);
        channel.writeAndFlush(request);
        return promise;
    }

    public void handle(Channel channel, AbstractIdRequest request, AuthenticationInfo authenticationInfo) throws IDException {
        if (request.encrypt) {
            Attribute<SessionDefault> attr = channel.attr(IdTcpTransport.SESSION_KEY);
            SessionDefault sessionDefault = attr.get();
            if (sessionDefault == null || !sessionDefault.isEncryptMessage()) {
                throw new IDException(IDException.ENCRYPTION_ERROR, "session not setup");
            }
            // 当没有进行加密时
            if (!sessionDefault.isEncryptMessage()) {

                // 密钥交换
                SessionSetupIdRequest sessionSetupRequest = new SessionSetupIdRequest(authenticationInfo.getUserIdHandle(), authenticationInfo.getUserIdIndex());
                sessionSetupRequest.requestId = requestIdGenerate.getNextInteger();
                sessionSetupRequest.sessionId = sessionDefault.getSessionId();
                sessionSetupRequest.keyExchangeMode = Common.KEY_EXCHANGE_CIPHER_HDL;

                Promise<AbstractIdResponse> sessionSetupPromise = getResponsePromise(sessionSetupRequest, channel);

                AbstractIdResponse sessionSetupTempResponse = null;
                try {
                    sessionSetupTempResponse = sessionSetupPromise.get(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new IDException(IDException.PROMISE_GET_ERROR, "session setup response error", e);
                } catch (ExecutionException e) {
                    throw new IDException(IDException.PROMISE_GET_ERROR, "session setup response error", e);
                } catch (TimeoutException e) {
                    throw new IDException(IDException.PROMISE_GET_ERROR, "session setup response error", e);
                }
                SessionSetupIdResponse sessionSetupResponse = (SessionSetupIdResponse) sessionSetupTempResponse;


                if (sessionSetupResponse.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_HDL) {
                    PublicKeyAuthenticationInfo publicKeyAuthenticationInfo = (PublicKeyAuthenticationInfo) authenticationInfo;
                    PrivateKey privateKey = publicKeyAuthenticationInfo.getPrivateKey();

                    String alg = privateKey.getAlgorithm().trim();

                    if (alg.equals("SM2")) {
                        byte[] sessionKeyBytes;
                        try {

                            final SM2 sm2 = new SM2(privateKey, null);
                            sessionKeyBytes = sm2.decrypt(sessionSetupResponse.data);
                        } catch (Exception e) {
                            throw new IDException(IDException.CLIENT_ERROR, "decrypt sessionKey error", e);
                        }

                        int sessionKeyAlg = Encoder.readInt(sessionKeyBytes, 0);
                        byte[] sessionKey = Util.substring(sessionKeyBytes, Encoder.INT_SIZE);
                        sessionDefault.setSessionKey(sessionKey);
                        sessionDefault.setSessionKeyAlgorithmCode(sessionKeyAlg);

                        sessionDefault.setEncryptMessage(true);
                        log.info("sessionKeyAlg:{},session key:{}", sessionKeyAlg, Hex.encodeHexString(sessionKey));

                    } else if (alg.equals("RSA")) {
                        byte[] sessionKeyBytes;
                        try {
                            sessionKeyBytes = EncryptionUtils.decryptByKey(sessionSetupResponse.data, privateKey);
                        } catch (Exception e) {
                            throw new IDException(IDException.CLIENT_ERROR, "decrypt sessionKey error", e);
                        }

                        int sessionKeyAlg = Encoder.readInt(sessionKeyBytes, 0);
                        byte[] sessionKey = Util.substring(sessionKeyBytes, Encoder.INT_SIZE);
                        sessionDefault.setSessionKey(sessionKey);
                        sessionDefault.setSessionKeyAlgorithmCode(sessionKeyAlg);

                        sessionDefault.setEncryptMessage(true);
                        log.info("sessionKeyAlg:{},session key:{}", sessionKeyAlg, Hex.encodeHexString(sessionKey));

                    }
                } else {
                    throw new IDException(IDException.CLIENT_ERROR, "Un support keyExchangeMode");
                }
            }
        }

    }


    @Override
    protected ResponsePromise doRequestInternal(AbstractIdRequest request) throws IDException {
        throw new IDException(IDException.CLIENT_ERROR, "Not implemented");
    }

    @Override
    public void close() throws IOException {
        fixedChannelPool.close();
    }
}
