package im.dlg.botsdk;

import im.dlg.botsdk.retry.RetryOptions;

import static im.dlg.botsdk.retry.RetryOptions.*;

/**
 * Main configuration file
 */
public class BotConfig {

    private final String botName;
    private final String password;
    private final String token;
    private final RetryOptions retryOptions;

    private BotConfig(String botName, String password, String token, RetryOptions retryOptions) {
        this.botName = botName;
        this.password = password;
        this.token = token;
        this.retryOptions = retryOptions;
    }

    public String getPassword() {
        return password;
    }

    /**
     * @return Token for bot auth
     */
    public String getToken() {
        return token;
    }

    /**
     * @return Used as application name
     */
    public String getBotName() {
        return botName;
    }

    public RetryOptions getRetryOptions() {
        return retryOptions;
    }

    public static final class Builder {

        private String botName = "JavaBot";
        private String token;
        private String password;
        private RetryOptions retryOptions = DEFAULT_RETRY_OPTIONS;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder withBotName(String botName) {
            this.botName = botName;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withToken(String token) {
            this.token = token;
            return this;
        }

        public Builder withRetryOptions(RetryOptions retryOptions) {
            this.retryOptions = retryOptions;
            return this;
        }

        public BotConfig build() {
            return new BotConfig(botName, password, token, retryOptions);
        }

    }
}
