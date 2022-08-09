package cn.teleinfo.idpointer.sdk.config;

public class IDClientConfig {
    // prd:36.112.25.8
    // ote:45.120.243.40
    private String recursionServerIp = "45.120.243.40";
    private int recursionServerPort = 3641;
    private int nioThreads = 0;
    private int minConnectionsPerServer = 1;
    private int maxConnectionsPerServer = 1;

    public IDClientConfig(String recursionServerIp, int recursionServerPort, int nioThreads, int minConnectionsPerServer, int maxConnectionsPerServer) {
        this.recursionServerIp = recursionServerIp;
        this.recursionServerPort = recursionServerPort;
        this.nioThreads = nioThreads;
        this.minConnectionsPerServer = minConnectionsPerServer;
        this.maxConnectionsPerServer = maxConnectionsPerServer;
    }

    public IDClientConfig() {
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

    public static class IDClientConfigBuilder {
        private String recursionServerIp = "45.120.243.40";
        private int recursionServerPort = 3641;
        private int nioThreads = 0;
        private int minConnectionsPerServer = 1;
        private int maxConnectionsPerServer = 100;

        IDClientConfigBuilder() {
        }

        public IDClientConfigBuilder prdEnv(){
            this.recursionServerIp = "36.112.25.8";
            this.recursionServerPort = 3641;
            return this;
        }

        public IDClientConfigBuilder oteEnv(){
            this.recursionServerIp = "45.120.243.40";
            this.recursionServerPort = 3641;
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

        public IDClientConfigBuilder minConnectionsPerServer(int minConnectionsPerServer) {
            this.minConnectionsPerServer = minConnectionsPerServer;
            return this;
        }

        public IDClientConfigBuilder maxConnectionsPerServer(int maxConnectionsPerServer) {
            this.maxConnectionsPerServer = maxConnectionsPerServer;
            return this;
        }

        public IDClientConfig build() {
            return new IDClientConfig(recursionServerIp, recursionServerPort, nioThreads, minConnectionsPerServer, maxConnectionsPerServer);
        }

        public String toString() {
            return "IDClientConfig.IDClientConfigBuilder(recursionServerIp=" + this.recursionServerIp + ", recursionServerPort=" + this.recursionServerPort + ", nioThreads=" + this.nioThreads + ", minConnectionsPerServer=" + this.minConnectionsPerServer + ", maxConnectionsPerServer=" + this.maxConnectionsPerServer + ")";
        }
    }
}
