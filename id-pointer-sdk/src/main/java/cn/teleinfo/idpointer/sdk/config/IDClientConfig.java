package cn.teleinfo.idpointer.sdk.config;

import cn.teleinfo.idpointer.sdk.client.TrustResolveManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * recursionServer
 * prd:36.112.25.8:3641
 * ote:45.120.243.40:3641
 * IDClientConfig
 */
public class IDClientConfig {

    /**
     * 递归服务ip,默认值 36.112.25.8
     */
    private String recursionServerIp;
    /**
     * 递归服务port，默认值 3641
     */
    private int recursionServerPort;
    /**
     * nio线程,默认值0
     */
    private int nioThreads;
    /**
     * 超时间时，默认60s
     */
    private int promiseTimeout;
    /**
     * 每个服务最小连接数,默认1
     */
    private int minConnectionsPerServer;
    /**
     * 每个服务最大连接数,默认10
     */
    private int maxConnectionsPerServer;
    /**
     * 空闲时间,默认600s
     */
    private int idleTimeSeconds;
    /**
     * 心跳执行,默认false
     */
    private boolean heatBeatRunning;
    /**
     * 每个用户、服务最小连接数,默认1
     */
    private int loginMinConnectionsPerServer;
    /**
     * 每个用户、服务最大连接数,默认10
     */
    private int loginMaxConnectionsPerServer;
    /**
     * 登录空闲时间,默认600s
     */
    private int loginIdleTimeSeconds;
    /**
     * 登录心跳执行,默认false
     */
    private boolean loginHeatBeatRunning;


    private String trustRootHandle;

    private String trustRootPubKeyPem;

    IDClientConfig(String recursionServerIp, int recursionServerPort, int nioThreads, int promiseTimeout, int minConnectionsPerServer, int maxConnectionsPerServer, int idleTimeSeconds, boolean heatBeatRunning, int loginMinConnectionsPerServer, int loginMaxConnectionsPerServer, int loginIdleTimeSeconds, boolean loginHeatBeatRunning, String trustRootHandle, String trustRootPubKeyPem) {
        this.recursionServerIp = recursionServerIp;
        this.recursionServerPort = recursionServerPort;
        this.nioThreads = nioThreads;
        this.promiseTimeout = promiseTimeout;
        this.minConnectionsPerServer = minConnectionsPerServer;
        this.maxConnectionsPerServer = maxConnectionsPerServer;
        this.idleTimeSeconds = idleTimeSeconds;
        this.heatBeatRunning = heatBeatRunning;
        this.loginMinConnectionsPerServer = loginMinConnectionsPerServer;
        this.loginMaxConnectionsPerServer = loginMaxConnectionsPerServer;
        this.loginIdleTimeSeconds = loginIdleTimeSeconds;
        this.loginHeatBeatRunning = loginHeatBeatRunning;
        this.trustRootHandle = trustRootHandle;
        this.trustRootPubKeyPem = trustRootPubKeyPem;
    }


    public static IDClientConfigBuilder builder() {
        return new IDClientConfigBuilder();
    }

    public String getRecursionServerIp() {
        return this.recursionServerIp;
    }

    public int getRecursionServerPort() {
        return this.recursionServerPort;
    }

    public int getNioThreads() {
        return this.nioThreads;
    }

    public int getMinConnectionsPerServer() {
        return this.minConnectionsPerServer;
    }

    public int getMaxConnectionsPerServer() {
        return this.maxConnectionsPerServer;
    }

    public int getIdleTimeSeconds() {
        return this.idleTimeSeconds;
    }

    public boolean isHeatBeatRunning() {
        return this.heatBeatRunning;
    }

    public int getLoginMinConnectionsPerServer() {
        return this.loginMinConnectionsPerServer;
    }

    public int getLoginMaxConnectionsPerServer() {
        return this.loginMaxConnectionsPerServer;
    }

    public int getLoginIdleTimeSeconds() {
        return this.loginIdleTimeSeconds;
    }

    public boolean isLoginHeatBeatRunning() {
        return this.loginHeatBeatRunning;
    }

    public int getPromiseTimeout() {
        return this.promiseTimeout;
    }

    public String getTrustRootHandle() {
        return trustRootHandle;
    }

    public String getTrustRootPubKeyPem() {
        return trustRootPubKeyPem;
    }

    public static class IDClientConfigBuilder {
        private String recursionServerIp;
        private int recursionServerPort;
        private int nioThreads;
        private int minConnectionsPerServer;
        private int maxConnectionsPerServer;
        private int idleTimeSeconds;
        private boolean heatBeatRunning;
        private int loginMinConnectionsPerServer;
        private int loginMaxConnectionsPerServer;
        private int loginIdleTimeSeconds;
        private boolean loginHeatBeatRunning;
        private int promiseTimeout;
        private String trustRootHandle;
        private String trustRootPubKeyPem;

        IDClientConfigBuilder() {
            this.nioThreads = 0;
            this.promiseTimeout = 60;
            this.minConnectionsPerServer = 1;
            this.maxConnectionsPerServer = 10;
            this.idleTimeSeconds = 600;
            this.heatBeatRunning = false;
            this.loginIdleTimeSeconds = 600;
            this.loginMinConnectionsPerServer = 1;
            this.loginMaxConnectionsPerServer = 10;
            this.loginHeatBeatRunning = false;
            prdEnv();
        }

