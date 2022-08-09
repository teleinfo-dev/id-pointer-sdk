package cn.teleinfo.idpointer.sdk.session;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionIdFactoryDefault implements SessionIdFactory{

    private AtomicInteger atomicInteger = new AtomicInteger(10000);
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

    private static SessionIdFactory sessionIdFactory;
    public static SessionIdFactory getInstance() {
        if (sessionIdFactory == null) {
            synchronized (SessionIdFactoryDefault.class) {
                if (sessionIdFactory == null) {
                    sessionIdFactory = new SessionIdFactoryDefault();
                }
            }
        }
        return sessionIdFactory;
    }
}
