package cn.teleinfo.idpointer.sdk.transport;

import io.netty.util.AttributeKey;

public interface Transport {

    AttributeKey<Integer> SESSION_ID_KEY = AttributeKey.newInstance("SESSION_ID");

}
