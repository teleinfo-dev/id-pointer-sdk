package cn.teleinfo.idpointer.sdk.session;

public class AuthenticationInfoManagerFactory {
    private static AuthenticationInfoManager authenticationInfoManager;

    public static AuthenticationInfoManager getAuthenticationInfoManager() {
        if (authenticationInfoManager == null) {
            synchronized (AuthenticationInfoManagerFactory.class) {
                if (authenticationInfoManager == null) {
                    authenticationInfoManager = new AuthenticationInfoManagerDefault();
                }
            }
        }
        return authenticationInfoManager;
    }

}
