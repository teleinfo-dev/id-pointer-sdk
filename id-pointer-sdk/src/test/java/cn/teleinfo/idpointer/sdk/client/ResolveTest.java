package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import org.junit.jupiter.api.Test;

public class ResolveTest {
    @Test
    void name() throws IDException {
        IDResolver idResolver = GlobalIdClientFactory.getIdResolver();
        HandleValue[] handleValues = idResolver.resolveHandle("88.111.1/teleinfo.cn");

    }
}
