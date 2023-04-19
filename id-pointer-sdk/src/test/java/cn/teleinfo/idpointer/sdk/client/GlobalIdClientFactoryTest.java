package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.config.IDClientConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalIdClientFactoryTest {

    @Test
    void getIdClientFactory() {
        IDClientConfig oteConfig = IDClientConfig.builder().oteEnv().build();
        GlobalIdClientFactory.init(oteConfig);
        IDClientFactory idClientFactory = GlobalIdClientFactory.getIdClientFactory();
        //...
    }
}