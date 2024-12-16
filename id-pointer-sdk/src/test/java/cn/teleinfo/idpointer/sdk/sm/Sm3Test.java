package cn.teleinfo.idpointer.sdk.sm;

import cn.hutool.crypto.SmUtil;
import org.junit.jupiter.api.Test;

public class Sm3Test {

    @Test
    void test1() {
        String digestHex = SmUtil.sm3("aaaaa");
        System.out.println(digestHex);
    }
}
