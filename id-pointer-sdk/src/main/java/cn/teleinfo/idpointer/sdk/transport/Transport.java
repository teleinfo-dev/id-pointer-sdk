package cn.teleinfo.idpointer.sdk.transport;

import cn.teleinfo.idpointer.sdk.session.Session;
import io.netty.util.AttributeKey;

public interface Transport {

    AttributeKey<Session> SESSION_KEY = AttributeKey.newInstance("TRANSPORT_SESSION");

}
