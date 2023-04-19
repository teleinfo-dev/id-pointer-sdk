package cn.teleinfo.idpointer.sdk.core;

import cn.hutool.crypto.SecureUtil;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;

class UtilTest {

    @Test
    void sm2KeyTest() throws Exception {
        KeyPair pair = SecureUtil.generateKeyPair("SM2");
        byte[] encoded = pair.getPublic().getEncoded();
        byte[] enc = new byte[Encoder.INT_SIZE*2 + encoded.length + 2 + Common.KEY_ENCODING_SM2_PUB_KEY.length];
        int offset = Encoder.writeByteArray(enc, 0, Common.KEY_ENCODING_SM2_PUB_KEY);
        offset += Encoder.writeInt2(enc, offset, 0);
        offset += Encoder.writeByteArray(enc, offset, encoded);

        PublicKey publicKey = Util.getPublicKeyFromBytes(enc, 0);

    }

}