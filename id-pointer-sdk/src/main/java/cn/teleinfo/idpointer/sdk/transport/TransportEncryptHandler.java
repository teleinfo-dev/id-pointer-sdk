package cn.teleinfo.idpointer.sdk.transport;

import cn.hutool.crypto.asymmetric.SM2;
import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.session.Session;
import cn.teleinfo.idpointer.sdk.util.EncryptionUtils;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

import java.security.PrivateKey;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TransportEncryptHandler {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TransportEncryptHandler.class);

    private RequestIdFactory requestIdGenerate;

    public TransportEncryptHandler(RequestIdFactory requestIdGenerate) {
        this.requestIdGenerate = requestIdGenerate;
    }

    public void handle(Channel channel, AbstractRequest request, MessageManager messageManager, AuthenticationInfo authenticationInfo) throws IDException {
        if (request.encrypt) {
            Attribute<Session> attr = channel.attr(Transport.SESSION_KEY);
            Session session = attr.get();
            // 当没有进行加密时
            if (!session.isEncryptMessage()) {

                // 密钥交换
                SessionSetupRequest sessionSetupRequest = new SessionSetupRequest(authenticationInfo.getUserIdHandle(), authenticationInfo.getUserIdIndex());
                sessionSetupRequest.requestId = requestIdGenerate.getNextInteger();
                sessionSetupRequest.sessionId = session.getSessionId();
                sessionSetupRequest.keyExchangeMode = Common.KEY_EXCHANGE_CIPHER_HDL;

                ResponsePromise sessionSetupPromise = messageManager.process(sessionSetupRequest, channel);

                AbstractResponse sessionSetupTempResponse = null;
                try {
                    sessionSetupTempResponse = sessionSetupPromise.get(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new IDException(IDException.PROMISE_GET_ERROR, "session setup response error", e);
                } catch (ExecutionException e) {
                    throw new IDException(IDException.PROMISE_GET_ERROR, "session setup response error", e);
                } catch (TimeoutException e) {
                    throw new IDException(IDException.PROMISE_GET_ERROR, "session setup response error", e);
                }
                SessionSetupResponse sessionSetupResponse = (SessionSetupResponse) sessionSetupTempResponse;


                if (sessionSetupResponse.keyExchangeMode == Common.KEY_EXCHANGE_CIPHER_HDL) {
                    PublicKeyAuthenticationInfo publicKeyAuthenticationInfo = (PublicKeyAuthenticationInfo) authenticationInfo;
                    PrivateKey privateKey = publicKeyAuthenticationInfo.getPrivateKey();

                    String alg = privateKey.getAlgorithm().trim();

                    if (alg.equals("EC")) {
                        // todo: to impl
                        byte[] sessionKeyBytes;
                        try {
                            final SM2 sm2 = new SM2(privateKey, null);
                            sessionKeyBytes = sm2.decrypt(sessionSetupResponse.data);
                        } catch (Exception e) {
                            throw new IDException(IDException.CLIENT_ERROR, "decrypt sessionKey error", e);
                        }

                        int sessionKeyAlg = Encoder.readInt(sessionKeyBytes, 0);
                        byte[] sessionKey = Util.substring(sessionKeyBytes, Encoder.INT_SIZE);
                        session.setSessionKey(sessionKey);
                        session.setSessionKeyAlgorithmCode(sessionKeyAlg);

                        session.setEncryptMessage(true);
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
                        session.setSessionKey(sessionKey);
                        session.setSessionKeyAlgorithmCode(sessionKeyAlg);

                        session.setEncryptMessage(true);
                        log.info("sessionKeyAlg:{},session key:{}", sessionKeyAlg, Hex.encodeHexString(sessionKey));

                    }
                } else {
                    throw new IDException(IDException.CLIENT_ERROR, "Un support keyExchangeMode");
                }

            }
        }

    }
}
