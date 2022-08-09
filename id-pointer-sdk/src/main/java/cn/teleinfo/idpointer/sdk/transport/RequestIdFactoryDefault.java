package cn.teleinfo.idpointer.sdk.transport;

import java.util.concurrent.atomic.AtomicInteger;

public class RequestIdFactoryDefault implements RequestIdFactory {
    private AtomicInteger atomicInteger;

    public RequestIdFactoryDefault() {
        atomicInteger = new AtomicInteger();
    }

    @Override
    public int getNextInteger() {
        int i = atomicInteger.incrementAndGet();
        if (i < 0) {
            synchronized (this) {
                i = atomicInteger.get();
                if (i < 0) {
                    atomicInteger.set(1);
                    i = 1;
                }
            }
        }
        return i;
    }

    private static RequestIdFactory requestIdFactory;
    public static RequestIdFactory getInstance() {
        if (requestIdFactory == null) {
            synchronized (RequestIdFactoryDefault.class) {
                if (requestIdFactory == null) {
                    requestIdFactory = new RequestIdFactoryDefault();
                }
            }
        }
        return requestIdFactory;
    }

}
