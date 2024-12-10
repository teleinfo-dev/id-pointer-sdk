package cn.teleinfo.idpointer.sdk.config;

import org.junit.jupiter.api.Test;

class IDClientConfigTest {

    @Test
    void name() {
        // 生产环境
        IDClientConfig prdConfig = IDClientConfig.builder().prdEnv().build();
        // OTE环境
        IDClientConfig oteConfig = IDClientConfig.builder().oteEnv().build();
    }
}