package cn.teleinfo.idpointer.sdk.session;

public class SessionManagerFactory {
    private static SessionManager sessionManager;

    public static SessionManager getSessionManager() {
        if (sessionManager == null) {
            synchronized (SessionManagerFactory.class) {
                if (sessionManager == null) {
                    sessionManager = new SessionManagerDefault();
                }
            }
        }
        return sessionManager;
    }
}
