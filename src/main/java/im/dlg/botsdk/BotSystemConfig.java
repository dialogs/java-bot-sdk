package im.dlg.botsdk;

public class BotSystemConfig {

    private final String host;
    private final int port;
    private final String certPath;
    private final String certPassword;
    private final boolean secure;
    private final boolean compression;

    public BotSystemConfig(String host, int port, String certPath, String certPassword, boolean secure, boolean compression) {
        this.host = host;
        this.port = port;
        this.certPath = certPath;
        this.certPassword = certPassword;
        this.secure = secure;
        this.compression = compression;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getCertPath() {
        return certPath;
    }

    public String getCertPassword() {
        return certPassword;
    }

    public boolean isSecure() {
        return secure;
    }

    public boolean isCompression() {
        return compression;
    }

    public static class Builder {

        private String host;
        private int port;
        private String certPath;
        private String certPassword;
        private boolean secure;
        private boolean compression;

        private Builder() {

        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withCertPath(String certPath) {
            this.certPath = certPath;
            return this;
        }

        public Builder withCertPassword(String certPassword) {
            this.certPassword = certPassword;
            return this;
        }

        public Builder withSecure(boolean secure) {
            this.secure = secure;
            return this;
        }

        public Builder withCompression(boolean compression) {
            this.compression = compression;
            return this;
        }

        public BotSystemConfig build() {
            return new BotSystemConfig(host, port, certPath, certPassword, secure, compression);
        }

        public static Builder newBuilder() {
            return new Builder();
        }

    }

}
