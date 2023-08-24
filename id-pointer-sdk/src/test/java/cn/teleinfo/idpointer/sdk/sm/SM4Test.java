package cn.teleinfo.idpointer.sdk.sm;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class SM4Test {
    @Test
    void test() throws DecoderException {
        String content = "test中文";
        SymmetricCrypto sm4 = SmUtil.sm4(Hex.decodeHex("14f2b9e773b253213fa9f6c9ceab2afb"));
        String encryptHex = sm4.encryptHex(content);

        System.out.println(encryptHex);
        String decryptStr = sm4.decryptStr(encryptHex, StandardCharsets.UTF_8);
    }
}