        public IDClientConfigBuilder prdEnv() {
            this.recursionServerIp = "36.112.25.8";
            this.recursionServerPort = 3641;
            this.trustRootHandle = "88.111.1/0.0";
            this.trustRootPubKeyPem = getRootPublicKeyPem();
            return this;
        }

        public IDClientConfigBuilder oteEnv() {
            this.recursionServerIp = "45.120.243.40";
            this.recursionServerPort = 3641;
            this.trustRootHandle = "88.111.1/0.0";
            this.trustRootPubKeyPem = getRootPublicKeyPem();
            return this;
        }

        public IDClientConfigBuilder trustRootHandle(String trustRootHandle) {
            this.trustRootHandle = trustRootHandle;
            return this;
        }

        public IDClientConfigBuilder trustRootPubKeyPem(String trustRootPubKeyPem) {
            this.trustRootPubKeyPem = trustRootPubKeyPem;
            return this;
        }


        public IDClientConfigBuilder recursionServerIp(String recursionServerIp) {
            this.recursionServerIp = recursionServerIp;
            return this;
        }

        public IDClientConfigBuilder recursionServerPort(int recursionServerPort) {
            this.recursionServerPort = recursionServerPort;
            return this;
        }

        public IDClientConfigBuilder nioThreads(int nioThreads) {
            this.nioThreads = nioThreads;
            return this;
        }

        public IDClientConfigBuilder promiseTimeout(int promiseTimeout) {
            this.promiseTimeout = promiseTimeout;
            return this;
        }

        public IDClientConfigBuilder minConnectionsPerServer(int minConnectionsPerServer) {
            this.minConnectionsPerServer = minConnectionsPerServer;
            return this;
        }

        public IDClientConfigBuilder maxConnectionsPerServer(int maxConnectionsPerServer) {
            this.maxConnectionsPerServer = maxConnectionsPerServer;
            return this;
        }

        public IDClientConfigBuilder idleTimeSeconds(int idleTimeSeconds) {
            this.idleTimeSeconds = idleTimeSeconds;
            return this;
        }

        public IDClientConfigBuilder heatBeatRunning(boolean heatBeatRunning) {
            this.heatBeatRunning = heatBeatRunning;
            return this;
        }

        public IDClientConfigBuilder loginMinConnectionsPerServer(int loginMinConnectionsPerServer) {
            this.loginMinConnectionsPerServer = loginMinConnectionsPerServer;
            return this;
        }

        public IDClientConfigBuilder loginMaxConnectionsPerServer(int loginMaxConnectionsPerServer) {
            this.loginMaxConnectionsPerServer = loginMaxConnectionsPerServer;
            return this;
        }

        public IDClientConfigBuilder loginIdleTimeSeconds(int loginIdleTimeSeconds) {
            this.loginIdleTimeSeconds = loginIdleTimeSeconds;
            return this;
        }

        public IDClientConfigBuilder loginHeatBeatRunning(boolean loginHeatBeatRunning) {
            this.loginHeatBeatRunning = loginHeatBeatRunning;
            return this;
        }

        public IDClientConfig build() {
            return new IDClientConfig(recursionServerIp, recursionServerPort, nioThreads, promiseTimeout, minConnectionsPerServer, maxConnectionsPerServer, idleTimeSeconds, heatBeatRunning, loginMinConnectionsPerServer, loginMaxConnectionsPerServer, loginIdleTimeSeconds, loginHeatBeatRunning, trustRootHandle, trustRootPubKeyPem);
        }

        @Override
        public String toString() {
            return "IDClientConfigBuilder{" +
                    "recursionServerIp='" + recursionServerIp + '\'' +
                    ", recursionServerPort=" + recursionServerPort +
                    ", nioThreads=" + nioThreads +
                    ", minConnectionsPerServer=" + minConnectionsPerServer +
                    ", maxConnectionsPerServer=" + maxConnectionsPerServer +
                    ", idleTimeSeconds=" + idleTimeSeconds +
                    ", heatBeatRunning=" + heatBeatRunning +
                    ", loginMinConnectionsPerServer=" + loginMinConnectionsPerServer +
                    ", loginMaxConnectionsPerServer=" + loginMaxConnectionsPerServer +
                    ", loginIdleTimeSeconds=" + loginIdleTimeSeconds +
                    ", loginHeatBeatRunning=" + loginHeatBeatRunning +
                    ", promiseTimeout=" + promiseTimeout +
                    ", trustRootHandle='" + trustRootHandle + '\'' +
                    ", trustRootPubKeyPem='" + trustRootPubKeyPem + '\'' +
                    '}';
        }
        private String getRootPublicKeyPem() {
            try (InputStream in = TrustResolveManager.class.getResourceAsStream("/public_key.pem");) {

                byte[] buffer = new byte[1024];
                int len = 0;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }

                String rootPublicKeyPem = new String(out.toByteArray(), "UTF-8");
                return rootPublicKeyPem;
            } catch (Exception e) {
                throw new RuntimeException("load public key error", e);
            }
        }
    }


}
