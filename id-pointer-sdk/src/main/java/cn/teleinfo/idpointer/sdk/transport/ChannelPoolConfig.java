package cn.teleinfo.idpointer.sdk.transport;

public class ChannelPoolConfig {
    private int minConnectionsPerServer;
    private int maxConnectionsPerServer;
    private int idleTimeSeconds;
    private boolean heatBeatRunning;

    public ChannelPoolConfig(int minConnectionsPerServer, int maxConnectionsPerServer, int idleTimeSeconds, boolean heatBeatRunning) {
        this.minConnectionsPerServer = minConnectionsPerServer;
        this.maxConnectionsPerServer = maxConnectionsPerServer;
        this.idleTimeSeconds = idleTimeSeconds;
        this.heatBeatRunning = heatBeatRunning;
    }

    public static ChannelPoolConfigBuilder builder() {
        return new ChannelPoolConfigBuilder();
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

    public static class ChannelPoolConfigBuilder {
        private int minConnectionsPerServer;
        private int maxConnectionsPerServer;
        private int idleTimeSeconds;
        private boolean heatBeatRunning;

        ChannelPoolConfigBuilder() {
        }

        public ChannelPoolConfigBuilder minConnectionsPerServer(int minConnectionsPerServer) {
            this.minConnectionsPerServer = minConnectionsPerServer;
            return this;
        }

        public ChannelPoolConfigBuilder maxConnectionsPerServer(int maxConnectionsPerServer) {
            this.maxConnectionsPerServer = maxConnectionsPerServer;
            return this;
        }

        public ChannelPoolConfigBuilder idleTimeSeconds(int idleTimeSeconds) {
            this.idleTimeSeconds = idleTimeSeconds;
            return this;
        }

        public ChannelPoolConfigBuilder heatBeatRunning(boolean heatBeatRunning) {
            this.heatBeatRunning = heatBeatRunning;
            return this;
        }

        public ChannelPoolConfig build() {
            return new ChannelPoolConfig(minConnectionsPerServer, maxConnectionsPerServer, idleTimeSeconds, heatBeatRunning);
        }

        public String toString() {
            return "ChannelPoolConfig.ChannelPoolConfigBuilder(minConnectionsPerServer=" + this.minConnectionsPerServer + ", maxConnectionsPerServer=" + this.maxConnectionsPerServer + ", idleTimeSeconds=" + this.idleTimeSeconds + ", heatBeatRunning=" + this.heatBeatRunning + ")";
        }
    }
}
