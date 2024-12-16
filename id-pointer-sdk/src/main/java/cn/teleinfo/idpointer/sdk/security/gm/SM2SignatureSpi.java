package cn.teleinfo.idpointer.sdk.security.gm;

import org.bouncycastle.crypto.signers.SM2Signer;

import java.security.*;

/**
 *  SM2签名算法
 *  fixme: 未实现
 *
 */
public class SM2SignatureSpi extends SignatureSpi {
    @Override
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {

    }

    @Override
    protected void engineUpdate(byte b) throws SignatureException {

    }

    @Override
    protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {

    }

    @Override
    protected byte[] engineSign() throws SignatureException {
        return new byte[0];
    }

    @Override
    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        return false;
    }

    @Override
    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {

    }

    @Override
    protected Object engineGetParameter(String param) throws InvalidParameterException {
        return null;
    }
}
